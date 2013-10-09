package com._37coins;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.LimitExceededException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.apache.shiro.realm.ldap.JndiLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.apache.shiro.subject.PrincipalCollection;

public class AuthorizingRealm extends JndiLdapRealm {
	public static final String SF = "(&(objectClass=person)(|(mail={0})(cn={0})(givenName={0})))";
	
	@Inject
	public AuthorizingRealm(JndiLdapContextFactory jlc){
		this.setContextFactory(jlc);
		this.setUserDnTemplate("cn={0},"+MessagingServletConfig.ldapBaseDn);
	}
	
	public static SearchResult searchUnique(String searchFilter,JndiLdapContextFactory jlc) throws IllegalStateException, NamingException{
		InitialLdapContext ctx = null;
		AuthenticationToken at = new UsernamePasswordToken(MessagingServletConfig.ldapUser, MessagingServletConfig.ldapPw);
		ctx = (InitialLdapContext)jlc.getLdapContext(at.getPrincipal(),at.getCredentials());
		ctx.setRequestControls(null);
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchControls.setTimeLimit(5000);
		NamingEnumeration<?> namingEnum = ctx.search(MessagingServletConfig.ldapBaseDn, searchFilter, searchControls);
		if (namingEnum.hasMore ()){
			SearchResult result = (SearchResult) namingEnum.next();
			if (namingEnum.hasMore()){
				throw new LimitExceededException("search with filter "+searchFilter+" returned more than 1 result");
			}
			namingEnum.close();
			ctx.close();
			return result;
		}else{
			throw new NameNotFoundException("search with filter "+searchFilter+" returned no result");
		}
	}
	
	@Override
	protected Object getLdapPrincipal(AuthenticationToken arg0) {
		String rv = null;
		try {
			String sf = SF.replace("{0}", (String)arg0.getPrincipal());
			SearchResult result = searchUnique(sf, (JndiLdapContextFactory)this.getContextFactory());
	        Attributes attrs = result.getAttributes();
	        rv = "cn="+attrs.get("cn").get(0)+","+MessagingServletConfig.ldapBaseDn; 
		} catch (NamingException e) {
			e.printStackTrace();
		}
		if (rv == null){
			rv = (String)super.getLdapPrincipal(arg0);
		}
		return rv;
	}
	
	@Override
	protected AuthorizationInfo queryForAuthorizationInfo(
			PrincipalCollection principals,
			LdapContextFactory ldapContextFactory) throws NamingException {
		System.out.println("queryForAuthorizationInfo");
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

		SearchControls searchCtls = new SearchControls();
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// SHIRO-115 - prevent potential code injection:
		String searchFilter = "(&(objectClass=*)(CN={0}))";
		Object[] searchArguments = new Object[] { username };
		String searchBase = "dc=37coins,dc=com";
		NamingEnumeration<SearchResult> answer = ldapContext.search(searchBase, searchFilter,
				searchArguments, searchCtls);

		while (answer.hasMoreElements()) {
			SearchResult sr = (SearchResult) answer.next();

			Attributes attrs = sr.getAttributes();

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
		}
		return roleNames;
	}
	
	protected AuthorizationInfo buildAuthorizationInfo(Set<String> roleNames) {
		return new SimpleAuthorizationInfo(roleNames);
	}

}
