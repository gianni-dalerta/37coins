package com._37coins.resources;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import com._37coins.BasicAccessAuthFilter;
import com._37coins.MessagingServletConfig;
import com._37coins.sendMail.MailServiceClient;
import com._37coins.web.GatewayUser;
import com._37coins.web.WithdrawRequest;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.plivo.helper.api.client.RestAPI;
import com.plivo.helper.api.response.call.Call;
import com.plivo.helper.exception.PlivoException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@Path(GatewayResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {
	public final static String PATH = "/api/gateway";
	private static final BigDecimal FEE = new BigDecimal("0.0007").setScale(8);
	
	final private InitialLdapContext ctx;
	final private Cache cache;
	final private NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	private final MailServiceClient mailClient;
	
	@Inject public GatewayResource(ServletRequest request, 
			Cache cache, MailServiceClient mailClient,
			NonTxWorkflowClientExternalFactoryImpl nonTxFactory) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		ctx = (InitialLdapContext)httpReq.getAttribute("ctx");
		this.nonTxFactory = nonTxFactory;
		this.mailClient = mailClient;
		this.cache = cache;
	}
	
	@GET
	@RolesAllowed({"gateway"})
	public GatewayUser login(@Context SecurityContext context){
		GatewayUser gu = new GatewayUser().setRoles(new ArrayList<String>());
		if (null!=context.getUserPrincipal()){
			gu.getRoles().add("gateway");
			gu.getRoles().add("admin");
			Iterator<String> i = gu.getRoles().iterator();
			while (i.hasNext()){
				String role = i.next();
				if (!context.isUserInRole(role)){
					i.remove();
				}
			}
		}
		try{
			gu.setId(context.getUserPrincipal().getName());
			Attributes atts = ctx.getAttributes(gu.getId(),new String[]{"mobile", "description","preferedLocale","departmentNumber"});
			gu.setMobile((atts.get("mobile")!=null)?(String)atts.get("mobile").get():null);
			gu.setCode("");
			if (atts.get("preferedLocale")!=null){
				gu.setLocaleString((String)atts.get("preferedLocale").get());
			}
			//some abuses here: description -> fee and departementNumber -> envayaToken
			gu.setFee((atts.get("description")!=null)?new BigDecimal((String)atts.get("description").get()).setScale(8):null);
			gu.setEnvayaToken((atts.get("departmentNumber")!=null)?(String)atts.get("departmentNumber").get():null);
		}catch(IllegalStateException | NamingException e){
			e.printStackTrace();
			throw new WebApplicationException(e,Response.Status.INTERNAL_SERVER_ERROR);
		}
		return gu;
	}
	
	@PUT
	@RolesAllowed({"gateway"})
	public GatewayUser confirm(@Context SecurityContext context,GatewayUser gu){
		GatewayUser rv = null;
		//fish user from directory
		String mobile = null;
		BigDecimal fee = null;
		try{
			Attributes atts = ctx.getAttributes(context.getUserPrincipal().getName(),new String[]{"mobile", "description","preferedLocale","departmentNumber"});
			mobile = (atts.get("mobile")!=null)?(String)atts.get("mobile").get():null;
			fee = (atts.get("description")!=null)?new BigDecimal((String)atts.get("description").get()).setScale(8):null;
		}catch(IllegalStateException | NamingException e){
			e.printStackTrace();
			throw new WebApplicationException(e,Response.Status.INTERNAL_SERVER_ERROR);
		}
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		if (mobile ==null && gu.getCode()==null && null!=gu.getMobile()){
			//start validation for received mobile number
			try {
				// parse the number
				PhoneNumber pn = phoneUtil.parse(gu.getMobile(), "ZZ");
				// check if it exists
				try{
					SearchResult sr = BasicAccessAuthFilter.searchUnique("(&(objectClass=person)(mobile={0}))", ctx);
					if (sr!=null)
						throw new WebApplicationException("gateway with phone" + pn + " exists already.", Response.Status.CONFLICT);
				}catch(NameNotFoundException e){
					//ok
				}
		        //create code
		        String code = RandomStringUtils.random(5, "0123456789");
		        //save code + number + dn
		        cache.put(new Element(code, pn));
		        //call and tell code
				RestAPI restAPI = new RestAPI(MessagingServletConfig.plivoKey, MessagingServletConfig.plivoSecret, "v1");
				LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
			    params.put("from", "+4971150888362");
			    params.put("to", phoneUtil.format(pn, PhoneNumberFormat.E164));
			    params.put("answer_url", MessagingServletConfig.basePath + "/plivo/register/"+code+"/"+gu.getLocale());
			    params.put("time_limit", "55");
			    Call response = restAPI.makeCall(params);
			    if (response.serverCode != 200 && response.serverCode != 201 && response.serverCode !=204){
			    	throw new PlivoException(response.message);
			    }
			    System.out.println("code: "+code);
			} catch (NumberParseException | IllegalStateException | NamingException | PlivoException e) {
				e.printStackTrace();
				throw new WebApplicationException(e,Response.Status.INTERNAL_SERVER_ERROR);
			}
		}else if (gu.getCode()!=null && gu.getFee()==null){
			//create queue in mqs
			Matcher m = Pattern.compile("[Cc][Nn]=([^,]+),").matcher(context.getUserPrincipal().getName());
			m.find();
			String cn = m.group(1);
			ConnectionFactory factory = new ConnectionFactory();
			Connection conn = null;
			Channel channel = null;
			try {
				factory.setUri(MessagingServletConfig.queueUri);
				conn = factory.newConnection();
				channel = conn.createChannel();
				channel.queueDeclare(cn, true, false, false, null);
				channel.queueBind(cn, "amq.direct", cn);
				channel.close();
				conn.close();
			} catch (KeyManagementException | NoSuchAlgorithmException
					| URISyntaxException | IOException e1) {
				e1.printStackTrace();
				throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
			}finally{
				try {
					if (null!=channel&&channel.isOpen()) channel.close();
					if (null!=conn&&conn.isOpen()) conn.close();
				} catch (IOException e1) {}
			}
			//complete validation for mobile number
			Element e = cache.get(gu.getCode()); 
			if (null==e){
				throw new WebApplicationException(gu.getCode()+" not correct", Response.Status.NOT_FOUND);
			}
			PhoneNumber pn = (PhoneNumber)e.getObjectValue();
			try {
				Attributes a = new BasicAttributes();
				a.put("preferredLanguage", gu.getLocaleString());
				a.put("mobile",phoneUtil.format(pn, PhoneNumberFormat.E164));
				//some abuses here: description -> fee and departementNumber -> envayapw
				a.put("description",FEE.toString());
				String envayaToken = RandomStringUtils.random(12, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
				a.put("departmentNumber",envayaToken);
				ctx.modifyAttributes(context.getUserPrincipal().getName(), DirContext.REPLACE_ATTRIBUTE, a);
				rv = new GatewayUser()
					.setLocale(gu.getLocale())
					.setFee(FEE)
					.setMobile(phoneUtil.format(pn, PhoneNumberFormat.E164))
					.setEnvayaToken(envayaToken);
			} catch (IllegalStateException | NamingException e1) {
				e1.printStackTrace();
				throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
			}
		}else if (mobile.equalsIgnoreCase(gu.getMobile()) && gu.getFee()!=null) {
			//set/update fee
			if (gu.getFee().compareTo(fee)!=0){
				try {
					Attributes a = new BasicAttributes("description",gu.getFee().toString());
					ctx.modifyAttributes(context.getUserPrincipal().getName(), DirContext.REPLACE_ATTRIBUTE, a);
					rv = new GatewayUser().setFee(gu.getFee());
				} catch (IllegalStateException | NamingException e1) {
					e1.printStackTrace();
					throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
				}
			}
		}else{
			throw new WebApplicationException("unexpected state", Response.Status.BAD_REQUEST);
		}
		return rv;
	}
	
	@POST
	@Path("/fee")
	@RolesAllowed({"gateway"})
	public GatewayUser setFee(@Context SecurityContext context, GatewayUser gu){
		GatewayUser rv = null;
		if (gu.getFee().compareTo(new BigDecimal("0.001"))>0){
			throw new WebApplicationException("fee to high", Response.Status.BAD_REQUEST);
		}
		try {
			Attributes a = new BasicAttributes("description",gu.getFee().toString());
			ctx.modifyAttributes(context.getUserPrincipal().getName(), DirContext.REPLACE_ATTRIBUTE, a);
			rv = new GatewayUser().setFee(gu.getFee());
		} catch (IllegalStateException | NamingException e1) {
			e1.printStackTrace();
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return rv;
	}
	
	@PUT
	@Path("/fee")
	@RolesAllowed({"gateway"})
	public GatewayUser updateFee(@Context SecurityContext context, GatewayUser gu){
		return setFee(context, gu);
	}
	
	@GET
	@Path("/balance")
	@RolesAllowed({"gateway"})
	public WithdrawRequest getBalance(@Context SecurityContext context){
		String cn = null;
		try{
			LdapName ln = new LdapName(context.getUserPrincipal().getName());
			for(Rdn rdn : ln.getRdns()) {
			    if(rdn.getType().equalsIgnoreCase("CN")) {
			    	cn = (String) rdn.getValue();
			    }
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		Element e = cache.get("balance"+cn);
		Element e2 = cache.get("balanceReq"+cn);
		if (null!=e && !e.isExpired()){
			return new WithdrawRequest().setBalance((BigDecimal)e.getObjectValue());
		}
		if (null==e2 || e2.isExpired()){
			DataSet data = new DataSet()
				.setAction(Action.GW_BALANCE)
				.setCn(cn);
			nonTxFactory.getClient(data.getAction()+"-"+cn).executeCommand(data);
			cache.put(new Element("balanceReq"+cn, true));
		}
		throw new WebApplicationException("cache miss, requested, ask again later.", Response.Status.ACCEPTED);
	}
	
	@POST
	@Path("/balance")
	@RolesAllowed({"gateway"})
	public WithdrawRequest withdraw(
			@Context SecurityContext context,
			WithdrawRequest withdrawalRequest){
		String cn = null;
		try{
			LdapName ln = new LdapName(context.getUserPrincipal().getName());
			for(Rdn rdn : ln.getRdns()) {
			    if(rdn.getType().equalsIgnoreCase("CN")) {
			    	cn = (String) rdn.getValue();
			    }
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		Element e = cache.get("balance"+cn);
		BigDecimal newBal = null;
		if (null!=e){
			BigDecimal bd = (BigDecimal)e.getObjectValue();
			newBal = bd.subtract(withdrawalRequest.getAmount());
		}
		if (newBal==null || newBal.compareTo(BigDecimal.ZERO)<0){
			throw new WebApplicationException("balance unknown or to low", Response.Status.BAD_REQUEST);
		}
		try{
			mailClient.send(
				"Withdrawal request", 
				"admin@37coins.com",
				MessagingServletConfig.senderMail, 
				"user "+ cn + " wants to withdraw " + withdrawalRequest.getAmount() + " to "+ withdrawalRequest.getAddress(),
				"<html><head></head><body>user "+ cn + " wants to withdraw " + withdrawalRequest.getAmount() + " to "+ withdrawalRequest.getAddress()+"</body></html>");
		}catch(Exception ex){
			ex.printStackTrace();
			throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return new WithdrawRequest().setBalance(newBal);
	}
	
}