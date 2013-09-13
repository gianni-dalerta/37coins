package com._37coins.workflow.pojo;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.ext.beans.ResourceBundleModel;

@JsonInclude(Include.NON_NULL)
public class Response {
	
	public enum RspAction {
		CREATE("create"), // create a new account
		DEPOSIT("deposit"), // request a bitcoin address to receive a payment
		SEND("send"), // send a payment
		REQUEST("request"), // send a payment
		SEND_CONFIRM("confirmSend"), 
		BALANCE("balance"), // request the balance
		RECEIVED("received"),
		HELP("help"),
		FORMAT_ERROR("formErr"),
		UNKNOWN_COMMAND("cmdErr"), 
		INSUFISSIENT_FUNDS("insufFund"), 
		TIMEOUT("timout"),
		TX_FAILED("txErr");
		

		private String text;

		RspAction(String text) {
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
		public static RspAction fromString(String text) {
			if (text != null) {
				for (RspAction b : RspAction.values()) {
					if (text.equalsIgnoreCase(b.text)) {
						return b;
					}
				}
			}
			return null;
		}
	}
	
	private RspAction action;
	
	private Locale locale;
	
	private MessageAddress to;
	
	private Object payload;
	
	private String service;
	
	private Long accountId;
	
	private ResourceBundleModel resBundle;
	
	private String sendHash;
	
	private String bizUrl;
	
	public Response respondTo(Request req){
		setLocale(req.getLocale());
		if (null!=req.getAction()){
			setAction(RspAction.fromString(req.getAction().getText()));
		}
		setTo(req.getFrom());
		setPayload(req.getPayload());
		setService(req.getService());
		setAccountId(req.getAccountId());
		return this;
	}
	
	public Response validate() throws IncompleteException{
		if (action==null || locale == null || to == null){
			throw new IncompleteException();
		}
		return this;
	}

	public RspAction getAction() {
		return action;
	}

	public Response setAction(RspAction action) {
		this.action = action;
		return this;
	}

	public Locale getLocale() {
		return locale;
	}

	public Response setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}

	public MessageAddress getTo() {
		return to;
	}

	public Response setTo(MessageAddress to) {
		this.to = to;
		return this;
	}

	public Object getPayload() {
		return payload;
	}

	public Response setPayload(Object payload) {
		this.payload = payload;
		return this;
	}

	public String getService() {
		return service;
	}

	public Response setService(String service) {
		this.service = service;
		return this;
	}

	public Long getAccountId() {
		return accountId;
	}

	public Response setAccountId(Long accountId) {
		this.accountId = accountId;
		return this;
	}

	public void copy(Response rsp) {
		setAccountId(rsp.getAccountId());
		setAction(rsp.getAction());
		setLocale(rsp.getLocale());
		setPayload(rsp.getPayload());
		setService(rsp.getService());
		setTo(rsp.getTo());
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

	public ResourceBundleModel getResBundle() {
		return resBundle;
	}

	public Response setResBundle(ResourceBundleModel resBundle) {
		this.resBundle = resBundle;
		return this;
	}

	public String getSendHash() {
		return sendHash;
	}

	public Response setSendHash(String sendHash) {
		this.sendHash = sendHash;
		return this;
	}

	public String getBizUrl() {
		return bizUrl;
	}

	public Response setBizUrl(String bizUrl) {
		this.bizUrl = bizUrl;
		return this;
	}

}
