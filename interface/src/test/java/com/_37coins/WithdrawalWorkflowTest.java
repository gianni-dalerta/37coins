package com._37coins;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

import javax.mail.internet.AddressException;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com._37coins.activities.BitcoindActivities;
import com._37coins.activities.CoreActivities;
import com._37coins.activities.MailActivities;
import com._37coins.bizLogic.WithdrawalWorkflowImpl;
import com._37coins.workflow.WithdrawalWorkflowClient;
import com._37coins.workflow.WithdrawalWorkflowClientFactory;
import com._37coins.workflow.WithdrawalWorkflowClientFactoryImpl;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatch;
import com.amazonaws.services.simpleworkflow.flow.junit.AsyncAssert;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(FlowBlockJUnit4ClassRunner.class)
public class WithdrawalWorkflowTest {

	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	final Map<String, Object> trace = new HashMap<>();

	private WithdrawalWorkflowClientFactory workflowFactory = new WithdrawalWorkflowClientFactoryImpl();

	@Before
	public void setUp() throws Exception {
		// Create and register mock activity implementation to be used during
		// test run
		BitcoindActivities activities = new BitcoindActivities() {
			@Override
			public Map<String, Object> sendTransaction(Map<String, Object> rsp) {
				rsp.put("txid", "txid2038942304");
				return rsp;
			}

			@Override
			public Map<String, Object> getAccountBalance(Map<String, Object> rsp) {
				rsp.put("balance", 2.5);
				return rsp;
			}

			@Override
			public Map<String, Object> createBcAccount(Map<String, Object> rsp) {
				return null;
			}

			@Override
			public Map<String, Object> getNewAddress(Map<String, Object> rsp) {
				return null;
			}
		};
		MailActivities mailActivities = new MailActivities() {

			@Override
			public void sendMail(Map<String, Object> rsp) {
				try {
					System.out.println(new ObjectMapper()
							.writeValueAsString(rsp));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				trace.putAll(rsp);
			}

			@Override
			public void sendConfirmation(Map<String, Object> rsp) {
				try {
					System.out.println(new ObjectMapper()
							.writeValueAsString(rsp));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}

			@Override
			public String requestWithdrawalConfirm(Map<String, Object> cmd) {
				return null;
			}

			@Override
			public String requestWithdrawalReview(Map<String, Object> cmd) {
				return null;
			}

		};
		CoreActivities coreActivities = new CoreActivities() {
			@Override
			public Map<String, Object> findAccountByMsgAddress(
					Map<String, Object> data) {
				if (!((String) data.get("action")).equalsIgnoreCase("create")
						&& ((String) data.get("msgAddress"))
								.equalsIgnoreCase("01027423984")) {
					data.put("account", "1");
				} else {
					throw new RuntimeException("not found");
				}
				return data;
			}

			@Override
			public Map<String, Object> createDbAccount(Map<String, Object> data) {
				return null;
			}

			@Override
			public Map<String, Object> readAccount(Map<String, Object> data) {
				return null;
			}
		};
		workflowTest.addActivitiesImplementation(activities);
		workflowTest.addActivitiesImplementation(mailActivities);
		workflowTest.addActivitiesImplementation(coreActivities);
		workflowTest
				.addWorkflowImplementationType(WithdrawalWorkflowImpl.class);
	}

	@After
	public void tearDown() throws Exception {
		// trace = null;
	}

	@Test
	public void testCreateAccount() throws AddressException {
		WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", 0.5);
		cmd.put("fee", 0.0005);
		cmd.put("source", "sms");
		cmd.put("locale", "en");
		Promise<Void> booked = workflow.executeCommand(cmd);
		Map<String, Object> expected = new HashMap<>();
		expected.put("account", "1");
		expected.put("amount", 0.5);
		expected.put("balance", 2.5);
		expected.put("action", "send");
		expected.put("txid", "txid2038942304");
		expected.put("msgAddress", "01027423984");
		expected.put("source", "sms");
		expected.put("locale", "en");
		AsyncAssert.assertEqualsWaitFor("successfull create", expected, trace,
				booked);
	}

	@Test
	public void testCreateNoAccount() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "11027423985");
		cmd.put("source", "sms");
		cmd.put("locale", "en");
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
    				expected.put("source", "sms");
    				expected.put("locale", "en");
    				AsyncAssert.assertEqualsWaitFor("failed send", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}
	
	@Test
	public void testCreateNoMoney() throws AddressException {
		final WithdrawalWorkflowClient workflow = workflowFactory.getClient();
		final Map<String, Object> cmd = new HashMap<>();
		cmd.put("action", "send");
		cmd.put("msgAddress", "01027423984");
		cmd.put("amount", 5.5);
		cmd.put("fee", 0.0005);
		cmd.put("source", "sms");
		cmd.put("locale", "en");
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
    				expected.put("locale", "en");
    				expected.put("amount",5.5);
    				expected.put("balance",2.5);
    				expected.put("account","1");
    				AsyncAssert.assertEqualsWaitFor("failed send", expected, trace);
            	}else{
            		throw e;
            	}
			}
		};
	}

}