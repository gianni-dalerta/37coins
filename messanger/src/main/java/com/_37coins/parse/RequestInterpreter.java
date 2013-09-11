package com._37coins.parse;

import com._37coins.workflow.pojo.IncompleteException;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Response;

public abstract class RequestInterpreter{
	
	final private MessageParser mp;

	public RequestInterpreter(MessageParser mp) {
		this.mp = mp;
	}
	
	public void process(MessageAddress sender, String subject) {
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
			if (req.getAction()==null){
				//we did not understand the command
				//but if the user comes writes for the first time,
				//then we just want to greet him and create an account
				//check if adr exists in db
				//if yes, respond with error
				//if no, change action to create and process
			}
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
			default:
				respond(new Response().respondTo(req));
			}
		}else{
			respond((Response)rv);
		}
	}
	
	public abstract void startWithdrawal(Request req);
	
	public abstract void startDeposit(Request req);
	
	public abstract void respond(Response rsp);

}
