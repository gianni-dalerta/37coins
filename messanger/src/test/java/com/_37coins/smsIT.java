package com._37coins;

import static com.jayway.restassured.RestAssured.given;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com._37coins.envaya.Command;
import com._37coins.resources.EnvayaSmsResource;
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
	public final String restUrl = "http://localhost:8084/rest";
	static Connection conn;
	static Channel channel;
	static final Set<String> response = new HashSet<>();
	ObjectMapper om = new ObjectMapper();
	
	
	@BeforeClass
	static public void startMqClient() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri("amqp://guest:guest@localhost:5672");
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
	
	@AfterClass
	static public void stopMqClient() throws IOException{
		channel.close();
		conn.close();
	}
	
	public String read(){
		synchronized (channel) {
			try {
				channel.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return response.iterator().next();
	}
	
	//@Test
	public void testEnvayaBalance() {
		given()
			.formParam("action", "incoming")
			.formParam("message", "balance")
			.formParam("phone_number", "+821012345678")
			.formParam("from", "01027423984")
			.formParam("message_type","sms")
		.expect()
			.statusCode(200)
		.when()
			.post(restUrl + EnvayaSmsResource.PATH);
	}
	
	@Test
	public void testEnvayaHelp() throws InterruptedException, JsonParseException, JsonMappingException, IOException, TemplateException {
		given()
			.formParam("action", "incoming")
			.formParam("message", "help")
			.formParam("phone_number", "821012345678")
			.formParam("from", "01027423984")
			.formParam("message_type","sms")
		.expect()
			.statusCode(200)
		.when()
			.post(restUrl + EnvayaSmsResource.PATH);
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("37Coins commands: bal, addr, send/request <amount> <receiver> [desc], conf <ref>", message);
		Assert.assertTrue(message.length()<160);
	}
	
	@Test
	public void testEnvayaHelpKorean() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		given()
			.formParam("action", "incoming")
			.formParam("message", "도움")
			.formParam("phone_number", "821012345678")
			.formParam("from", "01027423984")
			.formParam("message_type","sms")
		.expect()
			.statusCode(200)
		.when()
			.post(restUrl + EnvayaSmsResource.PATH);
		String message = om.readValue(read(), Command.class).getMessages().get(0).getMessage();
		Assert.assertEquals("37Coins 명령: 잔액조회, 주소, 송금/요청 <금액> <받는이> [서술], 확인 <언급>", message);
		Assert.assertTrue(message.length()<160);
	}

}
