package com._37coins;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;


import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restnucleus.PersistenceConfiguration;
import org.restnucleus.inject.ContextFactory;
import org.restnucleus.inject.PersistenceModule;
import org.restnucleus.servlet.RestletServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MessageProcessor;
import com._37coins.MessageProcessor.Action;
import com._37coins.bizLogic.DepositWorkflowImpl;
import com._37coins.bizLogic.WithdrawalWorkflowImpl;
import com._37coins.imap.JavaPushMailAccount;
import com._37coins.resources.HealthCheckResource;
import com._37coins.workflow.DepositWorkflowClientExternal;
import com._37coins.workflow.DepositWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternal;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.mysql.jdbc.AbandonedConnectionCleanupThread;
import com.wordnik.swagger.jaxrs.JaxrsApiReader;

public class ServletConfig extends GuiceServletContextListener {
	public static AWSCredentials awsCredentials;
	public static String domainName;
	public static String endpoint;
	public static String actListName = "core-activities-tasklist";
	public static String imapHost;
	public static final int IMAP_PORT = 993;
	public static final boolean IMAP_SSL = true;
	public static String imapUser;
	public static String imapPassword;
	public static String basePath;
	public static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";



	public static Logger log = LoggerFactory.getLogger(ServletConfig.class);
	static {
		JaxrsApiReader.setFormatString("");
		Map<String,Object> param2 = null;
		Map<String,Object> param3 = null;
		ObjectMapper om = new ObjectMapper();
		try {
			param2 = om.readValue(
					System.getProperty("PARAM2"), 
					new TypeReference<Map<String,Object>>() {});
			param3 = om.readValue(
					System.getProperty("PARAM3"), 
					new TypeReference<Map<String,Object>>() {});
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		awsCredentials = new BasicAWSCredentials(
				(String)param2.get("accessKey"),
				(String)param2.get("secretKey"));
		domainName = (String)param2.get("swfDomain");
		endpoint = (String)param2.get("endpoint");
		//EMAIL SETTINGS
		imapHost = (String)param3.get("imapHost");
		imapUser = (String)param3.get("imapUser");
		imapPassword = (String)param3.get("imapPassword");
		basePath = (String)param3.get("basePath");
	}

	private ServletContext servletContext;
	private ActivityWorker activityWorker;
	private JavaPushMailAccount jPM;
	private WorkflowWorker depositWorker;
	private WorkflowWorker withdrawalWorker;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		servletContext = servletContextEvent.getServletContext();
		super.contextInitialized(servletContextEvent);
		PersistenceConfiguration.getInstance().getEntityManagerFactory();
		final Injector i = getInjector();
		activityWorker = i.getInstance(ActivityWorker.class);
		activityWorker.start();
		depositWorker = i.getInstance(Key.get(WorkflowWorker.class,
				Names.named("deposit")));
		depositWorker.start();
		withdrawalWorker = i.getInstance(Key.get(WorkflowWorker.class,
				Names.named("withdrawal")));
		withdrawalWorker.start();
		// set up receiving mails
		final MessageProcessor mailProcessor = i.getInstance(MessageProcessor.class);
		jPM = new JavaPushMailAccount(imapUser, imapHost, IMAP_PORT, IMAP_SSL);
		jPM.setCredentials(imapUser, imapPassword);
		jPM.setMessageCounterListerer(new MessageCountListener() {
			@Override
			public void messagesRemoved(MessageCountEvent e) {
			}

			@Override
			public void messagesAdded(MessageCountEvent e) {
				try {
					for (Message m : e.getMessages()) {
						Map<String, Object> o = mailProcessor.process(
								m.getFrom(), m.getSubject());
						o.put("service","37coins");
						switch (Action.fromString((String)o.get("action"))) {
						case CREATE:
						case BALANCE:
						case DEPOSIT:
							o.put("action", o.get("action"));
							i.getInstance(DepositWorkflowClientExternal.class)
									.executeCommand(o);
							break;
						case SEND_CONFIRM:
						case SEND:
							o.put("action", o.get("action"));
							i.getInstance(
									WithdrawalWorkflowClientExternal.class)
									.executeCommand(o);
							break;
						}
					}
				} catch (MessagingException e1) {
					e1.printStackTrace();
				}
			}
		});
		jPM.run();
		log.info("ServletContextListener started");
	}

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new ServletModule() {
			@Override
			protected void configureServlets() {
				serve("/rest/*").with(RestletServlet.class);
			}
		}, new PersistenceModule(servletContext) {
			@Override
			protected void configure() {
				install(new FactoryModuleBuilder().implement(
						JaxRsApplication.class, CoreApplication.class).build(
						ContextFactory.class));
				bind(CoreActivitiesImpl.class).annotatedWith(
						Names.named("activityImpl")).to(
						CoreActivitiesImpl.class);
			}

			@Override
			public Set<Class<?>> getClassList() {
				Set<Class<?>> cs = new HashSet<>();
				cs.add(HealthCheckResource.class);
				return cs;
			}

			@Provides
			@Singleton
			@SuppressWarnings("unused")
			public DepositWorkflowClientExternal getDWorkflowClientExternal(
					@Named("wfClient") AmazonSimpleWorkflow workflowClient) {
				return new DepositWorkflowClientExternalFactoryImpl(
						workflowClient, domainName).getClient();
			}

			@Provides
			@Singleton
			@SuppressWarnings("unused")
			public WithdrawalWorkflowClientExternal getSWorkflowClientExternal(
					@Named("wfClient") AmazonSimpleWorkflow workflowClient) {
				return new WithdrawalWorkflowClientExternalFactoryImpl(
						workflowClient, domainName).getClient();
			}

			@Provides @Named("wfClient") @Singleton
			@SuppressWarnings("unused")
			AmazonSimpleWorkflow getSimpleWorkflowClient() {
				AmazonSimpleWorkflow rv = new AmazonSimpleWorkflowClient(
						awsCredentials);
				rv.setEndpoint("swf.ap-northeast-1.amazonaws.com");
				return rv;
			}

			@Provides
			@Named("deposit")
			@Singleton
			@SuppressWarnings("unused")
			public WorkflowWorker getDepositWorker(
					@Named("wfClient") AmazonSimpleWorkflow swfClient) {
				WorkflowWorker workflowWorker = new WorkflowWorker(swfClient,
						domainName, "deposit-workflow-tasklist");
				try {
					workflowWorker
							.addWorkflowImplementationType(DepositWorkflowImpl.class);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
				return workflowWorker;
			}

			@Provides
			@Named("withdrawal")
			@Singleton
			@SuppressWarnings("unused")
			public WorkflowWorker getWithdrawalWorker(
					@Named("wfClient") AmazonSimpleWorkflow swfClient) {
				WorkflowWorker workflowWorker = new WorkflowWorker(swfClient,
						domainName, "withdrawal-workflow-tasklist");
				try {
					workflowWorker
							.addWorkflowImplementationType(WithdrawalWorkflowImpl.class);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
				return workflowWorker;
			}

			@Provides
			@Singleton
			@SuppressWarnings("unused")
			public ActivityWorker getActivityWorker(
					@Named("wfClient") AmazonSimpleWorkflow swfClient,
					@Named("activityImpl") CoreActivitiesImpl activitiesImpl) {
				System.out.println(activitiesImpl);
				ActivityWorker activityWorker = new ActivityWorker(swfClient,
						domainName, actListName);
				try {
					activityWorker.addActivitiesImplementation(activitiesImpl);
				} catch (InstantiationException | IllegalAccessException
						| SecurityException | NoSuchMethodException e) {
					e.printStackTrace();
				}
				return activityWorker;
			}
		});
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.contextDestroyed(sce);
		PersistenceConfiguration.getInstance().closeEntityManagerFactory();
		jPM.disconnect();
		try {
			activityWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
			System.out.println("Activity Worker Exited.");
			depositWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
			withdrawalWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
			System.out.println("Workflow Worker Exited.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		deregisterJdbc();
		log.info("ServletContextListener destroyed");
	}

	public void deregisterJdbc() {
		// This manually deregisters JDBC driver, which prevents Tomcat 7 from
		// complaining about memory leaks wrto this class
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
				log.info(String.format("deregistering jdbc driver: %s", driver));
			} catch (SQLException e) {
				log.info(String.format("Error deregistering driver %s", driver));
				e.printStackTrace();
			}
		}
		try {
			AbandonedConnectionCleanupThread.shutdown();
		} catch (InterruptedException e) {
			log.warn("SEVERE problem cleaning up: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
