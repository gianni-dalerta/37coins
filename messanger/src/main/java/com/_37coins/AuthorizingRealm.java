package com._37coins;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.apache.shiro.realm.ldap.JndiLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.apache.shiro.subject.PrincipalCollection;

public class AuthorizingRealm extends JndiLdapRealm {
	
	@Inject
	public AuthorizingRealm(JndiLdapContextFactory jlc){
		this.setContextFactory(jlc);
		this.setUserDnTemplate("cn={0},"+MessagingServletConfig.ldapBaseDn);
	}
	
	@Override
	protected Object getLdapPrincipal(AuthenticationToken token) {
        return token.getPrincipal();
	}
	
	@Override
	protected AuthorizationInfo queryForAuthorizationInfo(
			PrincipalCollection principals,
			LdapContextFactory ldapContextFactory) throws NamingException {
		String username = (String) getAvailablePrincipal(principals);
		// Perform context search
		LdapContext ldapContext = ldapContextFactory.getSystemLdapContext();

		Set<String> roleNames;

		try {
			roleNames = getRoleNamesForUser(username, ldapContext);
		} finally {
			LdapUtils.closeContext(ldapContext);
		}
		
		if (null==roleNames){
			roleNames = new HashSet<String>();
		}
		roleNames.add("gateway");
		
		return buildAuthorizationInfo(roleNames);
	}

	protected Set<String> getRoleNamesForUser(String username,
			LdapContext ldapContext) throws NamingException {
		Set<String> roleNames;
		roleNames = new LinkedHashSet<String>();

		Attributes attrs = ldapContext.getAttributes(username);

		if (attrs != null) {
			NamingEnumeration<?> ae = attrs.getAll();
			while (ae.hasMore()) {
				Attribute attr = (Attribute) ae.next();

				if (attr.getID().equals("memberOf")) {

					Collection<String> groupNames = LdapUtils
							.getAllAttributeValues(attr);

					System.out.println("Groups found for user [" + username
							+ "]: " + groupNames);

					roleNames.addAll(groupNames);
				}
			}
		}
		return roleNames;
	}
	
	protected AuthorizationInfo buildAuthorizationInfo(Set<String> roleNames) {
		return new SimpleAuthorizationInfo(roleNames);
	}

}
