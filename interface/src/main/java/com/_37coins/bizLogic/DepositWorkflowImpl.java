package com._37coins.bizLogic;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


import com._37coins.activities.BitcoindActivitiesClient;
import com._37coins.activities.BitcoindActivitiesClientImpl;
import com._37coins.activities.CoreActivitiesClient;
import com._37coins.activities.CoreActivitiesClientImpl;
import com._37coins.activities.MailActivitiesClient;
import com._37coins.activities.MailActivitiesClientImpl;
import com._37coins.workflow.DepositWorkflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.annotations.NoWait;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Settable;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatch;

public class DepositWorkflowImpl implements DepositWorkflow {

    CoreActivitiesClient dbClient = new CoreActivitiesClientImpl();
    BitcoindActivitiesClient bcdClient = new BitcoindActivitiesClientImpl();
    MailActivitiesClient mailClient = new MailActivitiesClientImpl();

	@Override
	public void executeCommand(final Map<String,Object> data) {
		final Settable<Map<String,Object>> account = new Settable<>();
		//we have received a message from a user, because msgAddress is present
		if (data.get("msgAddress")!=null){
	    	new TryCatch() {
				@Override
	            protected void doTry() throws Throwable {
					//we try to read the account from database
					setAccount(account, dbClient.findAccountByMsgAddress(data));
				}
	            @Override
	            protected void doCatch(Throwable e) throws Throwable {
	            	//if requested, we create an account
	            	if (((String)data.get("action")).equalsIgnoreCase("create")){
	            		setAccount(account,dbClient.createDbAccount(data));
	            	}else{//or throw an exception if the user didn't create one yet
	            		data.put("action", "error001");
	            		account.set(data);
	            	}
				}
			};
		}else{ //this is from bitcoind, because there is no msgAddress
			data.put("action","received");
			data.put("service","37coins");
			setAccount(account,dbClient.readAccount(data));
		}
		handleAccount(account);
    }
	
	@SuppressWarnings("unchecked")
	@Asynchronous
	public void handleAccount(Promise<Map<String,Object>> data){
		//handle error cases
		if (((String)data.get().get("action")).contains("error")){
			mailClient.sendMail(data);
			return;
		}
		//handle balance request
		if (((String)data.get().get("action")).equalsIgnoreCase("balance")){
			Promise<Map<String,Object>> mail = bcdClient.getAccountBalance(data);
			mailClient.sendMail(mail);
			return;
		}
		//handle deposit 
		if (((String)data.get().get("action")).equalsIgnoreCase("received")){
			List<Map<String,Object>> l = (List<Map<String,Object>>)data.get().get("receive");
			BigDecimal amount = BigDecimal.ZERO;
			for (Map<String,Object> m : l){
				amount = amount.add((BigDecimal)m.get("amount"));
			}
			data.get().put("amount", amount);
			data.get().remove("receive");
			data.get().remove("send");
			Promise<Map<String,Object>> mail = bcdClient.getAccountBalance(data);
			mailClient.sendMail(mail);
			return;
		}
		final Settable<Map<String,Object>> account = new Settable<>();
		//we already created a db account, create a bitcoind account on top
		if(data.get().get("action")!=null &&((String)data.get().get("action")).equalsIgnoreCase("create")
				&& (data.get().get("bcAddress")==null)){
			setAccount(account, bcdClient.createBcAccount(data));
		}else{
			account.set(data.get());
		}
		//handle deposit requset by simply returning the bc address
		Promise<Map<String,Object>> mail = bcdClient.getNewAddress(account);
		mailClient.sendMail(mail);
	}
	
    
	@Asynchronous
	public void setAccount(@NoWait Settable<Map<String,Object>> account, Promise<Map<String,Object>> data){
		account.set(data.get());
	}

}
