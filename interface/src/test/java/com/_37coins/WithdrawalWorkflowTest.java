package com._37coins;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com._37coins.bizLogic.WithdrawalWorkflowImpl;
import com._37coins.workflow.WithdrawalWorkflowClient;
import com._37coins.workflow.WithdrawalWorkflowClientFactory;
import com._37coins.workflow.WithdrawalWorkflowClientFactoryImpl;
import com._37coins.workflow.pojo.Response;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatch;
import com.amazonaws.services.simpleworkflow.flow.junit.AsyncAssert;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;

@RunWith(FlowBlockJUnit4ClassRunner.class)
public class WithdrawalWorkflowTest {

	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	final Set<Response> trace = new HashSet<>();

	private WithdrawalWorkflowClientFactory workflowFactory = new WithdrawalWorkflowClientFactoryImpl();

	@Before
	public void setUp() throws Exception {
		// Create and register mock activity implementation to be used during
		// test run
		BitcoindActivities activities = new BitcoindActivities() {
			@Override
			public String sendTransaction(BigDecimal amount, BigDecimal fee,
					Long fromId, Long toId, String toAddress) {
				return "txid2038942304";
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
			public void sendConfirmation(Response rsp) {
			}

		};
		
		workflowTest.addActivitiesImplementation(activities);
		workflowTest.addActivitiesImplementation(mailActivities);
		workflowTest
				.addWorkflowImplementationType(WithdrawalWorkflowImpl.class);
	}

	@After
	public void tearDown() throws Exception {
		// trace = null;
	}

	@Test
	public void testSend() throws AddressException {
		WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", new BigDecimal("0.5"));
		cmd.put("fee", new BigDecimal("0.0005"));
		cmd.put("source", "sms");
		cmd.put("receiver", "456");
		Promise<Void> booked = workflow.executeCommand(cmd);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("amount", new BigDecimal("0.5"));
		expected.put("balance", new BigDecimal("2.5"));
		expected.put("action", "send");
		expected.put("txid", "txid2038942304");
		expected.put("msgAddress", "01027423984");
		expected.put("receiver", "456");
		expected.put("source", "sms");
		AsyncAssert.assertEqualsWaitFor("successfull create", expected, trace,
				booked);
	}

	@Test
	public void testSendNoAccount() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "11027423985");
		cmd.put("receiver", "456");
		cmd.put("source", "sms");
		new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(cmd);
				AsyncAssert.assertEquals("should have aborded because msgAddress not found", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
    				Map<String, Object> expected = new HashMap<>();
    				expected.put("action", "error001");
    				expected.put("msgAddress", "11027423985");
    				expected.put("receiver", "456");
    				expected.put("source", "sms");
    				AsyncAssert.assertEqualsWaitFor("failed send", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	@Test
	public void testSendNoMoney() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", new BigDecimal("5.5"));
		cmd.put("fee", new BigDecimal("0.0005"));
		cmd.put("receiver", "456");
		cmd.put("source", "sms");
		new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(cmd);
				AsyncAssert.assertEquals("should have aborded because unsuficient funds", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
    				Map<String, Object> expected = new HashMap<>();
    				expected.put("action", "error005");
    				expected.put("msgAddress", "01027423984");
    				expected.put("source", "sms");
    				expected.put("amount",new BigDecimal("5.5"));
    				expected.put("balance",new BigDecimal("2.5"));
    				expected.put("receiver", "456");
    				expected.put("account","1");
    				AsyncAssert.assertEqualsWaitFor("failed send", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	@Test
	public void testSendEmail() {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", new BigDecimal("0.1"));
		cmd.put("fee", new BigDecimal("0.0005"));
		cmd.put("source", "sms");
		cmd.put("receiverEmail", "receiver@37coins.com");
		Promise<Void> booked = workflow.executeCommand(cmd);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("amount", new BigDecimal("0.1"));
		expected.put("balance", new BigDecimal("2.5"));
		expected.put("action", "send");
		expected.put("txid", "txid2038942304");
		expected.put("msgAddress", "01027423984");
		expected.put("source", "sms");
		expected.put("receiverAccount", "2");
		expected.put("receiverEmail", "receiver@37coins.com");
		AsyncAssert.assertEqualsWaitFor("successfull move account", expected, trace, booked);
	}
	
	@Test
	public void testSendNoEmail() {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", new BigDecimal("0.1"));
		cmd.put("fee", new BigDecimal("0.0005"));
		cmd.put("source", "sms");
		cmd.put("receiverEmail", "receiver2@37coins.com");
		new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(cmd);
				AsyncAssert.assertEquals("should have aborded because receiver not found", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
    				Map<String, Object> expected = new HashMap<>();
    				expected.put("action", "error003");
    				expected.put("msgAddress", "01027423984");
    				expected.put("source", "sms");
    				expected.put("amount",new BigDecimal("0.1"));
    				expected.put("fee", new BigDecimal("0.0005"));
    				expected.put("account","1");
    				expected.put("receiverEmail", "receiver2@37coins.com");
    				AsyncAssert.assertEqualsWaitFor("failed move", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	@Test
	public void testSendPhone() {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", new BigDecimal("0.1"));
		cmd.put("fee", new BigDecimal("0.0005"));
		cmd.put("source", "sms");
		cmd.put("receiverPhone", "987654321");
		Promise<Void> booked = workflow.executeCommand(cmd);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("amount", new BigDecimal("0.1"));
		expected.put("balance", new BigDecimal("2.5"));
		expected.put("action", "send");
		expected.put("txid", "txid2038942304");
		expected.put("msgAddress", "01027423984");
		expected.put("source", "sms");
		expected.put("receiverPhone", "987654321");
		expected.put("receiverAccount", "2");
		AsyncAssert.assertEqualsWaitFor("successfull move account2", expected, trace, booked);
	}
	
	@Test
	public void testSendNoPhone() {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", new BigDecimal("0.1"));
		cmd.put("fee", new BigDecimal("0.0005"));
		cmd.put("receiverPhone", "12345678");
		new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
				Promise<Void> booked = workflow.executeCommand(cmd);
				AsyncAssert.assertEquals("should have aborded because receiver not found", 0, booked);
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	if (e.getCause()!=null && e.getCause().getClass() == CancellationException.class){
    				Map<String, Object> expected = new HashMap<>();
    				expected.put("action", "error003");
    				expected.put("msgAddress", "01027423984");
    				expected.put("amount",new BigDecimal("0.1"));
    				expected.put("fee", new BigDecimal("0.0005"));
    				expected.put("account","1");
    				expected.put("receiverPhone", "12345678");
    				AsyncAssert.assertEqualsWaitFor("failed move account2", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}

}