package com._37coins;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.AddressException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com._37coins.activities.BitcoindActivities;
import com._37coins.activities.CoreActivities;
import com._37coins.activities.MailActivities;
import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.bcJsonRpc.pojo.Transaction.Category;
import com._37coins.bizLogic.DepositWorkflowImpl;
import com._37coins.workflow.DepositWorkflowClient;
import com._37coins.workflow.DepositWorkflowClientFactory;
import com._37coins.workflow.DepositWorkflowClientFactoryImpl;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.junit.AsyncAssert;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;
import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(FlowBlockJUnit4ClassRunner.class)
public class DepositWorkflowTest {

	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	final Map<String, Object> trace = new HashMap<>();

	private DepositWorkflowClientFactory workflowFactory = new DepositWorkflowClientFactoryImpl();

	@Before
    public void setUp() throws Exception {
        // Create and register mock activity implementation to be used during test run
        BitcoindActivities activities = new BitcoindActivities() {
			@Override
			public Map<String, Object> sendTransaction(Map<String, Object> rsp) {
				return null;
			}
			@Override
			public Map<String, Object> getAccountBalance(Map<String, Object> rsp) {
				rsp.put("balance", new BigDecimal("2.5"));
				return rsp;
			}
			@Override
			public Map<String, Object> createBcAccount(Map<String, Object> rsp) {
				if (((String)rsp.get("account")).equalsIgnoreCase("1")){
					rsp.put("bcAddress", "1Nsateouhasontuh234");
				}else{
					throw new RuntimeException("not found");
				}
				return rsp;
			}
			@Override
			public Map<String, Object> getNewAddress(Map<String, Object> rsp) {
				if (((String)rsp.get("account")).equalsIgnoreCase("1")){
					rsp.put("bcAddress", "1Nsateouhasontuh234");
				}else{
					throw new RuntimeException("not found");
				}
				return rsp;
			}
			@Override
			public Map<String, Object> getAccount(Map<String, Object> rsp) {
				return null;
			}
        };
        MailActivities mailActivities = new MailActivities() {
        	
			@Override
			public void sendMail(Map<String, Object> rsp) {
				trace.putAll(rsp);
			}
			@Override
			public void sendConfirmation(Map<String, Object> rsp) {
			}
			@Override
			public String requestWithdrawalConfirm(Map<String, Object> cmd) {
				return null;
			}
			@Override
			public String requestWithdrawalReview(Map<String, Object> cmd) {
				return null;
			}
			@Override
			public void notifyMoveReceiver(Map<String, Object> rsp) {
			}

        };
        CoreActivities coreActivities = new CoreActivities(){
			@Override
			public Map<String, Object> findAccountByMsgAddress(
					Map<String, Object> data) {
				if (!((String)data.get("action")).equalsIgnoreCase("create") 
						&& ((String)data.get("msgAddress")).equalsIgnoreCase("test1@37coins.com")){
						data.put("account","1");
				}else{
					throw new RuntimeException("not found");
				}
				return data;
			}
			@Override
			public Map<String, Object> createDbAccount(Map<String, Object> data) {
				data.put("account","1");
				return data;
			}
			@Override
			public Map<String, Object> readAccount(Map<String, Object> data) {
				if (((String)data.get("account")).equalsIgnoreCase("1")){
					data.put("msgAddress","test1@37coins.com");
					data.put("source", "email");
					data.put("locale", new Locale("en"));
				}
				return data;
			}
			@Override
			public Map<String, Object> findReceiverAccount(Map<String, Object> data) {
				return null;
			}
        };
        workflowTest.addActivitiesImplementation(activities);
        workflowTest.addActivitiesImplementation(mailActivities);
        workflowTest.addActivitiesImplementation(coreActivities);
        workflowTest.addWorkflowImplementationType(DepositWorkflowImpl.class);
    }

	@After
	public void tearDown() throws Exception {
		// trace = null;
	}

	@Test
	public void testCreateAccount() throws AddressException {
		DepositWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> input = new HashMap<>();
		input.put("action","create");
		input.put("msgAddress","test@37coins.com");
		Promise<Void> booked = workflow.executeCommand(input);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("action", "create");
		expected.put("bcAddress", "1Nsateouhasontuh234");
		expected.put("msgAddress", "test@37coins.com");
		AsyncAssert.assertEqualsWaitFor("successfull create", expected, trace,
				booked);
	}

	@Test
	public void testDepositAccount() throws AddressException {
		DepositWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> input = new HashMap<>();
		input.put("action","deposit");
		input.put("msgAddress","test1@37coins.com");
		Promise<Void> booked = workflow.executeCommand(input);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("action", "deposit");
		expected.put("bcAddress", "1Nsateouhasontuh234");
		expected.put("msgAddress", "test1@37coins.com");
		AsyncAssert.assertEqualsWaitFor("successfull deposit", expected, trace,
				booked);
	}
	
	@Test
	public void testDepositNoAccount() throws AddressException {
		DepositWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> input = new HashMap<>();
		input.put("action","deposit");
		input.put("msgAddress","test2@37coins.com");
		Promise<Void> booked = workflow.executeCommand(input);
		Map<String, Object> expected = new HashMap<>();
		expected.put("action", "error001");
		expected.put("msgAddress", "test2@37coins.com");
		AsyncAssert.assertEqualsWaitFor("failed deposit", expected, trace,
				booked);
	}

	@Test
	public void testBalanceAccount() throws AddressException {
		DepositWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> input = new HashMap<>();
		input.put("action","balance");
		input.put("msgAddress","test1@37coins.com");
		Promise<Void> booked = workflow.executeCommand(input);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("action", "balance");
		expected.put("balance", new BigDecimal("2.5"));
		expected.put("msgAddress", "test1@37coins.com");
		AsyncAssert.assertEqualsWaitFor("successfull balance", expected, trace,
				booked);
	}
	
	@Test
	public void testBalanceNoAccount() throws AddressException {
		DepositWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> input = new HashMap<>();
		input.put("action","balance");
		input.put("msgAddress","test2@37coins.com");
		Promise<Void> booked = workflow
				.executeCommand(input);
		Map<String, Object> expected = new HashMap<>();
		expected.put("action", "error001");
		expected.put("msgAddress", "test2@37coins.com");
		AsyncAssert.assertEqualsWaitFor("failed balance", expected, trace,
				booked);
	}
	
	@Test
	public void testReceiveAccount() throws AddressException, JsonProcessingException {
		DepositWorkflowClient workflow = workflowFactory.getClient();
		Transaction tx1 = new Transaction().setAmount(new BigDecimal("0.5")).setCategory(Category.RECEIVE).setAccount("1");
		Transaction tx2 = new Transaction().setAmount(new BigDecimal("0.4")).setCategory(Category.SEND);
		Transaction tx3 = new Transaction().setAmount(new BigDecimal("0.1")).setCategory(Category.RECEIVE).setAccount("1");
		Transaction t = new Transaction().setDetails(Arrays.asList(tx1,tx2,tx3)).setTxid("txid22543456456");
		Promise<Void> booked = workflow.executeCommand(BitcoindClientFactory.txToMap(t));
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("action", "received");
		expected.put("balance", new BigDecimal("2.5"));
		expected.put("amount", new BigDecimal("0.6"));
		expected.put("confirmations", 0L);
		expected.put("txid", "txid22543456456");
		expected.put("msgAddress", "test1@37coins.com");
		expected.put("source", "email");
		expected.put("service", "37coins");
		expected.put("locale", new Locale("en"));
		AsyncAssert.assertEqualsWaitFor("successfull receive", expected, trace,
				booked);
	}
}