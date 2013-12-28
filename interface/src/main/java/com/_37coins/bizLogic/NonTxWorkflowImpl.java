package com._37coins.bizLogic;

import java.math.BigDecimal;
import java.util.List;

import com._37coins.activities.BitcoindActivitiesClient;
import com._37coins.activities.BitcoindActivitiesClientImpl;
import com._37coins.activities.MessagingActivitiesClient;
import com._37coins.activities.MessagingActivitiesClientImpl;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.workflow.NonTxWorkflow;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;

public class NonTxWorkflowImpl implements NonTxWorkflow {

    BitcoindActivitiesClient bcdClient = new BitcoindActivitiesClientImpl();
    MessagingActivitiesClient msgClient = new MessagingActivitiesClientImpl();

	@Override
	public void executeCommand(final DataSet data) {
		if (data.getAction()==Action.DEPOSIT_REQ 
				||data.getAction()==Action.SIGNUP){
			Promise<String> bcAddress = bcdClient.getNewAddress(data.getCn());
			respondDepositReq(bcAddress, data);
		}else if (data.getAction()==Action.BALANCE){
			Promise<BigDecimal> balance = bcdClient.getAccountBalance(data.getCn());
			respondBalance(balance, data);
		}else if (data.getAction()==Action.GW_BALANCE){
			Promise<BigDecimal> balance = bcdClient.getAccountBalance(data.getCn());
			cacheBalance(balance, data);
		}else if (data.getAction()==Action.TRANSACTION){
			Promise<List<Transaction>> transactions = bcdClient.getAccountTransactions(data.getCn());
			respondTransactions(transactions, data);
		}else if (data.getAction() == Action.DEPOSIT_CONF){
			Promise<BigDecimal> balance = bcdClient.getAccountBalance(data.getCn());
			respondDepositConf(balance, data);
		}else{
			throw new RuntimeException("unknown action");
		}
    }
	
	@Asynchronous
	public void respondDepositReq(Promise<String> bcAddress,DataSet data){
		data.setPayload(new PaymentAddress()
			.setAddress(bcAddress.get())
			.setAddressType(PaymentType.BTC));
		msgClient.sendMessage(data);
	}
	
	@Asynchronous
	public void respondBalance(Promise<BigDecimal> balance,DataSet data){
		data.setPayload(new Withdrawal()
				.setBalance(balance.get()));
		msgClient.sendMessage(data);
	}
	
	@Asynchronous
	public void cacheBalance(Promise<BigDecimal> balance,DataSet data){
		data.setPayload(new Withdrawal()
				.setBalance(balance.get()));
		msgClient.putCache(data);
	}
	
	@Asynchronous
	public void respondTransactions(Promise<List<Transaction>> transactions,DataSet data){
		data.setPayload(transactions.get());
		msgClient.sendMessage(data);
	}
	
	@Asynchronous
	public void respondDepositConf(Promise<BigDecimal> balance,DataSet data){
		Withdrawal dep = (Withdrawal)data.getPayload();
		dep.setBalance(balance.get());
		Promise<DataSet> addr = msgClient.readMessageAddress(data);
		msgClient.sendMessage(addr);
	}

}
