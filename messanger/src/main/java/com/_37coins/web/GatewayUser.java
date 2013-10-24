package com._37coins.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class GatewayUser {
	
	private String id;
	private List<String> roles;
	private Locale locale;
	private String mobile;
	private String code;
	private BigDecimal fee;
	private String envayaToken;
	
	public String getEnvayaToken() {
		return envayaToken;
	}

	public GatewayUser setEnvayaToken(String envayaToken) {
		this.envayaToken = envayaToken;
		return this;
	}

	public Locale getLocale() {
		return locale;
	}
	
	@JsonIgnore
	public String getLocaleString() {
		if (null!=locale){
			return locale.toString().replace("_", "-");
		}
		return null;
	}
	
	public GatewayUser setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}
	
	@JsonIgnore
	public GatewayUser setLocaleString(String locale){
		String[] l = locale.split("[-_]");
		switch(l.length){
	        case 2: this.locale = new Locale(l[0], l[1]); break;
	        case 3: this.locale = new Locale(l[0], l[1], l[2]); break;
	        default: this.locale = new Locale(l[0]); break;
	    }
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
	public String getMobile() {
		return mobile;
	}
	public GatewayUser setMobile(String mobile) {
		this.mobile = mobile;
		return this;
	}
	public String getCode() {
		if (null!=code && code.length()<2){
			return null;
		}
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

}
