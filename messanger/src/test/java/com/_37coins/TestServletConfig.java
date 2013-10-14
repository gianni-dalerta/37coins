package com._37coins;

import javax.jdo.PersistenceManagerFactory;

import org.restnucleus.PersistenceConfiguration;
import org.restnucleus.filter.PersistenceFilter;
import org.restnucleus.log.SLF4JTypeListener;

import com._37coins.envaya.QueueClient;
import com._37coins.parse.CommandParser;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class TestServletConfig extends GuiceServletContextListener {
	
	public static Injector injector;

	@Override
	protected Injector getInjector() {
		final String restUrl = "http://localhost:8080";
		 injector = Guice.createInjector(new ServletModule(){
	            @Override
	            protected void configureServlets(){
	            	filter("/*").through(PersistenceFilter.class);
	            	bindListener(Matchers.any(), new SLF4JTypeListener());
	        	}
	            
				@Provides @Singleton @SuppressWarnings("unused")
				PersistenceManagerFactory providePersistence(){
					PersistenceConfiguration pc = new PersistenceConfiguration();
					pc.createEntityManagerFactory();
					return pc.getPersistenceManagerFactory();
				}
				
				@Provides
				@Singleton
				@SuppressWarnings("unused")
				public CommandParser getMessageProcessor() {
				  return new CommandParser();
				}
				
				@Provides @Singleton @SuppressWarnings("unused")
				public NonTxWorkflowClientExternalFactoryImpl getDWorkflowClientExternal(
				    AmazonSimpleWorkflow workflowClient) {
				  return new NonTxWorkflowClientExternalFactoryImpl(
				      workflowClient, restUrl);
				}
				@Provides @Singleton @SuppressWarnings("unused")
				public WithdrawalWorkflowClientExternalFactoryImpl getSWorkflowClientExternal(
				    AmazonSimpleWorkflow workflowClient) {
				  return new WithdrawalWorkflowClientExternalFactoryImpl(
				      workflowClient, restUrl);
				}
				
				@Provides @Singleton @SuppressWarnings("unused")
				AmazonSimpleWorkflow getSimpleWorkflowClient() {
				  return new AmazonSimpleWorkflowClient();
				}
				
				@Provides @Singleton @SuppressWarnings("unused")
				public MessageFactory provideMessageFactory() {
					return new MessageFactory();
				}
				
				@Provides @Singleton @SuppressWarnings("unused")
				public QueueClient provideMessageFactory(MessageFactory mf) {
					return new QueueClient(mf);
				}				
			});
		return injector;
	}

}
