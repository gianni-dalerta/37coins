package com._37coins;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.Withdrawal;

import freemarker.template.TemplateException;

public class LocalizationTest {
	
	DataSet rsp;
	MessageFactory ef = new MessageFactory();
	
	@Before
	public void start(){
		rsp =  new DataSet()
		.setService("37coins")
		.setLocale(new Locale("en"))
		.setPayload(new PaymentAddress()
			.setAddress("mkGFr3M4HWy3NQm6LcSprcUypghQxoYmVq"))
		.setTo(new MessageAddress()
			.setAddress("test@37coins.com"));
	}
	
	@Test
	public void test37coinsCreate() throws IOException, TemplateException {
		rsp.setAction(Action.SIGNUP);
		System.out.println("SIGNUP:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsDeposit() throws IOException, TemplateException {
		rsp.setAction(Action.DEPOSIT_REQ);
		System.out.println("DEPOSIT REQ:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsHelp() throws IOException, TemplateException {
		rsp.setAction(Action.HELP);
		System.out.println("HELP:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
		
	@Test
	public void test37coinsReiceive() throws IOException, TemplateException {
		rsp.setAction(Action.DEPOSIT_CONF)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.05")));
		System.out.println("DEPOSIT CONFIRM:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsSend() throws IOException, TemplateException {
		rsp.setAction(Action.WITHDRAWAL_REQ)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.01"))
				.setMsgDest(new MessageAddress()
					.setAddress("other@37coins.com")));
		System.out.println("DEPOSIT CONFIRM:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsWithdrawalReq() throws IOException, TemplateException {
		rsp.setAction(Action.WITHDRAWAL_CONF)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.01"))
				.setConfKey("something")
				.setConfLink("http://37coins.com/rest/something")
				.setMsgDest(new MessageAddress()
					.setAddress("other@37coins.com")));
		System.out.println("WITHDRAWAL REQUEST:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsBalance() throws IOException, TemplateException {
		rsp.setAction(Action.BALANCE)
			.setPayload(new Withdrawal()
				.setBalance(new BigDecimal("0.05")));
		System.out.println("BALANCE:");
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void testInsuficcientFunds() throws IOException, TemplateException {
		rsp.setAction(Action.INSUFISSIENT_FUNDS)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("1000.051"))
				.setBalance(new BigDecimal("0.5123456789")));
		String s = ef.constructTxt(rsp);
		Assert.assertTrue(s.contains("1,000.051"));
		Assert.assertTrue(s.contains("0.51234568"));
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void testInsuficcientFundsDe() throws IOException, TemplateException {
		rsp.setAction(Action.INSUFISSIENT_FUNDS)
			.setLocale(new Locale("de"))
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("1000.051"))
				.setBalance(new BigDecimal("0.5123456789")));
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		Assert.assertTrue(s.contains("1.000,051"));
		Assert.assertTrue(s.contains("0,51234568"));
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void testTransactions() throws IOException, TemplateException {
		List<Transaction> list = new ArrayList<>();
		list.add(new Transaction().setTime(System.currentTimeMillis()-360000000L).setComment("hallo").setAmount(new BigDecimal("0.4")).setTo("hast@test.com"));
		list.add(new Transaction().setTime(System.currentTimeMillis()-760000000L).setComment("hallo").setAmount(new BigDecimal("0.3")).setTo("hast@test.com"));
		list.add(new Transaction().setTime(System.currentTimeMillis()-960000000L).setComment("hallo").setAmount(new BigDecimal("0.2")).setTo("hast@test.com"));
		list.add(new Transaction().setTime(System.currentTimeMillis()).setComment("hallo").setAmount(new BigDecimal("0.1")).setTo("hast@test.com"));
		rsp.setAction(Action.TRANSACTION)
			.setLocale(new Locale("de"))
			.setPayload(list);
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void testEmptyTransactions() throws IOException, TemplateException {
		List<Transaction> list = null;//new ArrayList<>();
		rsp.setAction(Action.TRANSACTION)
			.setLocale(new Locale("de"))
			.setPayload(list);
		String s = ef.constructTxt(rsp);
		System.out.println(s);
		ef.constructHtml(rsp);
		ef.constructSubject(rsp);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}

}
