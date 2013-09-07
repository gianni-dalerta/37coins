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
		+ "    \"locale\":\"en\""
		+ "}",o);
	}
	
	@Test
	public void testCreate() throws Exception {
		Object o =ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "Create");
		validate("{"
		+ "    \"action\": \"create\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\""
		+ "}",o);
	}
	
	@Test
	public void testHelp() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "HELP");
		validate("{"
		+ "    \"action\": \"help\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\""
		+ "}",o);
	}
	
	@Test
	public void testError() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "bla");
		validate("{"
		+ "    \"action\": \"error000\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\""
		+ "}",o);
	}
	
	@Test
	public void testDeposit() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "DEPOSIT ");
		validate("{"
		+ "    \"action\": \"deposit\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\""
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
		+ "    \"receiverEmail\":\"test2@37coins.com\""
		+ "}",o);
	}
	
	
	@Test
	public void testSendReverse() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send test2@37coins.com 0.1");
		validate("{"
		+ "    \"action\": \"send\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"amount\":0.1,"
		+ "    \"receiverEmail\":\"test2@37coins.com\""
		+ "}",o);
	}
	
	@Test
	public void testSendPhone() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 01029382039");
		validate("{"
		+ "    \"action\": \"send\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"amount\":0.1,"
		+ "    \"receiverPhone\":\"01029382039\""
		+ "}",o);
	}
	
	@Test
	public void testSendAddressValid() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf");
		validate("{"
		+ "    \"action\": \"send\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"amount\":0.1,"
		+ "    \"receiver\":\"1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf\""
		+ "}",o);
	}	
	
	@Test
	public void testSendAddressTestnet() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP");
		validate("{"
		+ "    \"action\": \"send\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"amount\":0.1,"
		+ "    \"receiver\":\"mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP\""
		+ "}",o);
	}	
	
	@Test
	public void testSendAddressWrong() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 31uEbMgunupShBVTewXjt123v5MndwfXhb");
		validate("{"
		+ "    \"locale\":\"en\","
		+ "    \"action\": \"error002\","
		+ "    \"msgAddress\":\"test@37coins.com\""
		+ "}",o);
	}	
	
	@Test
	public void testRequest() throws Exception {
		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "request test2@37coins.com 0.1");
		validate("{"
		+ "    \"action\": \"request\","
		+ "    \"msgAddress\":\"test@37coins.com\","
		+ "    \"locale\":\"en\","
		+ "    \"amount\":0.1,"
		+ "    \"receiverEmail\":\"test2@37coins.com\""
		+ "}",o);
	}
}
