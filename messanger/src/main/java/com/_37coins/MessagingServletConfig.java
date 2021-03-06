package com._37coins;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.restnucleus.log.SLF4JTypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bizLogic.NonTxWorkflowImpl;
import com._37coins.bizLogic.WithdrawalWorkflowImpl;
import com._37coins.imap.JavaPushMailAccount;
import com._37coins.parse.CommandParser;
import com._37coins.parse.InterpreterFilter;
import com._37coins.parse.ParserAccessFilter;
import com._37coins.parse.ParserClient;
import com._37coins.parse.ParserFilter;
import com._37coins.sendMail.AmazonEmailClient;
import com._37coins.sendMail.MailServiceClient;
import com._37coins.sendMail.SmtpEmailClient;
import com._37coins.web.AccountPolicy;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;



public class MessagingServletConfig extends GuiceServletContextListener {
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
	public static String srvcPath;
	public static String queueUri;
	public static String plivoKey;
	public static String plivoSecret;
	public static String resPath;
	public static String ldapUrl;
	public static String ldapUser;
	public static String ldapPw;
	public static String ldapBaseDn;
	public static String captchaPubKey;
	public static String captchaSecKey;
	public static int localPort;
	public static Logger log = LoggerFactory.getLogger(MessagingServletConfig.class);
	public static Injector injector;
	static {
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
		srvcPath = System.getProperty("srvcPath");
		queueUri = System.getProperty("queueUri");
		plivoKey = System.getProperty("plivoKey");
		plivoSecret = System.getProperty("plivoSecret");
		resPath = System.getProperty("resPath");
		ldapUrl = System.getProperty("ldapUrl");
		ldapUser = System.getProperty("ldapUser");
		ldapPw = System.getProperty("ldapPw");
		ldapBaseDn = System.getProperty("ldapBaseDn");
		captchaPubKey = System.getProperty("captchaPubKey");
		captchaSecKey = System.getProperty("captchaSecKey");
	}
	
	private ServletContext servletContext;
	private ActivityWorker activityWorker;
	private WorkflowWorker depositWorker;
	private WorkflowWorker withdrawalWorker;
	private JavaPushMailAccount jPM;
    
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		servletContext = servletContextEvent.getServletContext();
		super.contextInitialized(servletContextEvent);
		final Injector i = getInjector();
		activityWorker = i.getInstance(ActivityWorker.class);
		activityWorker.start();
		depositWorker = i.getInstance(Key.get(WorkflowWorker.class,
				Names.named("nonTx")));
		depositWorker.start();
		withdrawalWorker = i.getInstance(Key.get(WorkflowWorker.class,
				Names.named("withdrawal")));
		withdrawalWorker.start();
		// set up receiving mails
		if (null!=imapUser){
			jPM = new JavaPushMailAccount(imapUser, imapHost, IMAP_PORT, IMAP_SSL);
			jPM.setCredentials(imapUser, imapPassword);
			jPM.setMessageCounterListerer(i.getInstance(EmailListener.class));
			jPM.run();
		}
		log.info("ServletContextListener started");
	}
	
    @Override
    protected Injector getInjector(){
        injector = Guice.createInjector(new ServletModule(){
            @Override
            protected void configureServlets(){
            	filter("/*").through(CorsFilter.class);
            	filter("/*").through(GuiceShiroFilter.class);
            	filter("/envayasms/*").through(DirectoryFilter.class);
            	filter("/api/*").through(DirectoryFilter.class);
            	filter("/parser/*").through(ParserAccessFilter.class); //make sure no-one can access those urls
            	filter("/parser/*").through(ParserFilter.class); //read message into dataset
            	filter("/parser/*").through(DirectoryFilter.class); //allow directory access
            	filter("/account/*").through(DirectoryFilter.class); //allow directory access
            	filter("/data/*").through(DirectoryFilter.class); //allow directory access
            	filter("/parser/*").through(InterpreterFilter.class); //do semantic stuff
            	bindListener(Matchers.any(), new SLF4JTypeListener());
        		bind(MessagingActivitiesImpl.class).annotatedWith(Names.named("activityImpl")).to(MessagingActivitiesImpl.class);
        		bind(ParserClient.class);
        	}
			
			@Provides
			@Singleton
			@SuppressWarnings("unused")
			public CommandParser getMessageProcessor() {
				return new CommandParser(servletContext);
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
			@Named("nonTx")
			@Singleton
			@SuppressWarnings("unused")
			public WorkflowWorker getDepositWorker(AmazonSimpleWorkflow swfClient) {
				WorkflowWorker workflowWorker = new WorkflowWorker(swfClient,
						domainName, "deposit-workflow-tasklist");
				try {
					workflowWorker
							.addWorkflowImplementationType(NonTxWorkflowImpl.class);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
				return workflowWorker;
			}

			@Provides
			@Named("withdrawal")
			@Singleton
			@SuppressWarnings("unused")
			public WorkflowWorker getWithdrawalWorker(AmazonSimpleWorkflow swfClient) {
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
			
			@Provides @Singleton @SuppressWarnings("unused")
			public NonTxWorkflowClientExternalFactoryImpl getDWorkflowClientExternal(
					AmazonSimpleWorkflow workflowClient) {
				return new NonTxWorkflowClientExternalFactoryImpl(
						workflowClient, domainName);
			}

			@Provides @Singleton @SuppressWarnings("unused")
			public WithdrawalWorkflowClientExternalFactoryImpl getSWorkflowClientExternal(
					AmazonSimpleWorkflow workflowClient) {
				return new WithdrawalWorkflowClientExternalFactoryImpl(
						workflowClient, domainName);
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			AccountPolicy providePolicy(){
				return new AccountPolicy()
					.setEmailMxLookup(true);
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
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
			public ActivityWorker getActivityWorker(AmazonSimpleWorkflow swfClient, 
					@Named("activityImpl") MessagingActivitiesImpl activitiesImpl) {
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
			public MessageFactory provideMessageFactory() {
				return new MessageFactory(servletContext);
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			public JndiLdapContextFactory provideLdapClientFactory(){
				JndiLdapContextFactory jlc = new JndiLdapContextFactory();
				jlc.setUrl(ldapUrl);
				jlc.setAuthenticationMechanism("simple");
				jlc.setSystemUsername(ldapUser);
				jlc.setSystemPassword(ldapPw);
				return jlc;
			}
        
        	@Provides @Singleton @SuppressWarnings("unused")
        	public Cache provideCache(){
        		//Create a singleton CacheManager using defaults
        		CacheManager manager = CacheManager.create();
        		//Create a Cache specifying its configuration.
        		Cache testCache = new Cache(new CacheConfiguration("cache", 1000)
        		    .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
        		    .eternal(false)
        		    .timeToLiveSeconds(7200)
        		    .timeToIdleSeconds(3600)
        		    .diskExpiryThreadIntervalSeconds(0));
        		  manager.addCache(testCache);
        		  return testCache;
        	}},new MessagingShiroWebModule(this.servletContext));
        return injector;
    }
	
    @Override
	public void contextDestroyed(ServletContextEvent sce) {
    	if (null!=jPM)
    		jPM.disconnect();
		try {
			activityWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
            System.out.println("Activity Worker Exited.");
            
		}catch (InterruptedException e) {
            e.printStackTrace();
        }
		super.contextDestroyed(sce);
		log.info("ServletContextListener destroyed");
	}

}
