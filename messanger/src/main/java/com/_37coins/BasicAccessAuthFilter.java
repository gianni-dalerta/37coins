package com._37coins;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

public class BasicAccessAuthFilter extends BasicHttpAuthenticationFilter {
	
	public BasicAccessAuthFilter() {
		this.setApplicationName("Password Self Service");
		this.setAuthcScheme("B4S1C");
	}
	
	

}
