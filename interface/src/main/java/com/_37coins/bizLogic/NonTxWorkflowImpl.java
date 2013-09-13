package com._37coins.bizLogic;

import java.math.BigDecimal;
import java.util.List;

import com._37coins.activities.BitcoindActivitiesClient;
import com._37coins.activities.BitcoindActivitiesClientImpl;
import com._37coins.activities.MessagingActivitiesClient;
import com._37coins.activities.MessagingActivitiesClientImpl;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.workflow.NonTxWorkflow;
import com._37coins.workflow.pojo.Deposit;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;

public class NonTxWorkflowImpl implements NonTxWorkflow {

    BitcoindActivitiesClient bcdClient = new BitcoindActivitiesClientImpl();
    MessagingActivitiesClient msgClient = new MessagingActivitiesClientImpl();

	@Override
	public void executeCommand(final Object msg) {
		
		if (msg instanceof Request){
			Request req = (Request)msg;
			if (req.getAction()==ReqAction.DEPOSIT 
					||req.getAction()==ReqAction.CREATE){
				Promise<String> bcAddress = bcdClient.getNewAddress(req.getAccountId());
				respondDeposit(bcAddress, req);
			}else if (req.getAction()==ReqAction.BALANCE){
				Promise<BigDecimal> balance = bcdClient.getAccountBalance(req.getAccountId());
				respondBalance(balance, req);
			}else if (req.getAction()==ReqAction.TRANSACTION){
				Promise<List<Transaction>> transactions = bcdClient.getAccountTransactions(req.getAccountId());
				respondTransactions(transactions, req);
			}else{
				throw new RuntimeException("unknown action");
			}
		}else {
			Response rsp = (Response)msg;
			if (rsp.getAction() == RspAction.RECEIVED){
				Promise<BigDecimal> balance = bcdClient.getAccountBalance(rsp.getAccountId());
				respondReceived(balance, rsp);
			}else{
				throw new RuntimeException("unknown action");
			}
		}
		

    }
	
	@Asynchronous
	public void respondDeposit(Promise<String> bcAddress,Request req){
		Response rsp = new Response()
			.respondTo(req)
			.setPayload(new PaymentAddress()
				.setAddress(bcAddress.get())
				.setAddressType(PaymentType.BTC));
		msgClient.sendMessage(rsp);
	}
	
	@Asynchronous
	public void respondBalance(Promise<BigDecimal> balance,Request req){
		Response rsp = new Response()
			.respondTo(req)
			.setPayload(new Deposit()
				.setAmount(balance.get())
				.setCurrency(null));
		msgClient.sendMessage(rsp);
	}
	
	@Asynchronous
	public void respondTransactions(Promise<List<Transaction>> transactions,Request req){
		Response rsp = new Response()
			.respondTo(req)
			.setPayload(transactions);
		msgClient.sendMessage(rsp);
	}
	
	@Asynchronous
	public void respondReceived(Promise<BigDecimal> balance,Response rsp){
		Deposit dep = (Deposit)rsp.getPayload();
		dep.setBalance(balance.get());
		Promise<Response> addr = msgClient.readMessageAddress(rsp);
		msgClient.sendMessage(addr);
	}

}
