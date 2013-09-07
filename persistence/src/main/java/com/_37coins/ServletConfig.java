package com._37coins;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restnucleus.PersistenceConfiguration;
import org.restnucleus.inject.ContextFactory;
import org.restnucleus.inject.PersistenceModule;
import org.restnucleus.servlet.RestletServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bizLogic.DepositWorkflowImpl;
import com._37coins.bizLogic.WithdrawalWorkflowImpl;
import com._37coins.resources.HealthCheckResource;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
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
	public static AWSCredentials awsCredentials=null;
	public static String domainName;
	public static String endpoint;
	public static String actListName = "core-activities-tasklist";
	public static String basePath;
	public static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";



	public static Logger log = LoggerFactory.getLogger(ServletConfig.class);
	static {
		JaxrsApiReader.setFormatString("");
		if (null!=System.getProperty("accessKey")){
			awsCredentials = new BasicAWSCredentials(
				System.getProperty("accessKey"),
				System.getProperty("secretKey"));
		}
		domainName = System.getProperty("swfDomain");
		endpoint = System.getProperty("endpoint");
		basePath = System.getProperty("basePath");
	}

	private ServletContext servletContext;
	private ActivityWorker activityWorker;
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

			@Provides @Named("wfClient") @Singleton
			@SuppressWarnings("unused")
			AmazonSimpleWorkflow getSimpleWorkflowClient() {
				AmazonSimpleWorkflow rv = null;
				if (null!=awsCredentials){
					rv = new AmazonSimpleWorkflowClient(awsCredentials);
				}else{
					rv = new AmazonSimpleWorkflowClient();
				}
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
