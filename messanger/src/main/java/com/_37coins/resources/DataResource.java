package com._37coins.resources;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale.Builder;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import com._37coins.MessagingServletConfig;
import com._37coins.web.GatewayUser;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Path(DataResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class DataResource {
	public final static String PATH = "/data";
	
	final private InitialLdapContext ctx;
	final private Cache cache;
	
	@Inject public DataResource(ServletRequest request, 
			Cache cache) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		ctx = (InitialLdapContext)httpReq.getAttribute("ctx");
		this.cache = cache;
	}
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/gateways")
	public Set<GatewayUser> getGateways(){
		Element e = cache.get("gateways");
		if (null!=e && !e.isExpired()){
			Set<GatewayUser> gateways = (Set<GatewayUser>)e.getObjectValue();
			return gateways;
		}
		Set<GatewayUser> rv = new HashSet<GatewayUser>();
		NamingEnumeration<?> namingEnum = null;
		try{
			ctx.setRequestControls(null);
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			searchControls.setTimeLimit(1000);
			namingEnum = ctx.search("ou=gateways,"+MessagingServletConfig.ldapBaseDn, "(objectClass=person)", searchControls);
			while (namingEnum.hasMore()){
				Attributes atts = ((SearchResult) namingEnum.next()).getAttributes();
				String mobile = (atts.get("mobile")!=null)?(String)atts.get("mobile").get():null;
				BigDecimal fee = (atts.get("description")!=null)?new BigDecimal((String)atts.get("description").get()):null;
				if (null!=mobile && null!=fee){
					PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
					PhoneNumber pn = phoneUtil.parse(mobile, "ZZ");
					String cc = phoneUtil.getRegionCodeForCountryCode(pn.getCountryCode());
					GatewayUser gu = new GatewayUser()
						.setMobile(PhoneNumberUtil.getInstance().format(pn,PhoneNumberFormat.E164))
						.setFee(fee)
						.setLocale(new Builder().setRegion(cc).build());
					rv.add(gu);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
		}finally{
			if(null!=namingEnum)
				try {
					namingEnum.close();
				} catch (NamingException e1) {}
		}
		cache.put(new Element("gateways", rv));
		return rv;
	}

}
