package com._37coins.workflow.pojo;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
public class Request {
	
	public enum ReqAction {
		CREATE("create"), // create a new account
		DEPOSIT("deposit"), // request a bitcoin address to receive a payment
		SEND("send"), // send a payment
		REQUEST("request"), // send a payment
		SEND_CONFIRM("confirm"), 
		BALANCE("balance"), // request the balance
		HELP("help"), 
		TRANSACTION("tx");
		

		private String text;

		ReqAction(String text) {
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
		public static ReqAction fromString(String text) {
			if (text != null) {
				for (ReqAction b : ReqAction.values()) {
					if (text.equalsIgnoreCase(b.text)) {
						return b;
					}
				}
			}
			return null;
		}
	}
	
	public Request(){
		setService("37coins");
	}
	
	private ReqAction action;
	
	private Locale locale;
	
	private MessageAddress from;
	
	private Long accountId;
	
	private Object payload;
	
	private String service;

	public ReqAction getAction() {
		return action;
	}

	public Request setAction(ReqAction action) {
		this.action = action;
		return this;
	}

	public Locale getLocale() {
		return locale;
	}

	public Request setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}

	public MessageAddress getFrom() {
		return from;
	}

	public Request setFrom(MessageAddress from) {
		this.from = from;
		return this;
	}

	public Object getPayload() {
		return payload;
	}

	public Request setPayload(Object payload) {
		this.payload = payload;
		return this;
	}

	public String getService() {
		return service;
	}

	public Request setService(String service) {
		this.service = service;
		return this;
	}

	public Long getAccountId() {
		return accountId;
	}

	public Request setAccountId(Long accountId) {
		this.accountId = accountId;
		return this;
	}
	
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
	

}
