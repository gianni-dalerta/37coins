package com._37coins.workflow.pojo;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
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
		HELP("Help"), 
		TRANSACTION("Transactions"),
		//RESPONSES
		SIGNUP("Signup"), // create a new account
		DEPOSIT_CONF("DepositConf"),
		FORMAT_ERROR("FormatError"),
		UNKNOWN_COMMAND("UnknownCommand"), 
		INSUFISSIENT_FUNDS("InsufficientFunds"), 
		TIMEOUT("Timeout"),
		TX_FAILED("TransactionFailed");

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
	
	private Long accountId;
	
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

	public Locale getLocale() {
		return locale;
	}

	public DataSet setLocale(Locale locale) {
		this.locale = locale;
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

	public Long getAccountId() {
		return accountId;
	}

	public DataSet setAccountId(Long accountId) {
		this.accountId = accountId;
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
