package com._37coins.bizLogic;

import java.math.BigDecimal;
import java.util.concurrent.CancellationException;

import com._37coins.activities.BitcoindActivitiesClient;
import com._37coins.activities.BitcoindActivitiesClientImpl;
import com._37coins.activities.MessagingActivitiesClient;
import com._37coins.activities.MessagingActivitiesClientImpl;
import com._37coins.workflow.WithdrawalWorkflow;
import com._37coins.workflow.pojo.Deposit;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
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

    BitcoindActivitiesClient bcdClient = new BitcoindActivitiesClientImpl();
    MessagingActivitiesClient msgClient = new MessagingActivitiesClientImpl();
    private final int confirmationPeriod = 3500;
    DecisionContextProvider provider = new DecisionContextProviderImpl();
    DecisionContext context = provider.getDecisionContext();
    private WorkflowClock clock = context.getWorkflowClock();

    
    @Override
    public void executeCommand(final Request req) {
    	Promise<BigDecimal> balance = bcdClient.getAccountBalance(req.getAccountId());
    	handleAccount(balance, req);
    }
    
    @Asynchronous
    public void handleAccount(Promise<BigDecimal> balance, Request req){
		final Settable<Response> confirm = new Settable<>();
    	Withdrawal w = (Withdrawal)req.getPayload();
    	BigDecimal amount = w.getAmount();
    	BigDecimal fee = w.getFee();
    	if (balance.get().compareTo(amount.add(fee))<0){
    		Response rsp = new Response()
    			.respondTo(req)
    			.setAction(RspAction.INSUFISSIENT_FUNDS);
    		Promise<Void> fail = msgClient.sendMessage(rsp);
    		fail(fail);
    		return;
    	}else{
			if (req.getFrom().getAddressType()==MsgType.EMAIL){
				final Response rsp = new Response()
					.respondTo(req)
					.setAction(RspAction.SEND_CONFIRM);
				final Promise<Void> response = msgClient.sendConfirmation(rsp);
				final OrPromise confirmOrTimer = new OrPromise(startDaemonTimer(confirmationPeriod), response);
			   	new TryCatch() {
					@Override
		            protected void doTry() throws Throwable {
						setConfirm(confirm, confirmOrTimer, response, rsp);
					}
		            @Override
		            protected void doCatch(Throwable e) throws Throwable {
		            	rsp.setAction(RspAction.TIMEOUT);
		    			msgClient.sendMessage(rsp);
		            	cancel(e);
					}
				};
			}else{
				final Response rsp = new Response()
					.respondTo(req);
				confirm.set(rsp);
			}
    	}
		handleTransaction(confirm);
    }
    
    @Asynchronous
    public void handleTransaction(final Promise<Response> rsp){
		new TryCatch() {
			@Override
            protected void doTry() throws Throwable {
	    		//define transaction
				Withdrawal w = (Withdrawal)rsp.get().getPayload();
	    		Promise<String> tx = bcdClient.sendTransaction(
	    				w.getAmount(), 
	    				w.getFee(), 
	    				rsp.get().getAccountId(), 
	    				(w.getPayDest().getAddressType()==PaymentType.ACCOUNT)?Long.parseLong(w.getPayDest().getAddress()):null, 
	    						(w.getPayDest().getAddressType()==PaymentType.BTC)?w.getPayDest().getAddress():null);
	    		afterSend(tx, rsp.get());
            }
            @Override
            protected void doCatch(Throwable e) throws Throwable {
            	rsp.get().setAction(RspAction.TX_FAILED);
    			msgClient.sendMessage(rsp);
            	cancel(e);
            }
		};
    }
    
    @Asynchronous
    public void afterSend(Promise<String> data, Response rsp){
    	Withdrawal w = (Withdrawal)rsp.getPayload();
    	w.setTxId(data.get());
    	msgClient.sendMessage(rsp);
    	if (data.get()==null){
    		Response rsp2 = new Response()
    			.setTo(w.getMsgDest())
    			.setPayload(new Deposit()
    				.setAmount(w.getAmount())
    				.setCurrency(w.getCurrency()));
    		msgClient.sendMessage(rsp2);
    	}
    }
    
	@Asynchronous
	public void setConfirm(@NoWait Settable<Response> account, OrPromise trigger, Promise<Void> response, Response data) throws Throwable{
		if (response.isReady()){
			account.set(data);
		}else{
			throw new Throwable("user did not confirm transaction.");
		}
		
	}
	
	@Asynchronous
	public void fail(Promise<Void> data){
		throw new CancellationException("insufficient funds");
	}

	
	@Asynchronous(daemon = true)
    private Promise<Void> startDaemonTimer(int seconds) {
        Promise<Void> timer = clock.createTimer(seconds);
        return timer;
    }

}
