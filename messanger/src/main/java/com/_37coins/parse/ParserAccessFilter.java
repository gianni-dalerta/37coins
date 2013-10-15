package com._37coins.parse;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class ParserAccessFilter implements Filter {

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		String clientAddr = request.getRemoteAddr();
		if (clientAddr.equalsIgnoreCase("localhost")
				|| clientAddr.equalsIgnoreCase("127.0.0.1")) {
			chain.doFilter(request, response);
		} else {
			handleInvalidAccess(request, response, clientAddr);
			return;
		}
	}

	private void handleInvalidAccess(ServletRequest request,
			ServletResponse response, String clientAddr) throws IOException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
