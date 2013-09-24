package com._37coins;

import static com.jayway.restassured.RestAssured.given;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Form;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restnucleus.dao.Model;
import org.restnucleus.test.DbHelper;
import org.restnucleus.test.EmbeddedJetty;

import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Gateway;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.persistence.dto.Transaction;
import com._37coins.persistence.dto.Transaction.State;
import com._37coins.plivo.GetDigits;
import com._37coins.plivo.Speak;
import com._37coins.plivo.Wait;
import com._37coins.resources.EnvayaSmsResource;
import com._37coins.resources.HealthCheckResource;
import com._37coins.resources.PlivoResource;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public class RestTest {	
	static String address = "821038492849";
	static String pw = "test";
	static Account acc1;
	
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
		List<Account> accounts = new ArrayList<>();
		acc1 = new Account();
		accounts.add(acc1);
		List<MsgAddress> addrs = new ArrayList<>();
		addrs.add(new MsgAddress().setAddress("01029382039").setOwner(accounts.get(0)).setGateway(gws.get(0)));
		List<Transaction> tx = new ArrayList<>();
		tx.add(new Transaction().setKey("abcd").setState(State.STARTED).setTaskToken("taskToken"));
		Map<Class<? extends Model>, List<? extends Model>> data = new HashMap<>();
		data.put(Account.class, accounts);
		data.put(Gateway.class,gws);
		data.put(MsgAddress.class, addrs);
		data.put(Transaction.class, tx);
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
		
		com._37coins.plivo.Response rv = new com._37coins.plivo.Response()
		.add(new Speak().setText("Welcome to 37 coins! To secure your transactions, this call will set up a 4 digit pin number."))
		.add(new Wait())
		.add(new GetDigits()
			.setAction(MessagingServletConfig.basePath+ "/plivo/0/abcd/create")
			.setNumDigits(4)
			.setRedirect(false)
			.setSpeak(new Speak()
				.setText("Please choose and enter a 4-digit pin number.")))
		.add(new Wait())
		.add(new GetDigits()
			.setAction(MessagingServletConfig.basePath+ "/plivo/0/abcd/confirm")
			.setNumDigits(4)
			.setRedirect(false)
			.setSpeak(new Speak()
				.setText("Ok! Please repeat your 4-digit pin.")))
		.add(new Speak().setText("Please Remember your pin for future transactions."));
		
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(com._37coins.plivo.Response.class,GetDigits.class,Speak.class,Wait.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	 
			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	 
			jaxbMarshaller.marshal(rv, System.out);
		}catch(Exception e){
			e.printStackTrace();
		}

		
		Response r = given()
			.formParam("Digits", 1234)
		.expect()
			.statusCode(200)
		.when()
			.post(embeddedJetty.getBaseUri() + PlivoResource.PATH+"/"+acc1.getId()+"/abcd/answer");
		
		
		
		System.out.println(r.asString());
	}

}
