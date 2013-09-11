package com._37coins;

import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.junit.Assert;
import org.junit.Test;

import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;

public class ProcessorTest{
	MessageParser ep = new MessageParser();

	@Test
	public void testBalance() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {
				Request expected = new Request()
					.setAction(ReqAction.BALANCE)
					.setLocale(new Locale("en"))
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), " balance");
	}
	
//	@Test
//	public void testCreate() throws Exception {
//		Object o =ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "Create");
//		validate("{"
//		+ "    \"action\": \"create\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\""
//		+ "}",o);
//	}
//	
//	@Test
//	public void testHelp() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "도움");
//		validate("{"
//		+ "    \"action\": \"help\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"ko\""
//		+ "}",o);
//	}
//	
//	@Test
//	public void testError() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "bla");
//		validate("{"
//		+ "    \"action\": \"error000\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\""
//		+ "}",o);
//	}
//	
//	@Test
//	public void testDeposit() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "DEPOSIT ");
//		validate("{"
//		+ "    \"action\": \"deposit\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\""
//		+ "}",o);
//	}
//	
//	@Test
//	public void testSend() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 test2@37coins.com");
//		validate("{"
//		+ "    \"action\": \"send\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\","
//		+ "    \"amount\":0.1,"
//		+ "    \"receiverEmail\":\"test2@37coins.com\""
//		+ "}",o);
//	}
//	
//	
//	@Test
//	public void testSendReverse() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send test2@37coins.com 0.1");
//		validate("{"
//		+ "    \"action\": \"send\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\","
//		+ "    \"amount\":0.1,"
//		+ "    \"receiverEmail\":\"test2@37coins.com\""
//		+ "}",o);
//	}
//	
//	@Test
//	public void testSendPhone() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 01029382039");
//		validate("{"
//		+ "    \"action\": \"send\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\","
//		+ "    \"amount\":0.1,"
//		+ "    \"receiverPhone\":\"01029382039\""
//		+ "}",o);
//	}
//	
//	@Test
//	public void testSendAddressValid() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf");
//		validate("{"
//		+ "    \"action\": \"send\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\","
//		+ "    \"amount\":0.1,"
//		+ "    \"receiver\":\"1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf\""
//		+ "}",o);
//	}	
//	
//	@Test
//	public void testSendAddressTestnet() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP");
//		validate("{"
//		+ "    \"action\": \"send\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\","
//		+ "    \"amount\":0.1,"
//		+ "    \"receiver\":\"mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP\""
//		+ "}",o);
//	}	
//	
//	@Test
//	public void testSendAddressWrong() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "send 0.1 31uEbMgunupShBVTewXjt123v5MndwfXhb");
//		validate("{"
//		+ "    \"locale\":\"en\","
//		+ "    \"action\": \"error002\","
//		+ "    \"msgAddress\":\"test@37coins.com\""
//		+ "}",o);
//	}	
//	
//	@Test
//	public void testRequest() throws Exception {
//		Object o = ep.process(new Address[]{new InternetAddress("test@37coins.com")}, "request test2@37coins.com 0.1");
//		validate("{"
//		+ "    \"action\": \"request\","
//		+ "    \"msgAddress\":\"test@37coins.com\","
//		+ "    \"locale\":\"en\","
//		+ "    \"amount\":0.1,"
//		+ "    \"receiverEmail\":\"test2@37coins.com\""
//		+ "}",o);
//	}
}
