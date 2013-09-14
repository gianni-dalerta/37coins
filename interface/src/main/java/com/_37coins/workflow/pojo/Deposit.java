package com._37coins.workflow.pojo;

import java.math.BigDecimal;
import java.util.Currency;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Deposit {
	
	private BigDecimal amount;
	
	private Currency currency;

	private BigDecimal balance;
	
	private String txId;
	
	private String comment;
	
	public BigDecimal getAmount() {
		return amount;
	}

	public Deposit setAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public Currency getCurrency() {
		return currency;
	}

	public Deposit setCurrency(Currency currency) {
		this.currency = currency;
		return this;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public Deposit setBalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public String getTxId() {
		return txId;
	}

	public Deposit setTxId(String txId) {
		this.txId = txId;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public Deposit setComment(String comment) {
		this.comment = comment;
		return this;
	}

	
	
}
