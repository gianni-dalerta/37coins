package com._37coins;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restnucleus.dao.Model;
import org.restnucleus.inject.ContextFactory;
import org.restnucleus.inject.PersistenceModule;
import org.restnucleus.test.AbstractDataHelper;

import com._37coins.persistence.dto.MailAddress;
import com._37coins.persistence.dto.SendJournal;
import com._37coins.pojo.SendAction;
import com._37coins.pojo.ServiceEntry;
import com._37coins.pojo.ServiceList;
import com._37coins.resources.HealthCheckResource;
import com._37coins.resources.PreferenceResource;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.jayway.restassured.http.ContentType;

public class PreferenceTest  extends AbstractDataHelper {	

	
	@Override
	public String getPath(){
		return "/rest";
	}
	
	@Override
	public AbstractModule getModule() {
		return new PersistenceModule() {
			@Override
			public Set<Class<?>> getClassList() {
				Set<Class<?>> cs = new HashSet<>();
				cs.add(PreferenceResource.class);
				cs.add(HealthCheckResource.class);
				return cs;
			}
			@Override
			protected void configure() {
				install(new FactoryModuleBuilder()
			     .implement(JaxRsApplication.class, CoinsApplication.class)
			     .build(ContextFactory.class));
			}
			@Provides @Singleton @SuppressWarnings("unused")
			public List<ServiceEntry> provideCategories() {
				return ServiceList.initialize(null);
			}
			@Provides @Singleton @SuppressWarnings("unused")
			public SendAction provideSendAction() {
				SendAction sa = new SendAction(null);
				return sa;
			}
		};
	}

	@Override
	@Before
	public void create() throws Exception {
		super.create();
	}
	
	public static List<ServiceEntry> sl;
	
	@Override
	public Map<Class<? extends Model>, List<? extends Model>> getData() {
		sl = ServiceList.initialize(null);
		List<MailAddress> mails = new ArrayList<>();
		MailAddress ma = MailAddress.prepareNewMail("test@37coins.com", new ServiceList("37coins", sl));
		mails.add(ma);
		List<SendJournal> journals = new ArrayList<>();
		journals.add(new SendJournal().setHash("123").setDestination(ma));
		Map<Class<? extends Model>, List<? extends Model>> data = new HashMap<Class<? extends Model>, List<? extends Model>>();
		data.put(MailAddress.class, mails);
		data.put(SendJournal.class, journals);
		return data;
	}
	
	@Test
	public void testGet() {
		given()
			.contentType(ContentType.JSON)
			.queryParam("filter", "hash=123")
			.queryParam("service", "37coins")
			.queryParam("address", "test@37coins.com")
		.expect()
			.statusCode(200)
			.body("newsletter", equalTo("newsletter"))
		.when()
			.get(restUrl + PreferenceResource.PATH);
	}
	@Test
	public void testPut() {
		given()
			.contentType(ContentType.JSON)
			.queryParam("hash", "123")
			.queryParam("service", "37coins")
			.queryParam("address", "test@37coins.com")
			.body("[\"newsletter\"]")
		.expect()
			.statusCode(200)
		.when()
			.put(restUrl + PreferenceResource.PATH);
		given()
			.contentType(ContentType.JSON)
			.queryParam("filter", "hash=123")
			.queryParam("service", "37coins")
			.queryParam("address", "test@37coins.com")
		.expect()
			.statusCode(200)
			.body(equalTo("{\"newsletter\":\"newsletter\"}"))
		.when()
			.get(restUrl + PreferenceResource.PATH);
	}
}
