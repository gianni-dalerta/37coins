package com._37coins.workflow.pojo;

import java.math.BigDecimal;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.ext.beans.ResourceBundleModel;

@JsonInclude(Include.NON_NULL)
public class DataSet {
	
	public enum Action {
		//REQUESTS
		DEPOSIT_REQ("DepositReq"), // request a bitcoin address to receive a payment
		WITHDRAWAL_REQ("WithdrawalReq"), // send a payment
		WITHDRAWAL_REQ_OTHER("WithdrawalReqOther"), // request a payment
		WITHDRAWAL_CONF("WithdrawalConf"), // confirm a payment
		BALANCE("Balance"), // request the balance
		GW_BALANCE("GwBal"), // request the balance
		HELP("Help"), 
		TRANSACTION("Transactions"),
		//RESPONSES
		SIGNUP("Signup"), // create a new account
		RESET("Reset"), // gateway reset password
		REGISTER("Register"), //gateway signup
		DEPOSIT_CONF("DepositConf"),
		FORMAT_ERROR("FormatError"),
		UNKNOWN_COMMAND("UnknownCommand"),
		ACCOUNT_BLOCKED("AccountBlocked"),
		INSUFISSIENT_FUNDS("InsufficientFunds"),
		BELOW_FEE("BelowFee"), 
		TIMEOUT("Timeout"),
		TX_FAILED("TransactionFailed"),
		TX_CANCELED("TransactionCanceled");

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
	
	public DataSet(){
		setService("37coins");
	}
	
	private Action action;
	
	private Locale locale;
	
	private MessageAddress to;
	
	private Object payload;
	
	private String service;
	
	private String gwCn;
	
	private BigDecimal gwFee;
	
	private String cn;
	
	private ResourceBundleModel resBundle;
	
	//########## UTILS

	@Override
	public boolean equals(Object obj) {
		if (obj==null)
			return false;
		JsonNode a = new ObjectMapper().valueToTree(this);
		JsonNode b = new ObjectMapper().valueToTree(obj);
		return a.equals(b);
	};	
	
	@Override
	public String toString(){
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}


	//########## GETTERS && SETTERS
	
	public Action getAction() {
		return action;
	}

	public DataSet setAction(Action action) {
		this.action = action;
		return this;
	}
	
	@JsonIgnore
	public String getLocaleString() {
		if (null!=locale)
			return locale.toString().replace("_", "-");
		return null;
	}

	public Locale getLocale() {
		return locale;
	}

	public DataSet setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}
	
	@JsonIgnore
	public DataSet setLocaleString(String locale){
		if (null==locale)
			return this;
		String[] l = locale.split("[-_]");
		switch(l.length){
	        case 2: this.locale = new Locale(l[0], l[1]); break;
	        case 3: this.locale = new Locale(l[0], l[1], l[2]); break;
	        default: this.locale = new Locale(l[0]); break;
	    }
		return this;
	}

	public MessageAddress getTo() {
		return to;
	}

	public DataSet setTo(MessageAddress to) {
		this.to = to;
		return this;
	}

	public Object getPayload() {
		return payload;
	}

	public DataSet setPayload(Object payload) {
		this.payload = payload;
		return this;
	}

	public String getService() {
		return service;
	}

	public DataSet setService(String service) {
		this.service = service;
		return this;
	}

	public String getCn() {
		return cn;
	}

	public DataSet setCn(String cn) {
		this.cn = cn;
		return this;
	}
	
	@JsonIgnore
	public String getGwCn() {
		return gwCn;
	}

	public DataSet setGwCn(String gwCn) {
		this.gwCn = gwCn;
		return this;
	}

	@JsonIgnore
	public BigDecimal getGwFee() {
		return gwFee;
	}

	public DataSet setGwFee(BigDecimal gwFee) {
		this.gwFee = gwFee;
		return this;
	}

	public ResourceBundleModel getResBundle() {
		return resBundle;
	}

	public DataSet setResBundle(ResourceBundleModel resBundle) {
		this.resBundle = resBundle;
		return this;
	}

}
