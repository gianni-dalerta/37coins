package com._37coins.workflow.pojo;

import java.math.BigDecimal;
import java.util.Currency;

public class Withdrawal {
	
	private MessageAddress msgDest;
	
	private PaymentAddress payDest;
	
	private BigDecimal amount;
	
	private BigDecimal fee;
	
	private Currency currency;

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
	
	

}
