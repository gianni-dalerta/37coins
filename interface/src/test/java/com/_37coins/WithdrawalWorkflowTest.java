package com._37coins;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.mail.internet.AddressException;

import junit.framework.Assert;

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
import com._37coins.workflow.WithdrawalWorkflow;
import com._37coins.workflow.WithdrawalWorkflowClient;
import com._37coins.workflow.WithdrawalWorkflowClientFactory;
import com._37coins.workflow.WithdrawalWorkflowClientFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
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

	final List<DataSet> trace = new ArrayList<>();
	
	public static BigDecimal FEE = new BigDecimal("0.0005").setScale(8);
	
	// a test user that has 2.5 BTC in his wallet
	// and outgoing tx volume of 0.2
	public static Withdrawal USER1 = new Withdrawal()
			.setMsgDest(new MessageAddress()
				.setAddressType(MsgType.SMS))
			.setPayDest(new PaymentAddress()
				.setAddress("1")
				.setAddressType(PaymentType.ACCOUNT))
			.setBalance(new BigDecimal("2.5").setScale(8))
			//for 24h transaction volume
			.setAmount(new BigDecimal("0.1").setScale(8));
	
	// another user 
	// if this user sends a transaction, he always rejects it later
	public static Withdrawal USER2 = new Withdrawal()
		.setMsgDest(new MessageAddress()
			.setAddressType(MsgType.SMS))
		.setPayDest(new PaymentAddress()
			.setAddress("2")
			.setAddressType(PaymentType.ACCOUNT));	

	// sending to this user will always give a network error
	public static Withdrawal USER3 = new Withdrawal()
		.setPayDest(new PaymentAddress().setAddress("n3Rf315KpvWR7cq2VnZkgkC11KARck2rS4").setAddressType(PaymentType.BTC));
	

	private WithdrawalWorkflowClientFactory workflowFactory = new WithdrawalWorkflowClientFactoryImpl();

	@Before
	public void setUp() throws Exception {
		// Create and register mock activity implementation to be used during
		// test run
		BitcoindActivities activities = new BitcoindActivities() {
			@Override
			public String sendTransaction(BigDecimal amount, BigDecimal fee,
					String fromCn, String toCn, String toAddress, String id, String comment) {
				if (null!=amount && null!=fee && null!=fromCn &&(null!=toCn || null!=toAddress)){
					if (null!=toAddress && toAddress.equalsIgnoreCase(USER3.getPayDest().getAddress())){
						throw new RuntimeException("unknown reason");
					}else{
						return "txid2038942304";
					}
				}else{
					throw new RuntimeException("param missing");
				}
			}

			@Override
			public BigDecimal getAccountBalance(String cn) {
				Assert.assertNotNull("getAccountId called with null",cn);
				return USER1.getBalance();
			}

			@Override
			public String getNewAddress(String cn) {
				return null;
			}
			@Override
			public Long getAccount(String bcAddress) {
				return null;
			}
			@Override
			public List<Transaction> getAccountTransactions(String cn) {
				return null;
			}

			@Override
			public BigDecimal getTransactionVolume(String cn, int hours) {
				return USER1.getAmount();
			}
		};
		MessagingActivities mailActivities = new MessagingActivities() {

			@Override
			public void sendMessage(DataSet rsp) {
				trace.add(rsp);
			}

			@Override
			public Action sendConfirmation(DataSet rsp, String workflowId) {
				if (rsp.getCn().equalsIgnoreCase(USER2.getPayDest().getAddress())){
					return Action.TX_CANCELED;
				}else{
					Withdrawal w = (Withdrawal)rsp.getPayload();
					w.setConfKey("123");
					w.setConfLink("http://test.com/123");
					trace.add(rsp);
					return Action.WITHDRAWAL_REQ;
				}
			}
			@Override
			public DataSet readMessageAddress(DataSet data) {
				return data.setTo(new MessageAddress()
					.setAddress("")
					.setAddressType(MsgType.SMS)
					.setGateway(""));
			}

			@Override
			public Action phoneConfirmation(DataSet rsp, String workflowId) {
				if (rsp.getCn().equalsIgnoreCase(USER2.getPayDest().getAddress())){
					return Action.TX_CANCELED;
				}else{
					trace.add(rsp);
					return Action.WITHDRAWAL_REQ;
				}
			}

			@Override
			public void putCache(DataSet rsp) {
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
	
	/**
	 * Send a transaction that will only be verified by sms
	 * should be tx history + txamout < 100x(smsFee+callFee)
	 * so 0.2 + 0.0001 > 100x(0.0005+0.0015)
	 */
	@Test
	public void testSendSms() throws AddressException {
		WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER1.getPayDest().getAddress())
			.setTo(USER1.getMsgDest())
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.0011").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER2.getPayDest()));
		Promise<Void> booked = workflow.executeCommand(req);
		validateSendSms(booked);
	}
	
	@Asynchronous
	public void validateSendSms(Promise<Void> booked){
		Assert.assertTrue("verification not executed, no result", trace.size()==3 
				&& trace.get(0).getAction()==Action.WITHDRAWAL_REQ
				&& trace.get(1).getAction()==Action.WITHDRAWAL_CONF
				&& trace.get(2).getAction()==Action.DEPOSIT_CONF);
		Assert.assertEquals(((Withdrawal)trace.get(0).getPayload()).getConfKey(), "123");
		Withdrawal w = (Withdrawal)trace.get(1).getPayload();
		Assert.assertTrue("tx not executed", w.getTxId().equalsIgnoreCase("txid2038942304"));
	}

	/**
	 * Send a transaction that is substantial enough to trigger call verification
	 * should be tx history + txamout > 100x(smsFee+callFee)
	 * so 0.2 + 0.5 > 100x(0.0005+0.0015)
	 */
	@Test
	public void testSendCall() throws AddressException {
		WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER1.getPayDest().getAddress())
			.setTo(USER1.getMsgDest())
			.setPayload(new Withdrawal()
				.setComment("hallo")
				.setAmount(new BigDecimal("0.5").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER2.getPayDest()));
		Promise<Void> booked = workflow.executeCommand(req);
		validateSendCall(booked);
	}
	
	@Asynchronous
	public void validateSendCall(Promise<Void> booked){
		Assert.assertTrue("verification not executed, no result", trace.size()==3
				&& trace.get(0).getAction()==Action.WITHDRAWAL_REQ
				&& trace.get(1).getAction()==Action.WITHDRAWAL_CONF
				&& trace.get(2).getAction()==Action.DEPOSIT_CONF);
		Assert.assertEquals(
				((Withdrawal)trace.get(0).getPayload()).getConfKey(), WithdrawalWorkflow.VOICE_VER_TOKEN);
		Withdrawal w = (Withdrawal)trace.get(1).getPayload();
		Assert.assertTrue("comment not processed", w.getComment().equalsIgnoreCase("hallo"));
		Assert.assertTrue("tx not executed", w.getTxId().equalsIgnoreCase("txid2038942304"));
	}
	
	/**
	 * User failed sms verification
	 */
	@Test
	public void testSendSmsVerFail() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER2.getPayDest().getAddress())
			.setTo(USER2.getMsgDest())
			.setPayload(new Withdrawal()
				.setComment("hallo")
				.setAmount(new BigDecimal("0.0011").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER1.getPayDest()));
		new TryCatch() {
			@Override
			protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(req);
				AsyncAssert.assertEquals("expected Insuficient Funds exception not thrown", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
            		Assert.assertTrue("unexpected result", trace.size()==1 
            				&& trace.get(0).getAction()==Action.TX_CANCELED);
            		Withdrawal w = (Withdrawal)trace.get(0).getPayload();
            		Assert.assertTrue("verification has returned result",w.getConfKey()==null);
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	/**
	 * User failed voice call verification
	 */
	@Test
	public void testSendVoiceVerFail() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER2.getPayDest().getAddress())
			.setTo(USER2.getMsgDest())
			.setPayload(new Withdrawal()
				.setComment("hallo")
				.setAmount(new BigDecimal("1.0").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER1.getPayDest()));
		new TryCatch() {
			@Override
			protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(req);
				AsyncAssert.assertEquals("expected Insuficient Funds exception not thrown", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
            		Assert.assertTrue("unexpected result", trace.size()==1 
            				&& trace.get(0).getAction()==Action.TX_CANCELED);
            		Withdrawal w = (Withdrawal)trace.get(0).getPayload();
            		Assert.assertEquals(w.getConfKey(), WithdrawalWorkflow.VOICE_VER_TOKEN);
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	/**
	 * Error on the network
	 * 2 results will return, a successful sms verification, then the error 
	 */
	@Test
	public void testSendFail() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER1.getPayDest().getAddress())
			.setTo(USER1.getMsgDest())
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.0011").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER3.getPayDest()));
		new TryCatch() {
			@Override
			protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(req);
				AsyncAssert.assertEquals("expected Insuficient Funds exception not thrown", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
            		List<DataSet> t = trace;
            		Assert.assertTrue("verification not executed, no result", t.size()==2 
            				&& t.get(0).getAction()==Action.WITHDRAWAL_REQ
            				&& t.get(1).getAction()==Action.TX_FAILED);
            		Withdrawal w = (Withdrawal)t.get(0).getPayload();
            		Assert.assertEquals(w.getConfKey(), "123");
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	/**
	 * try to send a to big transaction
	 */
	@Test
	public void testInsufficientFunds() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER1.getPayDest().getAddress())
			.setTo(USER1.getMsgDest())
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("100.005").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER2.getPayDest()));		
		new TryCatch() {
			@Override
			protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(req);
				AsyncAssert.assertEquals("expected Insuficient Funds exception not thrown", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
            		Assert.assertTrue("unexpected result", trace.size()==1 
            				&& trace.get(0).getAction()==Action.INSUFISSIENT_FUNDS);
            		Withdrawal w = (Withdrawal)trace.get(0).getPayload();
            		//amount needed as displayed to user
            		Assert.assertEquals(new BigDecimal("100.0055").setScale(8),w.getAmount());
            		//current balance as displayed to user
            		Assert.assertEquals(new BigDecimal("2.5").setScale(8),w.getBalance());
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	/**
	 * filter all transaction that are < 2 fee
	 */
	@Test
	public void testBelowFee() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final DataSet req = new DataSet()
			.setAction(Action.WITHDRAWAL_REQ)
			.setCn(USER1.getPayDest().getAddress())
			.setTo(USER1.getMsgDest())
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.0005").setScale(8))
				.setFee(FEE)
				.setFeeAccount("1")
				.setPayDest(USER2.getPayDest()));		
		new TryCatch() {
			@Override
			protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(req);
				AsyncAssert.assertEquals("expected Insuficient Funds exception not thrown", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
            		Assert.assertTrue("unexpected result", trace.size()==1 
            				&& trace.get(0).getAction()==Action.BELOW_FEE);
            		Withdrawal w = (Withdrawal)trace.get(0).getPayload();
            		//amount send by user
            		Assert.assertEquals(FEE,w.getAmount());
            		//amount displaying the fee in the response
            		Assert.assertEquals(FEE,w.getBalance());
            	}else{
            		throw e;
            	}
			}
		};
	}

}