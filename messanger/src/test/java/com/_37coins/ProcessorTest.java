package com._37coins;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.Model;

import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com._37coins.workflow.pojo.Withdrawal;

public class ProcessorTest{
	static MessageParser ep =null;
	GenericRepository gr = null;
	
	@Before
	public void before(){
		if (ep==null){
			ep =  new MessageParser();
			gr = new GenericRepository();
			List<Account> accounts = new ArrayList<>();
			accounts.add(new Account());
			List<MsgAddress> addrs = new ArrayList<>();
			addrs.add(new MsgAddress().setAddress("test@37coins.com").setOwner(accounts.get(0)));
			Map<Class<? extends Model>, List<? extends Model>> data = new HashMap<Class<? extends Model>, List<? extends Model>>();
			data.put(Account.class, accounts);
			data.put(MsgAddress.class, addrs);
			persist(data);
		}
	}
	
	private <E extends Model> void persist(
			Map<Class<? extends Model>, List<? extends Model>> data) {
		if (null != data) {
			for (Entry<Class<? extends Model>, List<? extends Model>> e : data
					.entrySet()) {
				try {
					if (null == e.getValue() || e.getValue().size() < 1) {
						System.out.println("no data found for: " + e.getKey().getSimpleName());
					} else {
						for (Model m : e.getValue()) {
							if (m.getId() == null)
								gr.add(m);
						}
						System.out.println(e.getKey().getSimpleName()
								+ " populated with " + e.getValue().size()
								+ " entities");
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}

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
					.setAccountId(0L)
					.setService("37coins")
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
				.setService("37coins")
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "도움");
	}
	
	@Test
	public void testNonExistingCommand() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {
				Request expected = new Request()
					.setAction(ReqAction.CREATE)
					.setAccountId(1L)
					.setLocale(new Locale("en"))
					.setFrom(new MessageAddress()
						.setAddress("test3@37coins.com"));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test3@37coins.com"), "bla");
	}
	
	@Test
	public void testDeposit() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {Assert.assertFalse(true);}
			@Override
			public void startDeposit(Request req) {
				Request expected = new Request()
					.setAction(ReqAction.DEPOSIT)
					.setLocale(new Locale("en"))
					.setAccountId(0L)
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "DEPOSIT ");
	}
	
	@Test
	public void testSend() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {
				Request expected = new Request()
				.setAction(ReqAction.SEND)
				.setLocale(new Locale("en"))
				.setService("37coins")
				.setAccountId(0L)
				.setFrom(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("test2@37coins.com")));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 test2@37coins.com");
	}
	
	
	@Test
	public void testSendReverse() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {
				Request expected = new Request()
				.setAction(ReqAction.SEND)
				.setLocale(new Locale("en"))
				.setService("37coins")
				.setAccountId(0L)
				.setFrom(new MessageAddress()
					.setAddress("test@37coins.com"))
				.setPayload(new Withdrawal()
					.setAmount(new BigDecimal("0.1"))
					.setMsgDest(new MessageAddress()
						.setAddress("test2@37coins.com")));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send test2@37coins.com 0.1");
	}
	
	@Test
	public void testSendPhone() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {
				Request expected = new Request()
					.setAction(ReqAction.SEND)
					.setLocale(new Locale("en"))
					.setAccountId(0L)
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"))
					.setPayload(new Withdrawal()
						.setAmount(new BigDecimal("0.1"))
						.setMsgDest(new MessageAddress()
							.setAddress("01029382039")));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 01029382039");
	}
	
	@Test
	public void testSendAddressValid() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {
				Request expected = new Request()
					.setAction(ReqAction.SEND)
					.setLocale(new Locale("en"))
					.setAccountId(0L)
					.setService("37coins")
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"))
					.setPayload(new Withdrawal()
						.setAmount(new BigDecimal("0.1"))
						.setPayDest(new PaymentAddress()
							.setAddress("1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf")
							.setAddressType(PaymentType.BTC)));
				Assert.assertEquals(expected, req);
				}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 1BLyr8ydFDcbgU9TUPy7NiGSCbq89hBiUf");
	}	
	
	@Test
	public void testSendAddressTestnet() throws Exception {
		RequestInterpreter ri = new RequestInterpreter(ep) {
			@Override
			public void startWithdrawal(Request req) {
				Request expected = new Request()
					.setAction(ReqAction.SEND)
					.setLocale(new Locale("en"))
					.setAccountId(0L)
					.setFrom(new MessageAddress()
						.setAddress("test@37coins.com"))
					.setPayload(new Withdrawal()
						.setAmount(new BigDecimal("0.1"))
						.setPayDest(new PaymentAddress()
							.setAddress("mhYxdhvp9kuLypKC3ux6oMPyKTfGm5GaVP")
							.setAddressType(PaymentType.BTC)));
				Assert.assertEquals(expected, req);
			}
			@Override
			public void startDeposit(Request req) {Assert.assertFalse(true);}
			@Override
			public void respond(Response rsp) {Assert.assertFalse(true);}
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
				.setService("37coins")
				.setTo(new MessageAddress()
					.setAddress("test@37coins.com"));
			Assert.assertEquals(expected, rsp);
			}
		};
		ri.process(new MessageAddress().setAddress("test@37coins.com"), "send 0.1 mhYxdhvp9kuLypKC3u123MPyKTfGm5GaVP");
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
