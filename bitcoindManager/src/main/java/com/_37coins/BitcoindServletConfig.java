package com._37coins;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restnucleus.inject.ContextFactory;
import org.restnucleus.inject.PersistenceModule;
import org.restnucleus.servlet.RestletServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.events.WalletListener;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.resources.HealthCheckResource;
import com._37coins.workflow.DepositWorkflowClientExternal;
import com._37coins.workflow.DepositWorkflowClientExternalFactoryImpl;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
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

public class BitcoindServletConfig extends GuiceServletContextListener {
	public static AWSCredentials awsCredentials=null;
	public static String domainName;
	public static String actListName = "bitcoind-activities-tasklist";
	public static String endpoint;
	public static URL bcdUrl;
	public static String bcdUser;
	public static String bcdPassword;
	public static BigDecimal fee;
	public static String feeAddress;
	public static Logger log = LoggerFactory.getLogger(BitcoindServletConfig.class);
	static {
		JaxrsApiReader.setFormatString("");
		if (null!= System.getProperty("accessKey")){
			awsCredentials = new BasicAWSCredentials(
				System.getProperty("accessKey"),
				System.getProperty("secretKey"));
		}
		domainName = System.getProperty("swfDomain");
		endpoint = System.getProperty("endpoint");
		try {
			bcdUrl = new URL(System.getProperty("url"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		bcdUser = System.getProperty("user");
		bcdPassword = System.getProperty("password");
		fee = new BigDecimal(System.getProperty("fee"));
		feeAddress = System.getProperty("feeAddress");
	}
	private ServletContext servletContext;
	private ActivityWorker activityWorker;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		servletContext = servletContextEvent.getServletContext();
		super.contextInitialized(servletContextEvent);
		final Injector i = getInjector();
		activityWorker = i.getInstance(ActivityWorker.class);
		activityWorker.start();
		
		BitcoindInterface client = i.getInstance(BitcoindInterface.class);
		
		try {
			new WalletListener(client).addObserver(new Observer() {
				@SuppressWarnings("unchecked")
				@Override
				public void update(Observable o, Object arg) {
					Transaction t = (Transaction)arg;
					Map<String,Object> data = BitcoindClientFactory.txToMap(t);
					if (null!= data.get("receive") && ((List<Map<String,Object>>)data.get("receive")).size() > 0){
						i.getInstance(DepositWorkflowClientExternal.class).executeCommand(data);
					}else{
						System.out.println("received unrelevant transaction: "+data.get("txid"));
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
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
						JaxRsApplication.class, CoinApplication.class).build(
						ContextFactory.class));
				bind(BitcoindActivitiesImpl.class).annotatedWith(
						Names.named("activityImpl")).to(
						BitcoindActivitiesImpl.class);
			}

			@Override
			public Set<Class<?>> getClassList() {
				Set<Class<?>> cs = new HashSet<>();
				cs.add(HealthCheckResource.class);
				return cs;
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
			BitcoindInterface getClient(){
				BitcoindClientFactory bcf = null;
				try {
					bcf = new BitcoindClientFactory(bcdUrl, bcdUser, bcdPassword);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return bcf.getClient();
			}

			@Provides @Singleton @SuppressWarnings("unused")
			public ActivityWorker getActivityWorker(
					@Named("wfClient") AmazonSimpleWorkflow swfClient,
					@Named("activityImpl") BitcoindActivitiesImpl activitiesImpl) {
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
			
			@Provides @Singleton @SuppressWarnings("unused")
			public DepositWorkflowClientExternal getSWorkflowClientExternal(
					@Named("wfClient") AmazonSimpleWorkflow workflowClient) {
				return new DepositWorkflowClientExternalFactoryImpl(
						workflowClient, domainName).getClient();
			}

		});
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.contextDestroyed(sce);
		deregisterJdbc();
		try {
			activityWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
			System.out.println("Activity Worker Exited.");
		} catch (InterruptedException e) {
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
