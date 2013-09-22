package com._37coins;

import static com.jayway.restassured.RestAssured.given;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Form;

import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restnucleus.dao.Model;
import org.restnucleus.test.DbHelper;
import org.restnucleus.test.EmbeddedJetty;

import com._37coins.persistence.dto.Gateway;
import com._37coins.resources.EnvayaSmsResource;
import com._37coins.resources.HealthCheckResource;
import com._37coins.resources.PlivoResource;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public class RestTest {	
	static String address = "821038492849";
	static String pw = "test";
	
    private static EmbeddedJetty embeddedJetty;
    

    @BeforeClass
    public static void beforeClass() throws Exception {
        embeddedJetty = new EmbeddedJetty(){
        	@Override
        	public String setInitParam(ServletHolder holder) {
        		holder.setInitParameter("javax.ws.rs.Application", "com._37coins.TestApplication");
        		return "src/test/webapp";
        	}
        };
        embeddedJetty.start();
        List<Gateway> gws = new ArrayList<>();
		gws.add(new Gateway().setAddress(address).setPassword(pw));
		Map<Class<? extends Model>, List<? extends Model>> data = new HashMap<>();
		data.put(Gateway.class, gws);
        new DbHelper(embeddedJetty.getDao()).persist(data);
	}
    
    @AfterClass
    public static void afterClass() throws Exception {
        embeddedJetty.stop();
    }
	
	@Test
	public void testSignature() throws NoSuchAlgorithmException, UnsupportedEncodingException{
		given()
		.expect()
			.statusCode(200)
		.when()
			.get(embeddedJetty.getBaseUri() + HealthCheckResource.PATH);
		Form m = new Form();
		m.param("version", "0.1");
		m.param("now","12356789");
		m.param("power","30");
		m.param("action","status");	
		String serverUrl = embeddedJetty.getBaseUri() + EnvayaSmsResource.PATH+"/"+address+"/sms";
		String sig = EnvayaSmsResource.calculateSignature(serverUrl, m.asMap(), pw);
		// fire get successfully
		given()
			.contentType(ContentType.URLENC)
			.header("X-Request-Signature", sig)
			.formParam("version", m.asMap().getFirst("version"))
			.formParam("now", m.asMap().getFirst("now"))
			.formParam("power", m.asMap().getFirst("power"))
			.formParam("action", m.asMap().getFirst("action"))
		.expect()
			.statusCode(200)
		.when()
			.post(serverUrl);
	}
	
	@Test
	public void testHelthcheck(){
		given()
		.expect()
			.statusCode(200)
		.when()
			.get(embeddedJetty.getBaseUri() + HealthCheckResource.PATH);
	}
	
	@Test
	public void testXml(){
		Response r = given()
		.expect()
			.statusCode(200)
		.when()
			.get(embeddedJetty.getBaseUri() + PlivoResource.PATH);
		System.out.println(r.asString());
	}

}
