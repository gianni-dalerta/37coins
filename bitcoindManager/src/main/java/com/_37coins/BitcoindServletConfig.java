package com._37coins;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.events.WalletListener;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.bcJsonRpc.pojo.Transaction.Category;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class BitcoindServletConfig extends GuiceServletContextListener {
	public static AWSCredentials awsCredentials=null;
	public static String domainName;
	public static String actListName = "bitcoind-activities-tasklist";
	public static String endpoint;
	public static URL bcdUrl;
	public static String bcdUser;
	public static String bcdPassword;
	public static Logger log = LoggerFactory.getLogger(BitcoindServletConfig.class);
	static {
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
	}
	private ActivityWorker activityWorker;
	private WalletListener listener;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		super.contextInitialized(servletContextEvent);
		final Injector i = getInjector();
		BitcoindInterface client = i.getInstance(BitcoindInterface.class);
		
		try {
			listener = new WalletListener(client);
			listener.addObserver(new Observer() {
				@Override
				public void update(Observable o, Object arg) {
					Transaction t = (Transaction)arg;
					//group transaction inputs by account
					Map<String,List<Transaction>> txGrouping = new HashMap<>();
					for (Transaction tx : t.getDetails()){
						if (tx.getCategory()==Category.RECEIVE && tx.getAccount()!=null){
							if (txGrouping.containsKey(tx.getAccount())){
								txGrouping.get(tx.getAccount()).add(tx);
							}else{
								List<Transaction> set = new ArrayList<>();
								set.add(tx);
								txGrouping.put(tx.getAccount(), set);
							}
						}
					}					
					//start a workflow for each account concerned by transaction
					for (Entry<String,List<Transaction>> e : txGrouping.entrySet()){
						BigDecimal sum = BigDecimal.ZERO.setScale(0); 
						for (Transaction tx : e.getValue()){
							sum = sum.add(tx.getAmount().setScale(8,RoundingMode.UNNECESSARY)).setScale(0, RoundingMode.UNNECESSARY);
						}
						DataSet rsp = new DataSet()
						.setCn(e.getKey())
						.setPayload(new Withdrawal()
							.setTxId(t.getTxid())
							.setAmount(sum.setScale(8, RoundingMode.FLOOR)))
						.setAction(Action.DEPOSIT_CONF);
						i.getInstance(NonTxWorkflowClientExternalFactoryImpl.class).getClient().executeCommand(rsp);
					}
				}
			});
			
			activityWorker = i.getInstance(ActivityWorker.class);
			activityWorker.start();
			
			log.info("ServletContextListener started");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new ServletModule() {
			
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
			public NonTxWorkflowClientExternalFactoryImpl getSWorkflowClientExternal(
					@Named("wfClient") AmazonSimpleWorkflow workflowClient) {
				return new NonTxWorkflowClientExternalFactoryImpl(
						workflowClient, domainName);
			}

			@Override
			public void configureServlets() {
				bind(BitcoindActivitiesImpl.class).annotatedWith(Names.named("activityImpl")).to(BitcoindActivitiesImpl.class);
			}

		});
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.contextDestroyed(sce);
		listener.stop();
		try {
			activityWorker.shutdownAndAwaitTermination(1, TimeUnit.MINUTES);
			System.out.println("Activity Worker Exited.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		log.info("ServletContextListener destroyed");
	}

}
