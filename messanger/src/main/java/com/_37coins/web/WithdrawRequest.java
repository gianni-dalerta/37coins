package com._37coins.web;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class WithdrawRequest {
	
	private BigDecimal balance;
	
	private BigDecimal amount;
	
	private String address;

	public BigDecimal getAmount() {
		return amount;
	}

	public WithdrawRequest setAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public WithdrawRequest setAddress(String address) {
		this.address = address;
		return this;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public WithdrawRequest setBalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

}
