package com._37coins;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.core.Form;

import junit.framework.Assert;

import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com._37coins.parse.CommandParser;
import com._37coins.parse.ParserAction;
import com._37coins.parse.ParserClient;
import com._37coins.persistence.dto.Account;
import com._37coins.resources.EnvayaSmsResource;
import com._37coins.resources.HealthCheckResource;
import com._37coins.resources.ParserResource;
import com._37coins.workflow.pojo.DataSet;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;

public class RestTest {	
	static String address = "821038492849";
	static String pw = "test9900";
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
        // Create the configuration to use for the server.
        InMemoryDirectoryServerConfig config =
             new InMemoryDirectoryServerConfig("dc=37coins,dc=com");
        config.addAdditionalBindCredentials("cn=admin", "test9900");
        InMemoryListenerConfig lc = new InMemoryListenerConfig("test", Inet4Address.getByName("127.0.0.1"), 51382, null,null,null);
        config.setListenerConfigs(lc);

        // Create the directory server instance, populate it with data from the
        // "test-data.ldif" file, and start listening for client connections.
        InMemoryDirectoryServer ds = new InMemoryDirectoryServer(config);
        ds.importFromLDIF(true, "src/test/resources/test-data.ldif");
        ds.startListening();
	}
    
    @AfterClass
    public static void afterClass() throws Exception {
        embeddedJetty.stop();
    }
    
    @Test
	public void testParserClient() throws NoSuchAlgorithmException, UnsupportedEncodingException{
    	ParserClient parserClient = new ParserClient(new CommandParser());
		parserClient.start("+821039842742", "+821027423984", "send 0.1 +821039842743", 8087,
		new ParserAction() {
			@Override
			public void handleWithdrawal(DataSet data) {
				//save the transaction id to db
				try {
					System.out.println(new ObjectMapper().writeValueAsString(data));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				Assert.assertFalse(data.getTo().getGateway().contains("+"));
			}
			@Override
			public void handleResponse(DataSet data) {
				try {
					System.out.println(new ObjectMapper().writeValueAsString(data));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				Assert.assertFalse(data.getTo().getGateway().contains("+"));
			}
			
			@Override
			public void handleDeposit(DataSet data) {
				try {
					System.out.println(new ObjectMapper().writeValueAsString(data));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				Assert.assertFalse(data.getTo().getGateway().contains("+"));
			}
			
			@Override
			public void handleConfirm(DataSet data) {
				try {
					System.out.println(new ObjectMapper().writeValueAsString(data));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				Assert.assertFalse(data.getTo().getGateway().contains("+"));
			}
		});
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
		String serverUrl = embeddedJetty.getBaseUri() + EnvayaSmsResource.PATH+"/OZV4N1JS2Z3476NL/sms";
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
	public void testUser() throws JsonParseException, JsonMappingException, IOException{
		//ask help
		given()
			.formParam("from", "test@test.com")
			.formParam("gateway", "mail@37coins.com")
			.formParam("message", "help")
		.expect()
			.statusCode(200)
			.body("size()", is(2))
			.body("[0].action", equalTo("Help"))
			.body("[0].cn", equalTo("test@test.com"))
			.body("[0].to.gateway", equalTo("mail@37coins.com"))
			.body("[1].action", equalTo("Signup"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/Help");		
		//get btc address
		given()
			.formParam("from", "test@test.com")
			.formParam("gateway", "mail@37coins.com")
			.formParam("message", "deposit")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("DepositReq"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/DepositReq");
		//send money, new user, same country
		given()
			.formParam("from", "test@test.com")
			.formParam("gateway", "mail@37coins.com")
			.formParam("message", "send 0.01 test2@test.com")
		.expect()
			.statusCode(200)
			.body("size()", is(2))
			.body("[0].action", equalTo("WithdrawalReq"))
			.body("[0].payload.payDest.address", equalTo("test2@test.com"))
			.body("[0].payload.amount", equalTo(0.01f))
			.body("[0].payload.fee", equalTo(0.0001f))
			.body("[0].payload.feeAccount", equalTo("MAILN1JS2Z34MAIL"))
			.body("[1].action", equalTo("Signup"))
			.body("[1].to.gateway", equalTo("mail@37coins.com"))
			.body("[1].cn", equalTo("test2@test.com"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
		//ask help
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "help")
		.expect()
			.statusCode(200)
			.body("size()", is(2))
			.body("[0].action", equalTo("Help"))
			.body("[0].cn", equalTo("821012345678"))
			.body("[0].to.gateway", equalTo("OZV4N1JS2Z3476NL"))
			.body("[1].action", equalTo("Signup"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/Help");
		//get btc address
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "deposit")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("DepositReq"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/DepositReq");
		//list last transactions
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "tx")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("Transactions"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/Transactions");
		//get account balance
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "balance")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action",  equalTo("Balance"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/Balance");
		//send money from mobile to email
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "send 0.01 test@test.com")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action",  equalTo("WithdrawalReq"))
			.body("[0].payload.payDest.address", equalTo("test@test.com"))
			.body("[0].payload.feeAccount", equalTo("OZV4N1JS2Z3476NL"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
		//send money from email to mobile
		given()
			.formParam("from", "test@test.com")
			.formParam("gateway", "mail@37coins.com")
			.formParam("message", "send 0.01 +821012345678")
		.expect()
			.statusCode(200)
				.body("[0].action",  equalTo("WithdrawalReq"))
				.body("[0].payload.payDest.address", equalTo("821012345678"))
				.body("[0].payload.feeAccount", equalTo("MAILN1JS2Z34MAIL"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
		//request money from email to mobile
		given()
			.formParam("from", "test@test.com")
			.formParam("gateway", "mail@37coins.com")
			.formParam("message", "request 0.01 +821012345678")
		.expect()
			.statusCode(200)
			.body("[0].action",  equalTo("WithdrawalReqOther"))
			.body("[0].payload.payDest.address", equalTo("test@test.com"))
			.body("[0].payload.feeAccount", equalTo("OZV4N1JS2Z3476NL"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReqOther");
		//send money from email to local number
		given()
			.formParam("from", "test@test.com")
			.formParam("gateway", "mail@37coins.com")
			.formParam("message", "send 0.01 01012345678")
		.expect()
			.statusCode(200)
			.body("[0].action",  equalTo("FormatError"))
			.body("[0].to.email.address", equalTo("test@test.com"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
		//send money, new user, same country
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "send 0.01 01087654321")
		.expect()
			.statusCode(200)
			.body("size()", is(2))
			.body("[0].action", equalTo("WithdrawalReq"))
			.body("[0].payload.payDest.address", equalTo("821087654321"))
			.body("[0].payload.amount", equalTo(0.01f))
			.body("[0].payload.fee", equalTo(0.0007f))
			.body("[0].payload.feeAccount", equalTo("OZV4N1JS2Z3476NL"))
			.body("[1].action", equalTo("Signup"))
			.body("[1].to.gateway", equalTo("OZV4N1JS2Z3476NL"))
			.body("[1].cn", equalTo("821087654321"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
		//send money, new user, other country
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "send 0.01 +491087654321")
		.expect()
			.statusCode(200)
			.body("size()", is(2))
			.body("[0].action", equalTo("WithdrawalReq"))
			.body("[1].action", equalTo("Signup"))
			.body("[1].locale", equalTo("de_DE"))
			.body("[1].to.gateway", equalTo("+491027423984"))
			.body("[1].cn", equalTo("491087654321"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
		//request money, existing user
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "request 0.01 +491087654321")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("WithdrawalReqOther"))
			.body("[0].to.phoneNumber.countryCode", equalTo(49))
			.body("[0].payload.payDest.address", equalTo("821012345678"))
			.body("[0].payload.msgDest.phoneNumber.countryCode", equalTo(82))
			.body("[0].payload.amount", equalTo(0.01f))
			.body("[0].payload.fee", equalTo(0.002f))
			.body("[0].payload.feeAccount", equalTo("DEV4N1JS2Z3476DE"))
			.body("[0].cn", equalTo("491087654321"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReqOther");
		//confirm a transaction
		given()
			.formParam("from", "+821012345678")
			.formParam("gateway", "+821027423984")
			.formParam("message", "conf test")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("WithdrawalConf"))
			.body("[0].payload", equalTo("test"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalConf");
		//ask help
		given()
			.formParam("from", "+821099999999")
			.formParam("gateway", "+821027423984")
			.formParam("message", "bla")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("Signup"))
			.body("[0].cn", equalTo("821099999999"))
			.body("[0].to.gateway", equalTo("OZV4N1JS2Z3476NL"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/UnknownCommand");
		//ask again
		given()
			.formParam("from", "+821099999999")
			.formParam("gateway", "+821027423984")
			.formParam("message", "bla")
		.expect()
			.statusCode(200)
			.body("size()", is(1))
			.body("[0].action", equalTo("UnknownCommand"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/UnknownCommand");
		//to little
		given()
			.formParam("from", "+821099999999")
			.formParam("gateway", "+821027423984")
			.formParam("message", "send 0.0001 +821012345678")
		.expect()
			.statusCode(200)
			.body("[0].action", equalTo("BelowFee"))
		.when()
			.post(embeddedJetty.getBaseUri() + ParserResource.PATH+"/WithdrawalReq");
	}

}
