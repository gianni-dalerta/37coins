package com._37coins.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class PasswordRequest {
	
	private String token;
	
	private String ticket;
	
	private String email;
	
	private String password;
	
	private String dn;
	
	public String getDn() {
		return dn;
	}

	public PasswordRequest setDn(String dn) {
		this.dn = dn;
		return this;
	}

	public String getToken() {
		return token;
	}

	public PasswordRequest setToken(String token) {
		this.token = token;
		return this;
	}

	public String getTicket() {
		return ticket;
	}

	public PasswordRequest setTicket(String ticket) {
		this.ticket = ticket;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public PasswordRequest setEmail(String email) {
		this.email = email;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public PasswordRequest setPassword(String password) {
		this.password = password;
		return this;
	}

}
