package com._37coins.bizLogic;

import java.math.BigDecimal;
import java.util.concurrent.CancellationException;

import com._37coins.activities.BitcoindActivitiesClient;
import com._37coins.activities.BitcoindActivitiesClientImpl;
import com._37coins.activities.MessagingActivitiesClient;
import com._37coins.activities.MessagingActivitiesClientImpl;
import com._37coins.workflow.NonTxWorkflowClientFactory;
import com._37coins.workflow.NonTxWorkflowClientFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflow;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Withdrawal;
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
	DecisionContextProvider contextProvider = new DecisionContextProviderImpl();
    BitcoindActivitiesClient bcdClient = new BitcoindActivitiesClientImpl();
    MessagingActivitiesClient msgClient = new MessagingActivitiesClientImpl();
    NonTxWorkflowClientFactory factory = new NonTxWorkflowClientFactoryImpl();
    private final int confirmationPeriod = 3500;
    DecisionContextProvider provider = new DecisionContextProviderImpl();
    DecisionContext context = provider.getDecisionContext();
    private WorkflowClock clock = context.getWorkflowClock();

    
    @Override
    public void executeCommand(final DataSet data) {
    	Promise<BigDecimal> balance = bcdClient.getAccountBalance(data.getAccountId());
    	handleAccount(balance, data);
    }
    
    @Asynchronous
    public void handleAccount(Promise<BigDecimal> balance, final DataSet data){
		final Settable<DataSet> confirm = new Settable<>();
    	Withdrawal w = (Withdrawal)data.getPayload();
    	BigDecimal amount = w.getAmount().setScale(8);
    	BigDecimal fee = w.getFee().setScale(8);
    	
    	if (balance.get().compareTo(amount.add(fee).setScale(8))<0){
    		data.setPayload(new Withdrawal()
    				.setAmount(amount.add(fee).setScale(8))
    				.setBalance(balance.get()))
    			.setAction(Action.INSUFISSIENT_FUNDS);
    		Promise<Void> fail = msgClient.sendMessage(data);
    		fail(fail, "insufficient funds");
    		return;
    	}else{
			final Promise<Boolean> response = msgClient.phoneConfirmation(data,contextProvider.getDecisionContext().getWorkflowContext().getWorkflowExecution().getWorkflowId());
			final OrPromise confirmOrTimer = new OrPromise(startDaemonTimer(confirmationPeriod), response);
		   	new TryCatch() {
				@Override
	            protected void doTry() throws Throwable {
					setConfirm(confirm, confirmOrTimer, response, data);
				}
	            @Override
	            protected void doCatch(Throwable e) throws Throwable {
	            	data.setAction(Action.TIMEOUT);
	    			msgClient.sendMessage(data);
	            	cancel(e);
				}
			};
    	}
		handleTransaction(confirm);
    }
    
    @Asynchronous
    public void handleTransaction(final Promise<DataSet> rsp){
    	if (rsp.get().getAction()==Action.TX_FAILED){
    		Promise<Void> fail = msgClient.sendMessage(rsp.get());
    		fail(fail, "transaction failed");
    		return;
    	}
		new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
	    		//define transaction
				Withdrawal w = (Withdrawal)rsp.get().getPayload();
				String toId = null;
				String toAddress = null;
				if (w.getPayDest().getAddressType()==PaymentType.ACCOUNT){
					toId = w.getPayDest().getAddress();
					toAddress = (w.getMsgDest()!=null)?rsp.get().getTo().getAddress()+"::"+w.getMsgDest().getAddress():null;
				}
				if (w.getPayDest().getAddressType()==PaymentType.BTC){
					toAddress = w.getPayDest().getAddress();
				}
	    		Promise<String> tx = bcdClient.sendTransaction(
	    				w.getAmount(), 
	    				w.getFee(), 
	    				rsp.get().getAccountId(), 
	    				toId, 
	    				toAddress,
	    				contextProvider.getDecisionContext().getWorkflowContext().getWorkflowExecution().getWorkflowId(),
	    				w.getComment());
	    		afterSend(tx, rsp.get());
            }
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	rsp.get().setAction(Action.TX_FAILED);
    			msgClient.sendMessage(rsp);
    			e.printStackTrace();
            	cancel(e);
            }
		};
    }
    
    @Asynchronous
    public void afterSend(Promise<String> txId, DataSet data) throws Throwable{
    	Withdrawal w = (Withdrawal)data.getPayload();
    	bcdClient.sendTransaction(
    			w.getFee(), 
    			BigDecimal.ZERO, 
    			data.getAccountId(), 
    			w.getFeeAccount(), 
    			null,
    			contextProvider.getDecisionContext().getWorkflowContext().getWorkflowExecution().getWorkflowId(),
    			"");
    	w.setTxId(txId.get());
		data.setAction(Action.WITHDRAWAL_CONF);
    	msgClient.sendMessage(data);
    	if (w.getPayDest().getAddressType()==PaymentType.ACCOUNT){
    		//start child workflow to tell receiver about his luck
    		DataSet rsp2 = new DataSet()
    			.setAction(Action.DEPOSIT_CONF)
    			.setAccountId(Long.parseLong(w.getPayDest().getAddress()))
    			.setPayload(new Withdrawal()
    				.setAmount(w.getAmount())
    				.setTxId(context.getWorkflowContext().getWorkflowExecution().getRunId()));
    		Promise<Void> rv = factory.getClient().executeCommand(rsp2);
    		awaitWorkflow(rv, rsp2);
    	}
    }
    
    @Asynchronous
	public void awaitWorkflow(Promise<Void> isConfirmed, DataSet data){
		//do nothing
	}
    
	@Asynchronous
	public void setConfirm(@NoWait Settable<DataSet> account, OrPromise trigger, Promise<Boolean> isConfirmed, DataSet data) throws Throwable{
		System.out.println("call response: "+isConfirmed.get());
		if (isConfirmed.isReady()){
			if (null==isConfirmed.get() || !isConfirmed.get()){
				data.setAction(Action.TX_FAILED);
			}
			account.set(data);
		}else{
			throw new Throwable("user did not confirm transaction.");
		}
		
	}
	
	@Asynchronous
	public void fail(Promise<Void> data, String message){
		throw new CancellationException(message);
	}

	
	@Asynchronous(daemon = true)
    private Promise<Void> startDaemonTimer(int seconds) {
        Promise<Void> timer = clock.createTimer(seconds);
        return timer;
    }

}
