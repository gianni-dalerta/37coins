package com._37coins;

import javax.servlet.ServletContext;

import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.guice.web.ShiroWebModule;

import com.google.inject.Key;
import com.google.inject.name.Names;


public class MessagingShiroWebModule extends ShiroWebModule {

	public MessagingShiroWebModule(ServletContext servletContext) {
		super(servletContext);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void configureShiroWeb() {
		bindConstant().annotatedWith(Names.named("shiro.globalSessionTimeout")).to(30000L);
		bindConstant().annotatedWith(Names.named("shiro.successUrl")).to("/success");
		bindRealm().to(AuthorizingRealm.class).asEagerSingleton();
		bind(Authenticator.class).toInstance(new ModularRealmAuthenticator());
		Key<BasicAccessAuthFilter> customFilter = Key.get(BasicAccessAuthFilter.class);
		addFilterChain("/login", customFilter);
		addFilterChain("/api/**", customFilter);
	}
	
}
