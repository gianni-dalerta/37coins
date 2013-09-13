package com._37coins.parse;

import java.math.RoundingMode;
import java.util.Locale;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Gateway;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.workflow.pojo.IncompleteException;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;
import com._37coins.workflow.pojo.Withdrawal;

public abstract class RequestInterpreter{
	
	final private MessageParser mp;

	public RequestInterpreter(MessageParser mp) {
		this.mp = mp;
	}
	
	public void process(MessageAddress sender, String subject) {
		GenericRepository dao = new GenericRepository();
		Object rv = null;
		try {
			rv = mp.process(sender, subject);
			
			if (rv instanceof Request){
				Request req = (Request)rv;
				//handle subject
				RNQuery q = new RNQuery().addFilter("address", req.getFrom().getAddress());
				MsgAddress ma = dao.queryEntity(q, MsgAddress.class, false);
				if (null!=ma){
					req.setAccountId(ma.getOwner().getId());
					if (req.getAction()==null){
						respond(new Response().respondTo(req).setAction(RspAction.UNKNOWN_COMMAND));
					}
				}else{
					if (null==req.getLocale()){
						req.setLocale(new Locale("en"));
					}
					RNQuery gwQ = new RNQuery().addFilter("address", req.getFrom().getGateway());
					Gateway gw = dao.queryEntity(gwQ, Gateway.class);
					ma = new MsgAddress()
						.setAddress(req.getFrom().getAddress())
						.setLocale(req.getLocale())
						.setType(req.getFrom().getAddressType())
						.setOwner(new Account())
						.setGateway(gw);
					dao.add(ma);
					req.setAction(ReqAction.CREATE);
					req.setAccountId(ma.getOwner().getId());
				}
				switch (req.getAction()){
				case BALANCE:
				case CREATE:
				case DEPOSIT:
					startDeposit(req);
					break;
				case SEND:
					//handle object
					Withdrawal w = (Withdrawal)req.getPayload();
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
					RNQuery gwQ = new RNQuery().addFilter("address", req.getFrom().getGateway());
					Gateway gw = dao.queryEntity(gwQ, Gateway.class);
					w.setFee(gw.getFee().setScale(8,RoundingMode.UP));
					w.setFeeAccount(gw.getOwner().getId().toString());
					startWithdrawal(req);
					break;
				case SEND_CONFIRM:
					throw new RuntimeException("not implemented");
				case HELP:
					respond(new Response().respondTo(req));
					break;
				}
			}else{
				respond((Response)rv);
			}
		} catch (IncompleteException e) {
			// we don't have enough data to respond to the user, so notify admin
			// TODO send message to admin
			e.printStackTrace();
		} finally{
			dao.closePersistenceManager();
		}
	}
	
	public abstract void startWithdrawal(Request req);
	
	public abstract void startDeposit(Request req);
	
	public abstract void respond(Response rsp);

}
