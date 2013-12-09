package com._37coins;

import javax.inject.Inject;
import javax.naming.LimitExceededException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAccessAuthFilter extends BasicHttpAuthenticationFilter {
	public static final String SF = "(&(objectClass=person)(|(mail={0})(cn={0})(givenName={0})))";
	private static final Logger log = LoggerFactory.getLogger(BasicAccessAuthFilter.class);
	
	final private JndiLdapContextFactory jlc;
	
	@Inject
	public BasicAccessAuthFilter(JndiLdapContextFactory jlc) {
		this.setApplicationName("Password Self Service");
		this.setAuthcScheme("B4S1C");
		this.jlc = jlc;
	}
	
	public static SearchResult searchUnique(String searchFilter,InitialLdapContext ctx) throws IllegalStateException, NamingException{
		ctx.setRequestControls(null);
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchControls.setTimeLimit(1000);
		NamingEnumeration<?> namingEnum = ctx.search(MessagingServletConfig.ldapBaseDn, searchFilter, searchControls);
		if (namingEnum.hasMore ()){
			SearchResult result = (SearchResult) namingEnum.next();
			if (namingEnum.hasMore()){
				throw new LimitExceededException("search with filter "+searchFilter+" returned more than 1 result");
			}
			namingEnum.close();
			return result;
		}else{
			throw new NameNotFoundException("search with filter "+searchFilter+" returned no result");
		}
	}
	
	@Override
	protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String authorizationHeader = getAuthzHeader(request);
        if (authorizationHeader == null || authorizationHeader.length() == 0) {
            // Create an empty authentication token since there is no
            // Authorization header.
            return createToken("", "", request, response);
        }

        if (log.isDebugEnabled()) {
            log.debug("Attempting to execute login with headers [" + authorizationHeader + "]");
        }

        String[] prinCred = getPrincipalsAndCredentials(authorizationHeader, request);
        if (prinCred == null || prinCred.length < 2) {
            // Create an authentication token with an empty password,
            // since one hasn't been provided in the request.
            String username = prinCred == null || prinCred.length == 0 ? "" : prinCred[0];
            return createToken(username, "", request, response);
        }
        
        String username = prinCred[0];
        String password = prinCred[1];
    	String sf = SF.replace("{0}", username);
		try {
			AuthenticationToken at = new UsernamePasswordToken(MessagingServletConfig.ldapUser, MessagingServletConfig.ldapPw);
			InitialLdapContext ctx = (InitialLdapContext)jlc.getLdapContext(at.getPrincipal(),at.getCredentials());
	    	SearchResult result = searchUnique(sf, ctx);
	        Attributes attrs = result.getAttributes();
	        username = "cn="+attrs.get("cn").get(0)+",ou=gateways,"+MessagingServletConfig.ldapBaseDn;
	        ctx.close();
		} catch (IllegalStateException | NamingException e) {
			e.printStackTrace();
			log.warn(username + "not found in directory");
		}
        return createToken(username, password, request, response);
	}

}
