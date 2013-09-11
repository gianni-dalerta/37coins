package com._37coins;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.AddressException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com._37coins.activities.BitcoindActivities;
import com._37coins.activities.MessagingActivities;
import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.bcJsonRpc.pojo.Transaction.Category;
import com._37coins.bizLogic.NonTxWorkflowImpl;
import com._37coins.workflow.NonTxWorkflowClient;
import com._37coins.workflow.NonTxWorkflowClientFactory;
import com._37coins.workflow.NonTxWorkflowClientFactoryImpl;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.junit.AsyncAssert;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;
import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(FlowBlockJUnit4ClassRunner.class)
public class NonTxWorkflowTest {

	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	final Response trace = new Response();

	private NonTxWorkflowClientFactory workflowFactory = new NonTxWorkflowClientFactoryImpl();

	@Before
    public void setUp() throws Exception {
        // Create and register mock activity implementation to be used during test run
        BitcoindActivities activities = new BitcoindActivities() {
			@Override
			public String sendTransaction(BigDecimal amount, BigDecimal fee, Long fromId,
					Long toId, String toAddress) {
				return null;
			}
			@Override
			public BigDecimal getAccountBalance(Long accountId) {
				return new BigDecimal("2.5");
			}
			@Override
			public String getNewAddress(Long accountId) {
				if (accountId == 1L){
					return "1Nsateouhasontuh234";
				}else{
					throw new RuntimeException("not found");
				}
			}
			@Override
			public Long getAccount(String bcAddress) {
				return null;
			}
			@Override
			public List<Transaction> getAccountTransactions(Long accountId) {
				return null;
			}
        };
        MessagingActivities mailActivities = new MessagingActivities() {

			@Override
			public void sendMessage(Response rsp) {
				trace.copy(rsp);
			}
			@Override
			public void sendConfirmation(Response rsp) {
			}

        };
        workflowTest.addActivitiesImplementation(activities);
        workflowTest.addActivitiesImplementation(mailActivities);
        workflowTest.addWorkflowImplementationType(NonTxWorkflowImpl.class);
    }

	@After
	public void tearDown() throws Exception {
		// trace = null;
	}

	@Test
	public void testCreateAccount() throws AddressException {
		NonTxWorkflowClient workflow = workflowFactory.getClient();
		Request req = new Request()
			.setAction(ReqAction.CREATE)
			.setAccountId(1L)
			.setFrom(new MessageAddress()
				.setAddress("test@37coins.com"));
		Promise<Void> booked = workflow.executeCommand(req);
		Response rsp = new Response()
			.respondTo(req)
			.setPayload(new PaymentAddress()
				.setAddress("1Nsateouhasontuh234")
				.setAddressType(PaymentType.BTC));
		AsyncAssert.assertEqualsWaitFor("successfull create", rsp, trace,booked);
	}

	@Test
	public void testDepositAccount() throws AddressException {
		NonTxWorkflowClient workflow = workflowFactory.getClient();
		Request req = new Request()
			.setAction(ReqAction.DEPOSIT)
			.setAccountId(1L)
			.setFrom(new MessageAddress()
				.setAddress("test@37coins.com"));
		Promise<Void> booked = workflow.executeCommand(req);
		Response rsp = new Response()
			.respondTo(req)
			.setPayload(new PaymentAddress()
				.setAddress("1Nsateouhasontuh234")
				.setAddressType(PaymentType.BTC));
		AsyncAssert.assertEqualsWaitFor("successfull deposit", rsp, trace,booked);
	}

	@Test
	public void testBalanceAccount() throws AddressException {
		NonTxWorkflowClient workflow = workflowFactory.getClient();
		Request req = new Request()
			.setAction(ReqAction.BALANCE)
			.setAccountId(1L)
			.setFrom(new MessageAddress()
				.setAddress("test@37coins.com"));
		Promise<Void> booked = workflow.executeCommand(req);
		Response rsp = new Response()
			.respondTo(req)
			.setPayload(new PaymentAddress()
				.setAddress("1Nsateouhasontuh234")
				.setAddressType(PaymentType.BTC));
		AsyncAssert.assertEqualsWaitFor("successfull balance", rsp, trace,
				booked);
	}
	
	@Test
	public void testBalanceNoAccount() throws AddressException {
		NonTxWorkflowClient workflow = workflowFactory.getClient();
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
		NonTxWorkflowClient workflow = workflowFactory.getClient();
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