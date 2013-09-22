package com._37coins.parse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Gateway;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.persistence.dto.Transaction;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
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
	final private GenericRepository dao;
	
	@Inject
	public RequestInterpreter(MessageParser mp, GenericRepository dao, AmazonSimpleWorkflow swfService) {
		this.mp = mp;
		this.swfService = swfService;
		this.dao = dao;
	}
	
	public void process(MessageAddress sender, String subject) {
		DataSet data = null;
		try {
			data = mp.process(sender, subject);
			
			if (data.getAction()==null||MessageParser.reqCmdList.contains(data.getAction())){
				Transaction t = new Transaction().setKey(Transaction.generateKey());
				Withdrawal w = (data.getPayload() instanceof Withdrawal)?(Withdrawal)data.getPayload():null;
				if (data.getAction()==Action.WITHDRAWAL_REQ_OTHER){
					MessageAddress temp = w.getMsgDest();
					w.setMsgDest(data.getTo());
					data.setTo(temp);
				}
				RNQuery q = new RNQuery().addFilter("address", data.getTo().getAddress());
				MsgAddress ma = dao.queryEntity(q, MsgAddress.class, false);
				if (null!=ma){
					data.setAccountId(ma.getOwner().getId());
					if (data.getTo().getGateway()==null){
						data.getTo().setGateway(ma.getGateway().getAddress());
					}
					if (data.getAction()==null){
						data.setLocale(ma.getLocale());
						respond(data.setAction(Action.UNKNOWN_COMMAND));
						return;
					}else{
						ma.setLocale(data.getLocale());
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
					data.setAccountId(ma.getOwner().getId());
					//call create
					DataSet create = new DataSet()
						.setAction(Action.SIGNUP)
						.setTo(data.getTo())
						.setAccountId(data.getAccountId())
						.setLocale(data.getLocale())
						.setService(data.getService());
					startDeposit(create);
					//nothing to do if this was the first message and had no meaning 
					if (data.getAction()==null){
						return;
					}
				}
				switch (data.getAction()){
				case BALANCE:
				case SIGNUP:
				case TRANSACTION:
				case DEPOSIT_REQ:
					startDeposit(data);
					break;
				case WITHDRAWAL_REQ_OTHER:
				case WITHDRAWAL_REQ:
					if (null!= w.getMsgDest() && w.getMsgDest().getAddress()!=null){
						RNQuery q2 = new RNQuery().addFilter("address", w.getMsgDest().getAddress());
						MsgAddress ma2 = dao.queryEntity(q2, MsgAddress.class, false);
						if (ma2==null){
							if (w.getMsgDest().getAddressType()==MsgType.SMS){
								Gateway gw2 = null;
								//set gateway from referring user's gateway
								if (data.getTo().getAddressType() == MsgType.SMS 
										&& w.getMsgDest().getPhoneNumber().getCountryCode() == data.getTo().getPhoneNumber().getCountryCode()){
									gw2 = ma.getGateway();
								}else{//or try to find a gateway in the database
									RNQuery gwq2 = new RNQuery().addFilter("countryCode", w.getMsgDest().getPhoneNumber().getCountryCode());
									List<Gateway> list = dao.queryList(gwq2, Gateway.class);
									if (null!=list&&list.size()>0){
										gw2=list.get(0);
									}else{
										throw new RuntimeException("no gateway available for this user");
									}
								}
								if (null!=gw2){
									ma2 = new MsgAddress()
									.setGateway(gw2)
									.setLocale(ma.getLocale())
									.setType(w.getMsgDest().getAddressType())
									.setOwner(new Account())
									.setAddress(w.getMsgDest().getAddress());
								}
							}else if (w.getMsgDest().getAddressType()==MsgType.EMAIL){
								//how to set the email gateway?
								throw new RuntimeException("not implemented");
							}else{
								throw new RuntimeException("not implemented");
							}
							if (ma2!=null){
								//save
								dao.add(ma2);
								//and say hi to new user
								DataSet create = new DataSet()
									.setAction(Action.SIGNUP)
									.setTo(new MessageAddress()
										.setAddress(w.getMsgDest().getAddressObject())
										.setAddressType(ma2.getType())
										.setGateway(ma2.getGateway().getAddress()))
									.setAccountId(ma2.getOwner().getId())
									.setLocale(ma2.getLocale())
									.setService(data.getService());
								startDeposit(create);
							}
						}
						if (ma2!=null){
							//set our payment destination
							if (null == w.getPayDest()){
								w.setPayDest(new PaymentAddress());
							}
							w.getPayDest()
								.setAddress(ma2.getOwner().getId().toString())
								.setAddressType(PaymentType.ACCOUNT);
							w.getMsgDest()
								.setGateway(ma2.getGateway().getAddress());
						}
					}
					//set the fee
					RNQuery gwQ = new RNQuery().addFilter("address", data.getTo().getGateway());
					Gateway gw = dao.queryEntity(gwQ, Gateway.class);
					w.setFee(gw.getFee().setScale(8,RoundingMode.UP));
					w.setFeeAccount(gw.getOwner().getId().toString());
					//check that transaction amount is > fee 
					//(otherwise tx history gets screwed up)
					if (w.getAmount().compareTo(w.getFee())<=0){
						data.setAction(Action.BELOW_FEE);
						w.setAmount(w.getFee().add(new BigDecimal("0.00001").setScale(8)));
						respond(data);
						break;
					}
					//save the transaction id to db
					dao.add(t);
					//run
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
			if (dao!=null){
				dao.closePersistenceManager();
			}
		}
	}
	
	public abstract void startWithdrawal(DataSet data,String workflowId);
	
	public abstract void startDeposit(DataSet data);
	
	public abstract void respond(DataSet rsp);

}
