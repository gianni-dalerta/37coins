package com._37coins.bizLogic;

import java.util.Map;
import java.util.concurrent.CancellationException;


import com._37coins.activities.BitcoindActivitiesClient;
import com._37coins.activities.BitcoindActivitiesClientImpl;
import com._37coins.activities.CoreActivitiesClient;
import com._37coins.activities.CoreActivitiesClientImpl;
import com._37coins.activities.MailActivitiesClient;
import com._37coins.activities.MailActivitiesClientImpl;
import com._37coins.workflow.WithdrawalWorkflow;
import com.amazonaws.services.simpleworkflow.flow.DecisionContext;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.annotations.NoWait;
import com.amazonaws.services.simpleworkflow.flow.core.OrPromise;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Settable;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatch;

public class WithdrawalWorkflowImpl implements WithdrawalWorkflow {

    CoreActivitiesClient dbClient = new CoreActivitiesClientImpl();
    BitcoindActivitiesClient bcdClient = new BitcoindActivitiesClientImpl();
    MailActivitiesClient mailClient = new MailActivitiesClientImpl();
    private final int confirmationPeriod = 3500;
    DecisionContextProvider provider = new DecisionContextProviderImpl();
    DecisionContext context = provider.getDecisionContext();
    private WorkflowClock clock = context.getWorkflowClock();

    @Override
    public void executeCommand(final Map<String,Object> data) {
    	final Settable<Map<String,Object>> account = new Settable<>();
    	new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
				setAccount(account, dbClient.findAccountByMsgAddress(data));
			}
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	data.put("action", "error001");
            	mailClient.sendMail(data);
            	account.set(data);
			}
		};
		handleReceiver(account);
    }
    
    @Asynchronous
    public void handleReceiver(final Promise<Map<String,Object>> data){
    	if (((String)data.get().get("action")).contains("error")){
    		throw new CancellationException("account not found");
    	}
    	
    	final Settable<Map<String,Object>> account = new Settable<>();
    	if (data.get().get("receiver")!=null){
    		setAccount(account, bcdClient.getAccount(data));
    	}else{
    		new TryCatch() {
				@Override
	            protected void doTry() throws Throwable {
				   	if (data.get().get("receiverEmail")!=null){
				   		setAccount(account, dbClient.findReceiverAccount(data));
			    	}else if (data.get().get("receiverPhone")!=null){
			    		setAccount(account, dbClient.findReceiverAccount(data));
			    	}
				}
	            @Override
	            protected void doCatch(Throwable e) throws Throwable {
	            	data.get().put("action", "error003");
	    			mailClient.sendMail(data);
	            	account.set(data.get());
				}
			};
    	}
    	handleConfirm(account);
    }
    
    @Asynchronous
    public void handleConfirm(final Promise<Map<String,Object>> data){
    	if (data.get().get("receiver")==null && data.get().get("receiverAccount")==null){
    		throw new CancellationException("receiver not found");
    	}

		final Settable<Map<String,Object>> confirm = new Settable<>();
		if (((String)data.get().get("source")).equalsIgnoreCase("email")){
			data.get().put("action", "confirmSend");
			final Promise<Void> response = mailClient.sendConfirmation(data);
			final OrPromise confirmOrTimer = new OrPromise(startDaemonTimer(confirmationPeriod), response);
		   	new TryCatch() {
				@Override
	            protected void doTry() throws Throwable {
					setConfirm(confirm, confirmOrTimer, response, data);
				}
	            @Override
	            protected void doCatch(Throwable e) throws Throwable {
	            	data.get().put("action", "error003");
	    			mailClient.sendMail(data);
	            	cancel(e);
				}
			};
		}else{
			setAccount(confirm, data);
		}
		//read balanace
		Promise<Map<String,Object>> balance = bcdClient.getAccountBalance(confirm);
		handleBalance(balance);
    }
    
    @Asynchronous
    public void handleBalance(final Promise<Map<String,Object>> data){
    	double balance = (double)data.get().get("balance");
    	double amount = (double)data.get().get("amount");
    	double fee = (double)data.get().get("fee");
    	data.get().put("action", "send");
    	data.get().remove("fee");
    	if (balance < amount + fee){
    		data.get().put("action", "error005");
    		Promise<Void> fail = mailClient.sendMail(data);
    		fail(fail);
    	}else{
    		new TryCatch() {
    			@Override
                protected void doTry() throws Throwable {
		    		//define transaction
		    		Promise<Map<String,Object>> tx = bcdClient.sendTransaction(data);
		    		mailClient.sendMail(tx);
                }
                @Override
                protected void doCatch(Throwable e) throws Throwable {
	            	data.get().put("action", "error003");
	    			mailClient.sendMail(data);
	            	cancel(e);
                }
    		};
    	}
    }
    
	@Asynchronous
	public void setConfirm(@NoWait Settable<Map<String,Object>> account, OrPromise trigger, Promise<Void> response, Promise<Map<String,Object>> data) throws Throwable{
		if (response.isReady()){
			account.set(data.get());
		}else{
			throw new Throwable("user did not confirm transaction.");
		}
		
	}
	
	@Asynchronous
	public void fail(Promise<Void> data){
		throw new CancellationException("insufficient funds");
	}
    
	@Asynchronous
	public void setAccount(@NoWait Settable<Map<String,Object>> account, Promise<Map<String,Object>> data){
		account.set(data.get());
	}
	
	@Asynchronous(daemon = true)
    private Promise<Void> startDaemonTimer(int seconds) {
        Promise<Void> timer = clock.createTimer(seconds);
        return timer;
    }

}
