package com._37coins;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com._37coins.pojo.SendAction;
import com._37coins.sendMail.EmailFactory;
import com._37coins.workflow.pojo.Deposit;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com._37coins.workflow.pojo.Withdrawal;

import freemarker.template.TemplateException;

public class LocalizationTest {
	
	Response rsp;
	EmailFactory ef = new EmailFactory();
	SendAction sendAction = new SendAction(null);
	
	@Before
	public void start(){
		rsp =  new Response()
		.setService("37coins")
		.setLocale(new Locale("en"))
		.setBizUrl("http://37coins.com/")
		.setSendHash("ao87u9o8u0oa8eu7098o")
		.setPayload(new PaymentAddress()
			.setAddress("mkGFr3M4HWy3NQm6LcSprcUypghQxoYmVq"))
		.setTo(new MessageAddress()
			.setAddress("test@37coins.com"));
	}
	
	@Test
	public void test37coinsCreate() throws IOException, TemplateException {
		rsp.setAction(RspAction.CREATE);
		System.out.println("SIGNUP:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsDeposit() throws IOException, TemplateException {
		rsp.setAction(RspAction.DEPOSIT);
		System.out.println("DEPOSIT REQ:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsHelp() throws IOException, TemplateException {
		rsp.setAction(RspAction.HELP);
		System.out.println("HELP:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
		
	@Test
	public void test37coinsReiceive() throws IOException, TemplateException {
		rsp.setAction(RspAction.RECEIVED)
			.setPayload(new Deposit()
				.setAmount(new BigDecimal("0.05")));
		System.out.println("DEPOSIT CONFIRM:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsSend() throws IOException, TemplateException {
		rsp.setAction(RspAction.SEND)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.01"))
				.setMsgDest(new MessageAddress()
					.setAddress("other@37coins.com")));
		System.out.println("DEPOSIT CONFIRM:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsWithdrawalReq() throws IOException, TemplateException {
		rsp.setAction(RspAction.SEND_CONFIRM)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.01"))
				.setTaskToken("1NN2394238N")
				.setConfLink("http://37coins.com/rest/something")
				.setMsgDest(new MessageAddress()
					.setAddress("other@37coins.com")));
		System.out.println("WITHDRAWAL REQUEST:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}
	
	@Test
	public void test37coinsBalance() throws IOException, TemplateException {
		rsp.setAction(RspAction.BALANCE)
			.setPayload(new Deposit()
				.setAmount(new BigDecimal("0.05")));
		System.out.println("BALANCE:");
		String s = ef.constructTxt(rsp, sendAction);
		System.out.println(s);
		ef.constructHtml(rsp, sendAction);
		ef.constructSubject(rsp, sendAction);
		Assert.assertTrue("SMS to long",s.getBytes().length<140);
	}

}
