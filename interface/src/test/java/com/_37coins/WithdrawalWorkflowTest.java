package com._37coins;

import java.math.BigDecimal;
import java.util.List;

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
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.junit.AsyncAssert;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;

@RunWith(FlowBlockJUnit4ClassRunner.class)
public class WithdrawalWorkflowTest {

	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	final Response trace = new Response();

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
				trace.copy(rsp);
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
		Request req = new Request()
			.setAction(ReqAction.SEND)
			.setAccountId(0L)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.5").setScale(8))
				.setFee(new BigDecimal("0.0005").setScale(8))
				.setMsgDest(new MessageAddress()
					.setAddress("1")));
		Promise<Void> booked = workflow.executeCommand(req);
		Response expected = new Response()
			.setAction(RspAction.SEND)
			.setAccountId(0L)
			.setPayload(new Withdrawal()
				.setAmount(new BigDecimal("0.5").setScale(8))
				.setFee(new BigDecimal("0.0005").setScale(8))
			.setMsgDest(new MessageAddress()
			.setAddress("1")));
		AsyncAssert.assertEqualsWaitFor("successfull create", expected, trace, booked);
	}

}