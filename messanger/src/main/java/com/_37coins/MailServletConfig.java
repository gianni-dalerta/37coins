package com._37coins;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
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

import com._37coins.MessageProcessor.Action;
import com._37coins.envaya.MessageFactory;
import com._37coins.imap.JavaPushMailAccount;
import com._37coins.pojo.SendAction;
import com._37coins.pojo.ServiceEntry;
import com._37coins.pojo.ServiceList;
import com._37coins.resources.EnvayaSmsResource;
import com._37coins.resources.HealthCheckResource;
import com._37coins.resources.PreferenceResource;
import com._37coins.resources.WithdrawalResource;
import com._37coins.sendMail.AmazonEmailClient;
import com._37coins.sendMail.EmailFactory;
import com._37coins.sendMail.MailServiceClient;
import com._37coins.sendMail.SmtpEmailClient;
import com._37coins.workflow.DepositWorkflowClientExternal;
import com._37coins.workflow.DepositWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternal;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.mysql.jdbc.AbandonedConnectionCleanupThread;
import com.wordnik.swagger.jaxrs.JaxrsApiReader;



public class MailServletConfig extends GuiceServletContextListener {
	public static AWSCredentials awsCredentials = null;
	public static String domainName;
	public static String actListName = "mail-activities-tasklist";
	public static String endpoint;
	public static String senderMail;
	public static String smtpHost;
	public static String smtpUser;
	public static String smtpPassword;
	public static String imapHost;
	public static final int IMAP_PORT = 993;
	public static final boolean IMAP_SSL = true;
	public static String imapUser;
	public static String imapPassword;
	public static String basePath;
	public static String queueUri;
	public static Logger log = LoggerFactory.getLogger(MailServletConfig.class);
	static {
		JaxrsApiReader.setFormatString("");
		if (null!=System.getProperty("accessKey")){
		awsCredentials = new BasicAWSCredentials(
				System.getProperty("accessKey"),
				System.getProperty("secretKey"));
		}
		domainName = System.getProperty("swfDomain");
		endpoint = System.getProperty("endpoint");
		senderMail = System.getProperty("senderMail");
		smtpHost = System.getProperty("smtpHost");
		smtpUser = System.getProperty("smtpUser");
		smtpPassword = System.getProperty("smtpPassword");
		//EMAIL SETTINGS
		imapHost = System.getProperty("imapHost");
		imapUser = System.getProperty("imapUser");
		imapPassword = System.getProperty("imapPassword");
		basePath = System.getProperty("basePath");
		queueUri = System.getProperty("queueUri");
	}
	
	private ServletContext servletContext;
	private ActivityWorker activityWorker;
	private JavaPushMailAccount jPM;
    
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		servletContext = servletContextEvent.getServletContext();
		super.contextInitialized(servletContextEvent);
		PersistenceConfiguration.getInstance().getEntityManagerFactory();
		final Injector i = getInjector();
		activityWorker = i.getInstance(ActivityWorker.class);
		activityWorker.start();
		// set up receiving mails
		final MessageProcessor msgProcessor = i.getInstance(MessageProcessor.class);
		
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
						Map<String, Object> o = msgProcessor.process(
								m.getFrom(), m.getSubject());
						o.put("source","email");
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
    protected Injector getInjector(){
        return Guice.createInjector(new ServletModule(){
            @Override
            protected void configureServlets(){
            	serve("/rest/*").with(RestletServlet.class);
            }
        },new PersistenceModule(servletContext){
        	@Override
        	protected void configure() {
        		install(new FactoryModuleBuilder()
        	    	.implement(JaxRsApplication.class, CoinsApplication.class)
        	    	.build(ContextFactory.class));
        		bind(MailActivitiesImpl.class).annotatedWith(Names.named("activityImpl")).to(MailActivitiesImpl.class);
        	}
			@Override
			public Set<Class<?>> getClassList() {
				Set<Class<?>> cs = new HashSet<>();
				cs.add(PreferenceResource.class);
				cs.add(EnvayaSmsResource.class);
				cs.add(WithdrawalResource.class);
				cs.add(HealthCheckResource.class);
				return cs;
			}
			
			
			@Provides
			@Singleton
			@SuppressWarnings("unused")
			public MessageProcessor getMessageProcessor() {
				return new MessageProcessor(servletContext);
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			MailServiceClient getMailClient(){
				if (null!=smtpHost){
					return new SmtpEmailClient(smtpHost, smtpUser, smtpPassword);
				}else{
					if (awsCredentials==null){
						return new AmazonEmailClient(
								new AmazonSimpleEmailServiceClient());
					}else{
						return new AmazonEmailClient(
								new AmazonSimpleEmailServiceClient(awsCredentials));
					}					
				}
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
			
			@Provides @Named("wfClient") @Singleton @SuppressWarnings("unused")
			AmazonSimpleWorkflow getSimpleWorkflowClient() {
				AmazonSimpleWorkflow rv = null;
				if (null!=awsCredentials){
					rv = new AmazonSimpleWorkflowClient(awsCredentials);
				}else{
					rv = new AmazonSimpleWorkflowClient();
				}
				rv.setEndpoint(endpoint);
				return rv;
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			public ActivityWorker getActivityWorker(@Named("wfClient") AmazonSimpleWorkflow swfClient, 
					@Named("activityImpl") MailActivitiesImpl activitiesImpl) {
				ActivityWorker activityWorker = new ActivityWorker(swfClient, domainName,
						actListName);
				try {
					activityWorker.addActivitiesImplementation(activitiesImpl);
				} catch (InstantiationException | IllegalAccessException
						| SecurityException | NoSuchMethodException e) {
					e.printStackTrace();
				}
				return activityWorker;
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			public SendAction provideSendAction() {
				SendAction sa = new SendAction(servletContext);
				return sa;
			}
			@Provides @Singleton @SuppressWarnings("unused")
			public List<ServiceEntry> provideCategories() {
				return ServiceList.initialize(servletContext);
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			public EmailFactory provideEmailFactory() {
				return new EmailFactory(servletContext);
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			public MessageFactory provideMessageFactory() {
				return new MessageFactory(servletContext);
			}
		});
    }
	
    @Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.contextDestroyed(sce);
		PersistenceConfiguration.getInstance().closeEntityManagerFactory();
		deregisterJdbc();
		jPM.disconnect();
		try {
			activityWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
            System.out.println("Activity Worker Exited.");
		}catch (InterruptedException e) {
            e.printStackTrace();
        }
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
