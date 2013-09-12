package com._37coins.workflow.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MessageAddress {
	
	public enum MsgType {
		SMS,
		EMAIL,
		UNKNOWN;
	}
	
	private String address;
	
	private MsgType addressType;
	
	private String gateway;

	public String getAddress() {
		return address;
	}

	public MessageAddress setAddress(String address) {
		this.address = address;
		return this;
	}
	
	public MsgType getAddressType() {
		return addressType;
	}

	public MessageAddress setAddressType(MsgType addressType) {
		this.addressType = addressType;
		return this;
	}

	public String getGateway() {
		return gateway;
	}

	public MessageAddress setGateway(String gateway) {
		this.gateway = gateway;
		return this;
	}

}
