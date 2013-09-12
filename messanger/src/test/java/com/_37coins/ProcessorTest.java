package com._37coins;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com._37coins.workflow.pojo.Withdrawal;

public class ProcessorTest{
	static MessageParser ep = new MessageParser();

	@Test
	public void testBalance() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {
				Assert.assertNotNull(req.getAccountId());
				Request expected = new Request()
					.setAction(ReqAction.BALANCE)
					.setLocale(new Locale("en"))
					.setAccountId(req.getAccountId())
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), " balance");
	}
	
	@Test
	public void testCreate() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {
				Assert.assertNotNull(req.getAccountId());
				Request expected = new Request()
					.setAction(ReqAction.CREATE)
					.setLocale(new Locale("en"))
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "Create");
	}
	
	@Test
	public void testHelp() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.HELP)
				.setLocale(new Locale("ko"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "도움");
	}
	
	@Test
	public void testError() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.CREATE)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "bla");
	}
	
	@Test
	public void testDeposit() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.DEPOSIT)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "DEPOSIT ");
	}
	
	@Test
	public void testSend() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.SEND)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("test2@37coins.com")));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 test2@37coins.com");
	}
	
	
	@Test
	public void testSendReverse() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.SEND)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("test2@37coins.com")));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send test2@37coins.com 0.1");
	}
	
	@Test
	public void testSendPhone() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.SEND)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("01029382039")));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 01029382039");
	}
	
	@Test
	public void testSendAddressValid() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.SEND)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf")));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf");
	}	
	
	@Test
	public void testSendAddressTestnet() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.SEND)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP")));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP");
	}	
	
	@Test
	public void testSendAddressWrong() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.FORMAT_ERROR)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP");
	}	
	
	@Test
	public void testRequest() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {
				Response expected = new Response()
				.setAction(RspAction.REQUEST)
				.setLocale(new Locale("en"))
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("test2@37coins.com")));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "request test2@37coins.com 0.1");
	}
}
