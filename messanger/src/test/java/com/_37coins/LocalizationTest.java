package com._37coins;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import com._37coins.pojo.SendAction;
import com._37coins.sendMail.EmailFactory;

import freemarker.template.TemplateException;

public class LocalizationTest {
	
	@Test
	public void test37coinsCreate() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "create");
		data.put("bcAddress", "1NNOUROEUtoEBUON2347");
		data.put("sendHash", "abcdneaoeus");
		data.put("url", "http://37coins.com/rest/something");
		SendAction sendAction = new SendAction(null);
		System.out.println("SIGNUP:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}
	
	@Test
	public void test37coinsDeposit() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "deposit");
		data.put("sendHash", "abcdneaoeus");
		data.put("url", "http://37coins.com/rest/something");
		SendAction sendAction = new SendAction(null);
		System.out.println("DEPOSIT REQUEST:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}
	
	@Test
	public void test37coinsHelp() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "help");
		data.put("sendHash", "abcdneaoeus");
		SendAction sendAction = new SendAction(null);
		System.out.println("HELP:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}
		
	@Test
	public void test37coinsReiceive() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "received");
		data.put("amount", 0.01);
		data.put("sendHash", "abcdneaoeus");
		data.put("transactionNumber", "12345");
		data.put("receiver", "other@37coins.com");
		data.put("url", "http://37coins.com/rest/something");
		SendAction sendAction = new SendAction(null);
		System.out.println("DEPOSIT CONFIRM:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}
	
	@Test
	public void test37coinsSend() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "send");
		data.put("amount", 0.01);
		data.put("sendHash", "abcdneaoeus");
		data.put("transactionNumber", "1NN2394238N");
		data.put("receiver", "other@37coins.com");
		data.put("url", "http://37coins.com/rest/something");
		SendAction sendAction = new SendAction(null);
		System.out.println("WITHDRAWAL CONFIRM:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}
	
	@Test
	public void test37coinsWethdrawalReq() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "confirmSend");
		data.put("amount", 0.01);
		data.put("sendHash", "abcdneaoeus");
		data.put("tasktoken", "1NN2394238N");
		data.put("receiver", "other@37coins.com");
		data.put("url", "http://37coins.com/rest/something");
		SendAction sendAction = new SendAction(null);
		System.out.println("WITHDRAWAL REQUEST:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}
	
	@Test
	public void test37coinsBalance() throws IOException, TemplateException {
		EmailFactory ef = new EmailFactory();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("locale", new Locale("ko"));
		data.put("service", "37coins");
		data.put("msgAddress", "test@37coins.com");
		data.put("action", "balance");
		data.put("balance", 0.01);
		data.put("sendHash", "abcdneaoeus");
		data.put("url", "http://37coins.com/rest/something");
		SendAction sendAction = new SendAction(null);
		System.out.println("BALANCE:");
		System.out.println(ef.constructTxt(data, sendAction));
		ef.constructHtml(data, sendAction);
		ef.constructSubject(data, sendAction);
	}

}
