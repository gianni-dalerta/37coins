package com._37coins.parse;

import java.math.RoundingMode;
import java.util.Locale;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Gateway;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.persistence.dto.Transaction;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;

public abstract class RequestInterpreter{
	
	final private MessageParser mp;
	private AmazonSimpleWorkflow swfService;

	public RequestInterpreter(MessageParser mp) {
		this.mp = mp;
	}
	
	public RequestInterpreter(MessageParser mp, AmazonSimpleWorkflow swfService) {
		this.mp = mp;
		this.swfService = swfService;
	}
	
	public void process(MessageAddress sender, String subject) {
		GenericRepository dao = new GenericRepository();
		DataSet data = null;
		try {
			data = mp.process(sender, subject);
			
			if (MessageParser.reqCmdList.contains(data.getAction())){
				//handle subject
				RNQuery q = new RNQuery().addFilter("address", data.getTo().getAddress());
				MsgAddress ma = dao.queryEntity(q, MsgAddress.class, false);
				if (null!=ma){
					data.setAccountId(ma.getOwner().getId());
					if (data.getAction()==null){
						respond(data.setAction(Action.UNKNOWN_COMMAND));
					}
				}else{
					if (null==data.getLocale()){
						data.setLocale(new Locale("en"));
					}
					RNQuery gwQ = new RNQuery().addFilter("address", data.getTo().getGateway());
					Gateway gw = dao.queryEntity(gwQ, Gateway.class);
					ma = new MsgAddress()
						.setAddress(data.getTo().getAddress())
						.setLocale(data.getLocale())
						.setType(data.getTo().getAddressType())
						.setOwner(new Account())
						.setGateway(gw);
					dao.add(ma);
					data.setAction(Action.SIGNUP);
					data.setAccountId(ma.getOwner().getId());
				}
				switch (data.getAction()){
				case BALANCE:
				case SIGNUP:
				case TRANSACTION:
				case DEPOSIT_REQ:
					startDeposit(data);
					break;
				case WITHDRAWAL_REQ:
					//handle object
					Withdrawal w = (Withdrawal)data.getPayload();
					if (null!= w.getMsgDest() && w.getMsgDest().getAddress()!=null){
						RNQuery q2 = new RNQuery().addFilter("address", w.getMsgDest().getAddress());
						MsgAddress ma2 = dao.queryEntity(q2, MsgAddress.class, false);
						if (ma2!=null){
							w.setPayDest(new PaymentAddress().setAddress(ma.getOwner().getId().toString()));
							w.getPayDest().setAddressType(PaymentType.ACCOUNT);
						}else{
							//use to send to not in db
							throw new RuntimeException("not implemented");
						}
					}
					RNQuery gwQ = new RNQuery().addFilter("address", data.getTo().getGateway());
					Gateway gw = dao.queryEntity(gwQ, Gateway.class);
					w.setFee(gw.getFee().setScale(8,RoundingMode.UP));
					w.setFeeAccount(gw.getOwner().getId().toString());
					Transaction t = new Transaction()
						.setKey(Transaction.generateKey());
					dao.add(t);
					startWithdrawal(data,t.getKey());
					break;
				case WITHDRAWAL_CONF:
					RNQuery ttQuery = new RNQuery().addFilter("key", (String)data.getPayload());
					Transaction tx = dao.queryEntity(ttQuery, Transaction.class);
			        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
			        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());
			        manualCompletionClient.complete(null);
			        //don't delete, to guarantee unique keys
			        break;
				case HELP:
					respond(data);
					break;
				}
			}else{
				respond(data);
			}
		} finally{
			dao.closePersistenceManager();
		}
	}
	
	public abstract void startWithdrawal(DataSet data,String workflowId);
	
	public abstract void startDeposit(DataSet data);
	
	public abstract void respond(DataSet rsp);

}
