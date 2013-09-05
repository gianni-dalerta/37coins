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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;


import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restnucleus.PersistenceConfiguration;
import org.restnucleus.inject.ContextFactory;
import org.restnucleus.inject.PersistenceModule;
import org.restnucleus.servlet.RestletServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.envaya.MessageFactory;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	public static AWSCredentials awsCredentials;
	public static String domainName;
	public static String actListName = "mail-activities-tasklist";
	public static String endpoint;
	public static String senderMail;
	public static String smtpHost;
	public static String smtpUser;
	public static String smtpPassword;
	public static String basePath;
	public static String queueUri;
	public static Logger log = LoggerFactory.getLogger(MailServletConfig.class);
	static {
		JaxrsApiReader.setFormatString("");
		Map<String,Object> param2 = null;
		Map<String,Object> param3 = null;
		ObjectMapper om = new ObjectMapper();
		try {
			param2 = om.readValue(
					System.getProperty("PARAM2"), new
					TypeReference<
					Map<String,Object>>() {});
			param3 = om.readValue(
					System.getProperty("PARAM3"), new
					TypeReference<
					Map<String,Object>>() {});
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		awsCredentials = new BasicAWSCredentials(
				(String)param2.get("accessKey"),
				(String)param2.get("secretKey"));
		domainName = (String)param2.get("swfDomain");
		endpoint = (String)param2.get("endpoint");
		senderMail = (String)param3.get("senderMail");
		smtpHost = (String)param3.get("smtpHost");
		smtpUser = (String)param3.get("smtpUser");
		smtpPassword = (String)param3.get("smtpPassword");
		basePath = (String)param3.get("basePath");
		queueUri = (String)param3.get("queueUri");
	}
	
	private ServletContext servletContext;
	private ActivityWorker activityWorker;
    
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		servletContext = servletContextEvent.getServletContext();
		super.contextInitialized(servletContextEvent);
		PersistenceConfiguration.getInstance().getEntityManagerFactory();
		final Injector i = getInjector();
		activityWorker = i.getInstance(ActivityWorker.class);
		activityWorker.start();
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
				AmazonSimpleWorkflow rv = new AmazonSimpleWorkflowClient(awsCredentials);
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
