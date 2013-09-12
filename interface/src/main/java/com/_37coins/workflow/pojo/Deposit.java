package com._37coins.workflow.pojo;

import java.math.BigDecimal;
import java.util.Currency;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Deposit {
	
	private BigDecimal amount;
	
	private Currency currency;


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

	
}
