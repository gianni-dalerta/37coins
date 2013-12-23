package com._37coins.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AccountRequest {
	
	private String email;
	
	private String password;
	
	private String ticket;
	
	private String token;

	public String getEmail() {
		return email;
	}

	public AccountRequest setEmail(String email) {
		this.email = email;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public AccountRequest setPassword(String password) {
		this.password = password;
		return this;
	}

	public String getTicket() {
		return ticket;
	}

	public AccountRequest setTicket(String ticket) {
		this.ticket = ticket;
		return this;
	}

	public String getToken() {
		return token;
	}

	public AccountRequest setToken(String tocken) {
		this.token = tocken;
		return this;
	}

}
