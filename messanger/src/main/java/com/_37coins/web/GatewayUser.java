package com._37coins.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class GatewayUser {
	
	private String id;
	private List<String> roles;
	private Long sessionTime;
	private Locale locale;
	private String mobile;
	private String code;
	private BigDecimal fee;
	private long testTime;
	
	public Locale getLocale() {
		return locale;
	}
	public GatewayUser setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}
	public String getId() {
		return id;
	}
	public GatewayUser setId(String id) {
		this.id = id;
		return this;
	}
	public List<String> getRoles() {
		return roles;
	}
	public GatewayUser setRoles(List<String> roles) {
		this.roles = roles;
		return this;
	}
	public Long getSessionTime() {
		return sessionTime;
	}
	public GatewayUser setSessionTime(Long sessionTime) {
		this.sessionTime = sessionTime;
		return this;
	}
	public String getMobile() {
		return mobile;
	}
	public GatewayUser setMobile(String mobile) {
		this.mobile = mobile;
		return this;
	}
	public String getCode() {
		return code;
	}
	public GatewayUser setCode(String code) {
		this.code = code;
		return this;
	}
	public BigDecimal getFee() {
		return fee;
	}
	public GatewayUser setFee(BigDecimal fee) {
		this.fee = fee;
		return this;
	}
	public long getTestTime() {
		return testTime;
	}
	public GatewayUser setTestTime(long testTime) {
		this.testTime = testTime;
		return this;
	}

}
