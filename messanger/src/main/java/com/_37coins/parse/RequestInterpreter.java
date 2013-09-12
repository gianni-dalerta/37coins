package com._37coins.parse;

import java.util.Locale;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.workflow.pojo.IncompleteException;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Request.ReqAction;
import com._37coins.workflow.pojo.Response;
import com._37coins.workflow.pojo.Response.RspAction;

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
		} catch (IncompleteException e) {
			// we don't have enough data to respond to the user, so notify admin
			// TODO send message to admin
			e.printStackTrace();
		}
		if (rv instanceof Request){
			Request req = (Request)rv;
			RNQuery q = new RNQuery().addFilter("address", req.getFrom().getAddress());
			MsgAddress ma = dao.queryEntity(q, MsgAddress.class, false);
			if (null!=ma){
				req.setAccountId(ma.getOwner().getId());
				if (req.getAction()==null){
					respond(new Response().respondTo(req).setAction(RspAction.UNKNOWN_COMMAND));
				}
			}else{
				ma = new MsgAddress()
					.setAddress(req.getFrom().getAddress())
					.setLocale(req.getLocale())
					.setType(req.getFrom().getAddressType())
					.setOwner(new Account());
				dao.add(ma);
				req.setAction(ReqAction.CREATE);
				req.setAccountId(ma.getOwner().getId());
				if (null==req.getLocale()){
					req.setLocale(new Locale("en"));
				}
			}
			dao.closePersistenceManager();
			switch (req.getAction()){
			case BALANCE:
			case CREATE:
			case DEPOSIT:
				startDeposit(req);
				break;
			case SEND:
			case SEND_CONFIRM:
				startWithdrawal(req);
				break;
			case HELP:
				respond(new Response().respondTo(req));
				break;
			}
		}else{
			respond((Response)rv);
		}
	}
	
	public abstract void startWithdrawal(Request req);
	
	public abstract void startDeposit(Request req);
	
	public abstract void respond(Response rsp);

}
