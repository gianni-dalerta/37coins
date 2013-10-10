package com._37coins;

import javax.servlet.ServletContext;

import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.guice.web.ShiroWebModule;

import com.google.inject.Key;


public class MessagingShiroWebModule extends ShiroWebModule {

	public MessagingShiroWebModule(ServletContext servletContext) {
		super(servletContext);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void configureShiroWeb() {
		bindRealm().to(AuthorizingRealm.class).asEagerSingleton();
		bind(Authenticator.class).toInstance(new ModularRealmAuthenticator());
		Key<BasicAccessAuthFilter> customFilter = Key.get(BasicAccessAuthFilter.class);
		addFilterChain("/api/**", customFilter);
	}
	
}
