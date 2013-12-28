package com._37coins.resources;

import java.io.IOException;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.BasicAccessAuthFilter;
import com._37coins.MessageFactory;
import com._37coins.MessagingServletConfig;
import com._37coins.sendMail.MailServiceClient;
import com._37coins.web.AccountPolicy;
import com._37coins.web.AccountRequest;
import com._37coins.web.PasswordRequest;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;

import freemarker.template.TemplateException;

@Path(AccountResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {
	public final static String PATH = "/account";
	public static Logger log = LoggerFactory.getLogger(AccountResource.class);
	
	private final Cache cache;
	
	final private InitialLdapContext ctx;
	
	private final HttpServletRequest httpReq;
	
	private final AccountPolicy accountPolicy;
	
	private final MailServiceClient mailClient;
	
	private final MessageFactory msgFactory;

	@Inject
	public AccountResource(Cache cache,
			ServletRequest request,
			AccountPolicy accountPolicy,
			MailServiceClient mailClient,
			MessageFactory msgFactory){
		this.cache = cache;
		httpReq = (HttpServletRequest)request;
		this.ctx = (InitialLdapContext)httpReq.getAttribute("ctx");
		this.accountPolicy = accountPolicy;
		this.mailClient = mailClient;
		this.msgFactory = msgFactory;
	}
	
	
	private String getRemoteAddress(){
		String addr = httpReq.getHeader("X-Forwarded-For");
		if (null==addr || addr.length()<7){
			addr = httpReq.getRemoteAddr();
		}
		return addr;
	}
	
	/**
	 * allow front-end to notify user about taken account 
	 * @param email
	 */
	@GET
	@Path("/check")
	public void checkEmail(@QueryParam("email") String email){
		//check it's a valid email
		if (!AccountPolicy.isValidEmail(email)){
			throw new WebApplicationException("email not valid", Response.Status.BAD_REQUEST);
		}
		//how to avoid account fishing?
		Element e = cache.get(getRemoteAddress());
		if (e!=null){
			if (e.getHitCount()>50){
				throw new WebApplicationException("to many requests", Response.Status.FORBIDDEN);
			}
		}
		//check it's not taken already
		try {
			BasicAccessAuthFilter.searchUnique("(&(objectClass=person)(mail="+email+"))", ctx).getAttributes();
		} catch (IllegalStateException | NamingException e1) {
			throw new WebApplicationException("email used", Response.Status.CONFLICT);
		}
	}
	
	/**
	 * an account-request is validated, and cached, then email is send
	 * @param accountRequest
	 */
	@POST
	public void register(AccountRequest accountRequest){
		// no ticket, no service
		if (null==cache.get("ticket"+accountRequest.getTicket())){
			log.debug("ticket required for this operation.");
			throw new WebApplicationException("ticket required for this operation.", Response.Status.BAD_REQUEST);
		}
		//#############validate email#################
		//check regex
		if (null==accountRequest.getEmail() || !AccountPolicy.isValidEmail(accountRequest.getEmail())){
			log.debug("send a valid email plz :D");
			throw new WebApplicationException("send a valid email plz :D", Response.Status.EXPECTATION_FAILED);
		}
		if (accountPolicy.isEmailMxLookup()){
			//check db for active email with same domain
			String hostName = accountRequest.getEmail().substring(accountRequest.getEmail().indexOf("@") + 1, accountRequest.getEmail().length());
			try{
				ctx.setRequestControls(null);
				SearchControls searchControls = new SearchControls();
				searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
				searchControls.setTimeLimit(1000);
				NamingEnumeration<?> namingEnum = ctx.search(MessagingServletConfig.ldapBaseDn, "(&(objectClass=person)(email=*@"+hostName+"))", searchControls);
				if (!namingEnum.hasMore()){
					//check host mx record
					boolean isValidMX = false;
					try{
						isValidMX = AccountPolicy.isValidMX(accountRequest.getEmail());
					}catch(Exception e){
						System.out.println("EmailRes.->check: "+ accountRequest.getEmail() + " not valid due: " + e.getMessage());
					}
					if(!isValidMX ){
						throw new WebApplicationException("This email's hostname does not have mx record.", Response.Status.EXPECTATION_FAILED);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		//################validate password############
		boolean isValid = accountPolicy.validatePassword(accountRequest.getPassword());
		if (!isValid){
			log.debug("password does not pass account policy");
			throw new WebApplicationException("password does not pass account policy", Response.Status.BAD_REQUEST);
		}
		//put it into cache, and wait for email validation
		String token = RandomStringUtils.random(14, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
		try {
			sendCreateEmail(accountRequest.getEmail() ,token);
		} catch (MessagingException | IOException| TemplateException e1) {
			e1.printStackTrace();
			throw new WebApplicationException(e1,Response.Status.INTERNAL_SERVER_ERROR);
		}
		AccountRequest ar = new AccountRequest()
			.setEmail(accountRequest.getEmail())
			.setPassword(accountRequest.getPassword());
		cache.put(new Element("create"+token,ar));
	}
	
	/**
	 * an account-request is taken from cache and put into the database
	 * @param accountRequest
	 */
	@POST
	@Path("/create")
	public void createAccount(AccountRequest accountRequest){
		Element e = cache.get("create"+accountRequest.getToken());
		if (null!=e){
			accountRequest = (AccountRequest)e.getObjectValue();
			//build a new user and same
			Attributes attributes=new BasicAttributes();
			Attribute objectClass=new BasicAttribute("objectClass");
			objectClass.add("inetOrgPerson");
			attributes.put(objectClass);
			Attribute sn=new BasicAttribute("sn");
			Attribute cn=new BasicAttribute("cn");
			String cnString = RandomStringUtils.random(16, "ABCDEFGHJKLMNPQRSTUVWXYZ123456789");
			sn.add(cnString);
			cn.add(cnString);
			attributes.put(sn);
			attributes.put(cn);
			attributes.put("mail", accountRequest.getEmail());
			attributes.put("userPassword", accountRequest.getPassword());
			try{
				ctx.createSubcontext("cn="+cnString+",ou=gateways,"+MessagingServletConfig.ldapBaseDn, attributes);
			}catch(NamingException ex){
				throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
			}
			cache.remove("create"+accountRequest.getToken());
		}else{
			throw new WebApplicationException("not found or expired", Response.Status.NOT_FOUND);
		}
	}
	
	/**
	 * a password-request is validated, cached and email send out
	 * @param pwRequest
	 */
	@POST
	@Path("/password/request")
	public void requestPwReset(PasswordRequest pwRequest){
		// no ticket, no service
		Element e = cache.get("ticket"+pwRequest.getTicket());
		if (null==e){
			throw new WebApplicationException("ticket required for this request.", Response.Status.BAD_REQUEST);
		}else{
			if (e.getHitCount()>3){
				cache.remove("ticket"+pwRequest.getTicket());
				throw new WebApplicationException("to many requests", Response.Status.BAD_REQUEST);
			}
		}
		//fetch account by email, then send email
		String dn = null;
		try {
			Attributes atts = BasicAccessAuthFilter.searchUnique("(&(objectClass=person)(mail="+pwRequest.getEmail()+"))", ctx).getAttributes();
			dn = "cn="+atts.get("cn").get()+",ou=gateways,"+MessagingServletConfig.ldapBaseDn;
		} catch (IllegalStateException | NamingException e1) {
			e1.printStackTrace();
			throw new WebApplicationException("account not found", Response.Status.NOT_FOUND);
		}
		String token = RandomStringUtils.random(14, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
		try {
			sendResetEmail(pwRequest.getEmail(), token);
		} catch (MessagingException | IOException| TemplateException e1) {
			e1.printStackTrace();
			throw new WebApplicationException(e1,Response.Status.INTERNAL_SERVER_ERROR);
		}
		PasswordRequest pwr = new PasswordRequest().setToken(token).setDn(dn);
		cache.put(new Element("reset"+token, pwr));
	}
	
	/**
	 * a password-request is taken from the cache and executed, then account-changes persisted
	 * @param pwRequest
	 */
	@POST
	@Path("/password/reset")
	public void reset(PasswordRequest pwRequest){
		Element e = cache.get("reset"+pwRequest.getToken());
		if (null!=e){
			String newPw = pwRequest.getPassword();
			boolean isValid = accountPolicy.validatePassword(newPw);
			if (!isValid)
				throw new WebApplicationException("password does not pass account policy", Response.Status.BAD_REQUEST);
			pwRequest = (PasswordRequest)e.getObjectValue();
			
			Attributes toModify = new BasicAttributes();
			toModify.put("userPassword", newPw);
			try{
				ctx.modifyAttributes(pwRequest.getDn(), DirContext.REPLACE_ATTRIBUTE, toModify);
			}catch(Exception ex){
				ex.printStackTrace();
				throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
			}

			cache.remove("reset"+pwRequest.getToken());
		}else{
			throw new WebApplicationException("not found or expired", Response.Status.NOT_FOUND);
		}
	}
	
	private void sendResetEmail(String email, String token) throws AddressException, MessagingException, IOException, TemplateException{
		DataSet ds = new DataSet()
			.setLocale(httpReq.getLocale())
			.setAction(Action.RESET)
			.setPayload(MessagingServletConfig.basePath+"#confReset/"+token);
		mailClient.send(
			msgFactory.constructSubject(ds), 
			email,
			MessagingServletConfig.senderMail, 
			msgFactory.constructTxt(ds),
			msgFactory.constructHtml(ds));
	}
	
	private void sendCreateEmail(String email, String token) throws AddressException, MessagingException, IOException, TemplateException{
		DataSet ds = new DataSet()
			.setLocale(httpReq.getLocale())
			.setAction(Action.REGISTER)
			.setPayload(MessagingServletConfig.basePath+"#confSignup/"+token);
		mailClient.send(
			msgFactory.constructSubject(ds), 
			email,
			MessagingServletConfig.senderMail, 
			msgFactory.constructTxt(ds),
			msgFactory.constructHtml(ds));
	}
	
}
