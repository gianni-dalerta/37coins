package com._37coins.workflow.pojo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Withdrawal {
	
	private MessageAddress msgDest;
	
	private PaymentAddress payDest;
	
	private BigDecimal amount;
	
	private BigDecimal fee;
	
	private BigDecimal balance;
	
	private String feeAccount;
	
	private String txId;
	
	private String confLink;
	
	private String confKey;
	
	private String comment;

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

	public String getConfKey() {
		return confKey;
	}

	public Withdrawal setConfKey(String confKey) {
		this.confKey = confKey;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public Withdrawal setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public Withdrawal setBalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

}
