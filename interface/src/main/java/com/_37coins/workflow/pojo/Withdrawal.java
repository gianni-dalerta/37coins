package com._37coins.workflow.pojo;

import java.math.BigDecimal;
import java.util.Currency;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Withdrawal {
	
	private MessageAddress msgDest;
	
	private PaymentAddress payDest;
	
	private BigDecimal amount;
	
	private BigDecimal fee;
	
	private String feeAccount;
	
	private Currency currency;
	
	private String txId;
	
	private String taskToken;
	
	private String confLink;


	public String getTaskToken() {
		return taskToken;
	}

	public Withdrawal setTaskToken(String taskToken) {
		this.taskToken = taskToken;
		return this;
	}

	public String getConfLink() {
		return confLink;
	}

	public Withdrawal setConfLink(String confLink) {
		this.confLink = confLink;
		return this;
	}

	public MessageAddress getMsgDest() {
		return msgDest;
	}

	public Withdrawal setMsgDest(MessageAddress msgAddress) {
		this.msgDest = msgAddress;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Withdrawal setAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public Currency getCurrency() {
		return currency;
	}

	public Withdrawal setCurrency(Currency currency) {
		this.currency = currency;
		return this;
	}

	public PaymentAddress getPayDest() {
		return payDest;
	}

	public Withdrawal setPayDest(PaymentAddress payAddress) {
		this.payDest = payAddress;
		return this;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public Withdrawal setFee(BigDecimal fee) {
		this.fee = fee;
		return this;
	}

	public String getTxId() {
		return txId;
	}

	public Withdrawal setTxId(String txId) {
		this.txId = txId;
		return this;
	}

	public String getFeeAccount() {
		return feeAccount;
	}

	public Withdrawal setFeeAccount(String feeAccount) {
		this.feeAccount = feeAccount;
		return this;
	}
	
	

}
