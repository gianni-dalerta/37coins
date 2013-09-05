package com._37coins;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class MessageProcessor {
	public static Logger log = LoggerFactory.getLogger(MessageProcessor.class);
	
	public enum Action {
	    CREATE("create"), //create a new account
	    DEPOSIT("deposit"), //request a bitcoin address to receive a payment
	    SEND("send"),	//send a payment
	    SEND_CONFIRM("confirm"),
	    BALANCE("balance"), //request the balance 
	    HELP("help"); 
	    
	    private String text;

	    Action(String text) {
	      this.text = text;
	    }
	    
	    @JsonValue
	    final String value() {
	        return this.text;
	    }

	    public String getText() {
	      return this.text;
	    }
	    
	    @JsonCreator
	    public static Action fromString(String text) {
	      if (text != null) {
	        for (Action b : Action.values()) {
	          if (text.equalsIgnoreCase(b.text)) {
	            return b;
	          }
	        }
	      }
	      return null;
	    }
	}

		
		
	public boolean verify(String address){
		//TODO: make sure it's a decent address
		return true;
	}
	
	public Action readCommand(String subject){
		if (null==subject || subject.length()<2){
			return null;
		}
		String[] ca = subject.toLowerCase().trim().split(" ");
		//TODO: take different languages into account
		
		return Action.fromString(ca[0]);
	}
	
	public Map<String,Object> process(Address[] addresses, String subject) {
		String sender = null;
		if (null==addresses || addresses.length!=1 || !verify(addresses[0].toString())){
			log.error("not a good sender address, exiting!");
			return null;
		}else{
			sender = ((InternetAddress)addresses[0]).getAddress();
		}
		return process(sender, subject);
	}

	public Map<String,Object> process(String sender, String subject) {
		//from now on we can send error messages back
		Action action = readCommand(subject);
		String[] ca = subject.trim().split(" ");
		Map<String,Object> c = new HashMap<>();
		if (null==action){
			return sendError(sender, "unknown command");
		}
		c.put("action", action.getText());
		c.put("source",(sender.contains("@"))?"email":"sms");
		c.put("locale","en");
		c.put("msgAddress", sender);
		switch (action) {
		case BALANCE:
			break;
		case CREATE:
			break;
		case DEPOSIT:
			break;
		case HELP:
			Map<String,Object> r = new HashMap<>();
			r.put("locale", "en");
			r.put("msgAddress", sender);
			r.put("service", "37coins");
			r.put("action", "help");
			return r;
		case SEND_CONFIRM:
			//TODO: think about it
			break;
		case SEND:
			Double btc = 0.0;
			
			try{
				btc = Double.parseDouble(ca[1]);
			}catch(Exception e){
				return sendError(sender,"number not recognized");
			}
			c.put("amount",btc);
			c.put("receiver",ca[2]);
			break;	
		}
		return c;
	}
	
	public Map<String,Object> sendError(String sender, String errMsg){
		Map<String,Object> r = new HashMap<>();
		r.put("locale","en");
		r.put("msgAddress",sender);
		r.put("action","error000");
		r.put("error","000");
		r.put("service", "37coins");
		r.put("devErrorMsg", errMsg);
		return r;
	}
}
