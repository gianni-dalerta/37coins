package com._37coins;

import static com.jayway.restassured.RestAssured.given;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.restnucleus.dao.Model;
import org.restnucleus.inject.PersistenceModule;
import org.restnucleus.log.SLF4JTypeListener;
import org.restnucleus.test.AbstractDataHelper;

import com._37coins.parse.MessageParser;
import com._37coins.persistence.dto.Gateway;
import com._37coins.resources.EnvayaSmsResource;
import com._37coins.resources.HealthCheckResource;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.jayway.restassured.http.ContentType;

public class EnvayaSignatureTest extends AbstractDataHelper {
	
	@Override
	public Map<Class<? extends Model>, List<? extends Model>> getData() {
		List<Gateway> gws = new ArrayList<>();
		gws.add(new Gateway().setAddress(address).setPassword(pw));
		Map<Class<? extends Model>, List<? extends Model>> data = new HashMap<>();
		data.put(Gateway.class, gws);
		return data;
	}

	@Override
	public AbstractModule getModule() {
		return new PersistenceModule() {
			@Override
			protected void configure() {
				bindListener(Matchers.any(), new SLF4JTypeListener());
				super.configure();
			}
			
			@Override
			public Set<Class<?>> getClassList() {
				Set<Class<?>> cs = new HashSet<>();
				cs.add(EnvayaSmsResource.class);
				cs.add(HealthCheckResource.class);
				return cs;
			}
			
			@Provides
			@Singleton
			@SuppressWarnings("unused")
			public MessageParser getMessageProcessor() {
				return new MessageParser();
			}
			
			@Provides @Singleton @SuppressWarnings("unused")
			public NonTxWorkflowClientExternalFactoryImpl getDWorkflowClientExternal(
					@Named("wfClient") AmazonSimpleWorkflow workflowClient) {
				return new NonTxWorkflowClientExternalFactoryImpl(
						workflowClient, restUrl);
			}

			@Provides @Singleton @SuppressWarnings("unused")
			public WithdrawalWorkflowClientExternalFactoryImpl getSWorkflowClientExternal(
					@Named("wfClient") AmazonSimpleWorkflow workflowClient) {
				return new WithdrawalWorkflowClientExternalFactoryImpl(
						workflowClient, restUrl);
			}
			
			@Provides @Named("wfClient") @Singleton @SuppressWarnings("unused")
			AmazonSimpleWorkflow getSimpleWorkflowClient() {
				return new AmazonSimpleWorkflowClient();
			}
		};
	}
	
	static String address = "821038492849";
	static String pw = "test";
	
	@Test
	public void testSignature() throws NoSuchAlgorithmException, UnsupportedEncodingException{
		given()
		.expect()
			.statusCode(200)
		.when()
			.get(restUrl + HealthCheckResource.PATH);
		Map<String,String> m = new HashMap<>();
		m.put("version", "0.1");
		m.put("now","12356789");
		m.put("power","30");
		m.put("action","status");	
		String serverUrl = restUrl + EnvayaSmsResource.PATH+"/"+address+"/sms";
		String sig = EnvayaSmsResource.calculateSignature(serverUrl, m, pw);
		// fire get successfully
		given()
			.contentType(ContentType.URLENC)
			.header("X-Request-Signature", sig)
			.formParam("version", m.get("version"))
			.formParam("now", m.get("now"))
			.formParam("power", m.get("power"))
			.formParam("action", m.get("action"))
		.expect()
			.statusCode(200)
		.when()
			.post(serverUrl);
	}

}
