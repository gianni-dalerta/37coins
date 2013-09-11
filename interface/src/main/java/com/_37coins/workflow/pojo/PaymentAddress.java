package com._37coins.workflow.pojo;


public class PaymentAddress {
	
	public enum PaymentType {
		BTC, 
		ACCOUNT;
	}
	
	private String address;
	
	private PaymentType addressType;

	public String getAddress() {
		return address;
	}

	public PaymentAddress setAddress(String address) {
		this.address = address;
		return this;
	}

	public PaymentType getAddressType() {
		return addressType;
	}

	public PaymentAddress setAddressType(PaymentType addressType) {
		this.addressType = addressType;
		return this;
	}

}
