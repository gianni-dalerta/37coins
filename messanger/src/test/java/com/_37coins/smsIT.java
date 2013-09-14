package com._37coins;

import static com.jayway.restassured.RestAssured.given;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com._37coins.envaya.Command;
import com._37coins.resources.EnvayaSmsResource;
import com._37coins.resources.GatewayResource;
import com._37coins.workflow.NonTxWorkflowClientExternalFactory;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.Deposit;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import freemarker.template.TemplateException;

public class smsIT {
	static String restUrl = System.getProperty("basePath")+"/rest";
	static Connection conn;
	static Channel channel;
	static final Set<String> response = new HashSet<>();
	static NonTxWorkflowClientExternalFactory factory;
	ObjectMapper om = new ObjectMapper();
	static Set<String> created = new HashSet<>();
	static BigDecimal FEE = new BigDecimal("0.0001").setScale(8);
	static String GATEWAY = "821012345678";
	static String SENDER1 = "01027423984";
	static String SENDER2 = "01023456789";
	
	
	
	@BeforeClass
	static public void startMqClient() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException{
		//initialize workflow starter
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
				System.getProperty("accessKey"),
				System.getProperty("secretKey"));
		AmazonSimpleWorkflow rv = new AmazonSimpleWorkflowClient(awsCredentials);
		rv.setEndpoint(System.getProperty("endpoint"));
		factory = new NonTxWorkflowClientExternalFactoryImpl(rv, System.getProperty("swfDomain"));
		//register gateway
		given()
			.formParam("ownerAddress", "schatzmeister@37coins.com")
			.formParam("address", GATEWAY)
			.formParam("fee", FEE)
		.when()
			.post(restUrl + GatewayResource.PATH);
		//connect to message bus
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(System.getProperty("queueUri"));
		conn = factory.newConnection();
		channel = conn.createChannel();
		channel.basicConsume("gateway000", new DefaultConsumer(channel){
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope,
					BasicProperties properties, byte[] body) throws IOException {
				String str = new String(body, "UTF-8");
				channel.basicAck(envelope.getDeliveryTag(), false);
				response.clear();
				response.add(str);
				synchronized (channel) {
					channel.notifyAll();
				}
			}
		});
	}
	
	@Before
	public void start(){
		response.clear();
	}
	
	@After
	public void clean(){
		response.clear();
	}
	
	@AfterClass
	static public void stopMqClient() throws IOException{
		channel.close();
		conn.close();
	}
	
	//#####################################  HELPERS
	
	public String read(){
		if (response.size()==0){
			synchronized (channel) {
				try {
					channel.wait(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		String rv = response.iterator().next();
		response.remove(rv);
		return rv;
	}
	
	public void create(String sender) throws JsonParseException, JsonMappingException, IOException{
		if (!created.contains(sender)){
			exec("create", GATEWAY, sender);
			String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
			System.out.println(message);
		}
	}
	
	public void exec(String cmd){
		exec(cmd, GATEWAY, SENDER1);
	}
	
	public void exec(String msg, String gateway, String user){
		given()
			.formParam("action", "incoming")
			.formParam("message", msg)
			.formParam("phone_number", gateway)
			.formParam("from", user)
			.formParam("message_type","sms")
		.expect()
			.statusCode(200)
		.when()
			.post(restUrl + EnvayaSmsResource.PATH);
	}
	
	//#####################################  TESTS
	
	@Test
	public void testEnvayaBalance() throws JsonParseException, JsonMappingException, IOException {
		create(SENDER1);
		exec("bal");
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertTrue(message.contains("BTC in your wallet."));
		Assert.assertTrue(message.getBytes().length<140);
	}
	
	@Test
	public void testEnvayaDeposit() throws JsonParseException, JsonMappingException, IOException {
		create(SENDER1);
		exec("adr");
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals(SENDER1, message);
		Assert.assertTrue(message.getBytes().length<140);
	}
	
	@Test
	public void testEnvayaHelp() throws InterruptedException, JsonParseException, JsonMappingException, IOException, TemplateException {
		exec("create");
		String createMsg = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("Welcome to 37Coins! Your global wallet.\n37Coins commands: bal, addr, send/request <amount> <receiver> [desc], conf <ref>", createMsg);
		Assert.assertTrue(createMsg.length()<160);
	}
	
	@Test
	public void testEnvayaHelpKorean() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		create(SENDER1);
		exec("%EB%8F%84%EC%9B%80"); //도움
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("37Coins 명령: 잔액조회, 주소, 송금/요청 <금액> <받는이> [서술], 확인 <언급>", message);
		Assert.assertTrue(message.length()<160);
	}
	
	@Test
	public void testEnvayaReceive() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		Long account = 1L;
		Response rsp = new Response()
			.setAccountId(account)
			.setPayload(new Deposit()
				.setAmount(new BigDecimal("0.5").setScale(8))
				.setTxId("1234"))
			.setAction(RspAction.RECEIVED);
		factory.getClient().executeCommand(rsp);
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("You have received 0.5 in your wallet.", message);
		Assert.assertTrue(message.length()<160);
	}
	
	@Test
	public void testEnvayaTransactions() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		create(SENDER1);
		exec("tx");
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("Transactions", message);
		Assert.assertTrue(message.length()<160);
	}
	
	@Test
	public void testEnvayaSend() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		create(SENDER2);
		create(SENDER1);
		exec("send 0.01 "+SENDER2);
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		System.out.println(message);
		Assert.assertTrue(message.contains("We have been ordered to transfer")); 
		Assert.assertTrue(message.contains("BTC from your account to "+SENDER2));
		String key = message.substring(message.indexOf("\"conf")+6, message.indexOf("\"conf")+11);
		exec("conf "+key);
		message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("We have transfered 0.01 BTC from your account to "+SENDER2+".", message);
		Assert.assertTrue(message.length()<160);
		message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("You have received 0.01 in your wallet.", message);
		Assert.assertTrue(message.length()<160);
	}
	
	@Test
	public void testInsufficientFunds() throws JsonParseException, JsonMappingException, IOException{
		BigDecimal amount = new BigDecimal("1000.01").setScale(8);
		create(SENDER2);
		create(SENDER1);
		exec("send "+amount.setScale(2)+" "+SENDER2);
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		System.out.println(message);
		Assert.assertTrue(message.contains("BTC, required for transaction: "+amount.add(FEE).setScale(4)+" BTC."));
		Assert.assertTrue(message.length()<160);
	}

}
