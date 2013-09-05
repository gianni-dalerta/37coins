package com._37coins;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import com.google.inject.servlet.GuiceFilter;

public class Filter extends GuiceFilter {
	@Override
	public void doFilter(	ServletRequest req, 
							ServletResponse rsp, 
							FilterChain fc) throws IOException, ServletException {
		super.doFilter(req, rsp, fc);
	}
}
