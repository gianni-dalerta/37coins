package com._37coins;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.restnucleus.log.SLF4JTypeListener;

import com._37coins.envaya.QueueClient;
import com._37coins.parse.CommandParser;
import com._37coins.parse.InterpreterFilter;
import com._37coins.parse.ParserClient;
import com._37coins.parse.ParserFilter;
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
	            	filter("/envayasms/*").through(DirectoryFilter.class);
	            	//filter("/parser/*").through(ParserAccessFilter.class); //make sure no-one can access those urls
	            	filter("/parser/*").through(ParserFilter.class); //read message into dataset
	            	filter("/parser/*").through(DirectoryFilter.class); //allow directory access
	            	filter("/parser/*").through(InterpreterFilter.class); //do semantic stuff
	            	bindListener(Matchers.any(), new SLF4JTypeListener());
	            	bind(ParserClient.class);
	        	}
				
				@Provides
				@Singleton
				@SuppressWarnings("unused")
				public CommandParser getMessageProcessor() {
				  return new CommandParser();
				}
				
				@Provides @Singleton @SuppressWarnings("unused")
				public JndiLdapContextFactory provideLdapClientFactory(){
					JndiLdapContextFactory jlc = new JndiLdapContextFactory();
					jlc.setUrl(MessagingServletConfig.ldapUrl);
					jlc.setAuthenticationMechanism("simple");
					jlc.setSystemUsername(MessagingServletConfig.ldapUser);
					jlc.setSystemPassword(MessagingServletConfig.ldapPw);
					return jlc;
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
	        	}
			});
		return injector;
	}

}
