package com._37coins.resources;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.AuthorizingRealm;
import com._37coins.MessagingServletConfig;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Gateway;
import com._37coins.persistence.dto.MsgAddress;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.plivo.helper.api.client.RestAPI;
import com.plivo.helper.api.response.message.MessageResponse;
import com.plivo.helper.exception.PlivoException;

@Path(GatewayResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {
	public final static String PATH = "/api/gateway";
	public final static String HTML_RESPONSE_DONE = "<html><head><title>Confirmation</title></head><body>The gateway has been registered successfully.</body></html>";
	
	final private GenericRepository dao;
	final private Cache cache;
	final private JndiLdapContextFactory jlc;
	
	@Inject public GatewayResource(ServletRequest request, 
			Cache cache,
			JndiLdapContextFactory jlc) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		dao = (GenericRepository)httpReq.getAttribute("gr");
		this.cache = cache;
		this.jlc = jlc;
	}
	
	@GET
	@RolesAllowed({"gateway"})
	public Response get(@Context SecurityContext context){
		if (null!=context.getUserPrincipal()){
			String username = context.getUserPrincipal().getName();
			return Response.ok("<html><body>username: "+username+"</body></html>", MediaType.TEXT_HTML_TYPE).build();
		}else{
			return Response.ok("<html><body>unautheticated</body></html>", MediaType.TEXT_HTML_TYPE).build();
		}
	}
	
	@POST
	@Path("/login")
	@RolesAllowed({"gateway","admin"})
	public List<String> login(@Context SecurityContext context){
		List<String> roles = new ArrayList<>();
		if (null!=context.getUserPrincipal()){
			roles.add("gateway");
			roles.add("admin");
			Iterator<String> i = roles.iterator();
			while (i.hasNext()){
				String role = i.next();
				if (!context.isUserInRole(role)){
					i.remove();
				}
			}
		}
		return roles;
	}
	
	@POST
	@Path("/confirm")
	@RolesAllowed({"gateway"})
	public Map<String,String> confirm(@Context SecurityContext context,String code){
		Element e = cache.get(code); 
		PhoneNumber pn = (PhoneNumber)e.getObjectValue();
		InitialLdapContext ctx = null;
		AuthenticationToken at = new UsernamePasswordToken(MessagingServletConfig.ldapUser, MessagingServletConfig.ldapPw);
		try {
			ctx = (InitialLdapContext)jlc.getLdapContext(at.getPrincipal(),at.getCredentials());
			Attributes a = new BasicAttributes("mobile",pn.toString());
			ctx.modifyAttributes(context.getUserPrincipal().getName(), DirContext.ADD_ATTRIBUTE, a);
			ctx.close();
			Map<String,String> rv = new HashMap<>();
			rv.put("number", pn.toString());
			return rv;
		} catch (IllegalStateException | NamingException e1) {
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@POST
	@Path("/verify")
	@RolesAllowed({"gateway"})
	public void startVerify(@Context SecurityContext context,String phoneNumber){
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			// parse the number
			PhoneNumber pn = phoneUtil.parse(phoneNumber, "ZZ");
			// check if it exists
			SearchResult sr = null;
			try{
				sr = AuthorizingRealm.searchUnique("(&(objectClass=person)(mobile={0}))", jlc);
			}catch(NameNotFoundException e){
				//ok
			}
			if (sr!=null){
				throw new WebApplicationException("gateway with phone" + phoneNumber + " exists already.", Response.Status.CONFLICT);
			}
	        //create code
	        String code = RandomStringUtils.random(5, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
	        //save code + number + dn
	        cache.put(new Element(code, pn));
	        //send sms
	        RestAPI restApi = new RestAPI(MessagingServletConfig.plivoKey, MessagingServletConfig.plivoSecret, "v1");
	        LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			parameters.put("src", "+4971150888362");
			parameters.put("dst", pn.toString());
			parameters.put("text", "please confirm: "+ code);
			//parameters.put("url", "http://server/message/notification/");
			try {
				MessageResponse msgResponse = restApi.sendMessage(parameters);
				System.out.println(msgResponse.apiId);
				if (msgResponse.serverCode == 202) {
					System.out.println(msgResponse.messageUuids.get(0).toString());
				} else {
					System.out.println(msgResponse.error); 
				}
			} catch (PlivoException e) {
				System.out.println(e.getLocalizedMessage());
			}
		} catch (NumberParseException | IllegalStateException | NamingException e) {
			throw new WebApplicationException(e,Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@POST
	public Response register(@FormParam("ownerAddress") String ownerAddress, 
			@FormParam("address") String address, 
			@FormParam("coutryCode") String countryCode,
			@FormParam("fee") String fee,
			@FormParam("password") String password){
		RNQuery query = new RNQuery().addFilter("address", ownerAddress);
		MsgAddress ma = dao.queryEntity(query, MsgAddress.class,false);
		if (null==ma){
			ma = new MsgAddress()
				.setAddress(query.getFilter("address"))
				.setOwner(new Account());
			dao.add(ma);
		}
		RNQuery gqQuery = new RNQuery().addFilter("address", address);
		Gateway gw = dao.queryEntity(gqQuery, Gateway.class, false);
		if (gw==null){
			gw = new Gateway()
				.setAddress(address)
				.setCountryCode((null!=countryCode)?Integer.parseInt(countryCode):null)
				.setFee(new BigDecimal(fee).setScale(8,RoundingMode.HALF_UP))
				.setPassword(password)
				.setOwner(ma.getOwner());
			dao.add(gw);
			return Response.ok(HTML_RESPONSE_DONE, MediaType.TEXT_HTML_TYPE).build();
		}else{
			throw new WebApplicationException("gateway exists already",
					javax.ws.rs.core.Response.Status.CONFLICT);
		}
	}
	
}