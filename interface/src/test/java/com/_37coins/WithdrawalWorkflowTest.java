package com._37coins;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.mail.internet.AddressException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com._37coins.activities.BitcoindActivities;
import com._37coins.activities.MessagingActivities;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.bizLogic.NonTxWorkflowImpl;
import com._37coins.bizLogic.WithdrawalWorkflowImpl;
import com._37coins.workflow.WithdrawalWorkflowClient;
import com._37coins.workflow.WithdrawalWorkflowClientFactory;
import com._37coins.workflow.WithdrawalWorkflowClientFactoryImpl;
import com._37coins.workflow.pojo.Deposit;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatch;
import com.amazonaws.services.simpleworkflow.flow.junit.AsyncAssert;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;

@RunWith(FlowBlockJUnit4ClassRunner.class)
public class WithdrawalWorkflowTest {

	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	final List<Response> trace = new ArrayList<>();

	private WithdrawalWorkflowClientFactory workflowFactory = new WithdrawalWorkflowClientFactoryImpl();

	@Before
	public void setUp() throws Exception {
		// Create and register mock activity implementation to be used during
		// test run
		BitcoindActivities activities = new BitcoindActivities() {
			@Override
			public String sendTransaction(BigDecimal amount, BigDecimal fee,
					Long fromId, String toId, String toAddress, String id, String comment) {
				if (null!=amount && null!=fee && null!=fromId &&(null!=toId || null!=toAddress)){
					return "txid2038942304";
				}else{
					throw new RuntimeException("param missing");
				}
			}

			@Override
			public BigDecimal getAccountBalance(Long accountId) {
				return new BigDecimal("2.5");
			}

			@Override
			public String getNewAddress(Long accountId) {
				return null;
			}

			@Override
			public Long getAccount(String bcAddress) {
				if (bcAddress.equalsIgnoreCase("123")){
					return 2L;
				}
				return null;
			}

			@Override
			public List<Transaction> getAccountTransactions(Long accountId) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		MessagingActivities mailActivities = new MessagingActivities() {

			@Override
			public void sendMessage(Response rsp) {
				trace.add(rsp);
			}

			@Override
			public void sendConfirmation(Response rsp, String workflowId) {
				Withdrawal w = (Withdrawal)rsp.getPayload();
				w.setConfKey("123");
				w.setConfLink("http://test.com/123");
				trace.add(rsp);
			}
			@Override
			public Response readMessageAddress(Response data) {
				return data.setTo(new MessageAddress()
					.setAddress("")
					.setAddressType(MsgType.SMS)
					.setGateway(""));
			}
		};
		
		workflowTest.addActivitiesImplementation(activities);
		workflowTest.addActivitiesImplementation(mailActivities);
		workflowTest.addWorkflowImplementationType(WithdrawalWorkflowImpl.class);
		workflowTest.addWorkflowImplementationType(NonTxWorkflowImpl.class);
	}

	@After
	public void tearDown() throws Exception {
		// trace = null;
	}

	@Test
	public void testSend() throws AddressException {
		WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		Request req = new Request()
			.setAction(ReqAction.SEND)
			.setAccountId(0L)
			.setFrom(new MessageAddress()
				.setAddressType(MsgType.SMS))
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.5").setScale(8))
				.setFee(new BigDecimal("0.0005").setScale(8))
				.setFeeAccount("1")
				.setPayDest(new PaymentAddress()
					.setAddress("2")
					.setAddressType(PaymentType.ACCOUNT)));
		Promise<Void> booked = workflow.executeCommand(req);
		Response expected = new Response()
			.setAction(RspAction.SEND_CONFIRM)
			.setService("37coins")
			.setAccountId(0L)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.5").setScale(8))
				.setFee(new BigDecimal("0.0005").setScale(8))
				.setFeeAccount("1")
				.setConfKey("123")
				.setConfLink("http://test.com/123")
				.setPayDest(new PaymentAddress()
					.setAddress("2")
					.setAddressType(PaymentType.ACCOUNT)))
			.setTo(new MessageAddress()
				.setAddressType(MsgType.SMS));
		validate("send no confirm", expected, trace, booked);
	}
	
	
	
	@Test
	public void testInsufficientFunds() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Request req = new Request()
			.setAction(ReqAction.SEND)
			.setAccountId(0L)
			.setFrom(new MessageAddress()
				.setAddressType(MsgType.SMS))
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("100.005").setScale(8))
				.setFee(new BigDecimal("0.0005").setScale(8))
				.setFeeAccount("1")
				.setPayDest(new PaymentAddress()
					.setAddress("2")
					.setAddressType(PaymentType.ACCOUNT)));		
		new TryCatch() {
			@Override
			protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(req);
				AsyncAssert.assertEquals("should have aborded because receiver not found", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
            		Response expected = new Response()
	        			.setAction(RspAction.INSUFISSIENT_FUNDS)
	        			.setService("37coins")
	        			.setAccountId(0L)
	        			.setPayload(new Deposit()
	        				.setAmount(new BigDecimal("100.0055").setScale(8))
	        				.setBalance(new BigDecimal("2.5").setScale(8)))
	        			.setTo(new MessageAddress()
	        				.setAddressType(MsgType.SMS));
            		validate("insufficient funds", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}	

	@Asynchronous
	public void validate(String desc, Object expected, List<Response> l){
		AsyncAssert.assertEqualsWaitFor(desc, expected, l.get(0));
	}
	
	@Asynchronous
	public void validate(String desc, Object expected, List<Response> l,Promise<Void> booked){
		AsyncAssert.assertEqualsWaitFor(desc, expected, l.get(0), booked);
	}

}