package com._37coins;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.restnucleus.filter.EncodingRequestWrapper;

import com.google.inject.Key;
import com.google.inject.name.Names;

@Singleton
public class DirectoryFilter implements Filter{
	
	final private JndiLdapContextFactory jlc;

	@Inject
	public DirectoryFilter(JndiLdapContextFactory jlc) {
		this.jlc = jlc;
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		InitialLdapContext ctx = null;
		AuthenticationToken at = new UsernamePasswordToken(MessagingServletConfig.ldapUser, MessagingServletConfig.ldapPw);
		try {
			ctx = (InitialLdapContext)jlc.getLdapContext(at.getPrincipal(),at.getCredentials());
		} catch (IllegalStateException | NamingException e) {
			throw new IOException(e);
		}
		HttpServletRequest httpReq = (HttpServletRequest)request;
		httpReq.setAttribute(Key.get(InitialLdapContext.class, Names.named("ctx")).toString(),ctx);
		httpReq.setAttribute("ctx", ctx);
		try {
			chain.doFilter(new EncodingRequestWrapper(httpReq), response);
		} finally {
			try {
				ctx.close();
			} catch (NamingException e) {
				e.printStackTrace();
			}
		}
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
