package com._37coins;

import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;


import org.junit.Test;

import com._37coins.MessageProcessor;
import com._37coins.test.SnsTestHelper;

public class ProcessorTest extends SnsTestHelper{
	MessageProcessor ep = new MessageProcessor();


	@Test
	public void testBalance() throws Exception {
		Map<String,Object> o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, " balance");
		validate("{"
		+ "    \"action\": \"balance\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"source\":\"email\""
		+ "}",o);
	}
	
	@Test
	public void testCreate() throws Exception {
		Object o =ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "Create");
		validate("{"
		+ "    \"action\": \"create\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"source\":\"email\""
		+ "}",o);
	}
	
	@Test
	public void testHelp() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "HELP");
		validate("{"
		+ "    \"action\": \"help\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"service\":\"37coins\","
		+ "    \"locale\":\"en\""
		+ "}",o);
	}
	
	@Test
	public void testError() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "bla");
		validate("{"
		+ "    \"action\": \"error000\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"error\":\"000\","
		+ "    \"service\":\"37coins\","
		+ "    \"devErrorMsg\":\"unknown command\""
		+ "}",o);
	}
	
	@Test
	public void testDeposit() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "DEPOSIT ");
		validate("{"
		+ "    \"action\": \"deposit\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"source\":\"email\""
		+ "}",o);
	}
	
	@Test
	public void testSend() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 test2@37coins.com");
		validate("{"
		+ "    \"action\": \"send\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"amount\":0.1,"
		+ "    \"receiver\":\"test2@37coins.com\","
		+ "    \"source\":\"email\""
		+ "}",o);
	}
}
