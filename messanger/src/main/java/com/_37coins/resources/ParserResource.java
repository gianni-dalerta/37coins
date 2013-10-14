package com._37coins.resources;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import com._37coins.BasicAccessAuthFilter;
import com._37coins.MessagingServletConfig;
import com._37coins.persistence.dto.Transaction;
import com._37coins.persistence.dto.Transaction.State;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

@Path(ParserResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ParserResource {
	public final static String PATH = "/parser";
	
	final private AmazonSimpleWorkflow swfService;
	final private List<DataSet> responseList;
	final private InitialLdapContext ctx;
	final private Cache cache;
	
	@SuppressWarnings("unchecked")
	@Inject public ParserResource(Cache cache,
			ServletRequest request, 
			AmazonSimpleWorkflow swfService,
			InitialLdapContext ctx) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		responseList = (List<DataSet>)httpReq.getAttribute("dsl");
		this.swfService = swfService;
		this.cache = cache;
		this.ctx = ctx;
	}
	
	@POST
	@Path("/Balance")
	public List<DataSet> balance(){
		return responseList;
	}
	@POST
	@Path("/Transactions")
	public List<DataSet> transactions(){
		return responseList;
	}
	@POST
	@Path("/DepositReq")
	public List<DataSet> depositReq(){
		return responseList;
	}
	@POST
	@Path("/Help")
	public List<DataSet> help(){
		return responseList;
	}
	
	@POST
	@Path("/WithdrawalReq")
	public List<DataSet> withdrawalReq(){
		DataSet data = responseList.get(0);
		Withdrawal w = (Withdrawal)data.getPayload();
		if (null!= w.getMsgDest() && w.getMsgDest().getAddress()!=null){
			
			String cn = null;
			String gwDn = null;
			String gwMobile = null;
			try{
				Attributes atts = BasicAccessAuthFilter.searchUnique("(&(objectClass=person)(mobile="+w.getMsgDest().getAddress()+"))", ctx).getAttributes();
				cn = (atts.get("cn")!=null)?(String)atts.get("cn").get():null;
				gwDn = (atts.get("manager")!=null)?(String)atts.get("manager").get():null;
			}catch(NameNotFoundException e){
				if (w.getMsgDest().getAddressType()==MsgType.SMS){//create a new user
					//set gateway from referring user's gateway
					if (data.getTo().getAddressType() == MsgType.SMS 
							&& w.getMsgDest().getPhoneNumber().getCountryCode() == data.getTo().getPhoneNumber().getCountryCode()){
						gwDn = data.getGwDn();
						gwMobile = data.getTo().getGateway();
					}else{//or try to find a gateway in the database
						try{
							ctx.setRequestControls(null);
							SearchControls searchControls = new SearchControls();
							searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
							searchControls.setTimeLimit(1000);
							NamingEnumeration<?> namingEnum = ctx.search("ou=gateways,"+MessagingServletConfig.ldapBaseDn, "(&(objectClass=person)(mobile="+w.getMsgDest().getPhoneNumber().getCountryCode()+"*))", searchControls);
							if (namingEnum.hasMore ()){
								Attributes attributes = ((SearchResult) namingEnum.next()).getAttributes();
								gwMobile = (attributes.get("mobile")!=null)?(String)attributes.get("mobile").get():null;
								gwDn = "cn="+cn+",ou=gateways,"+MessagingServletConfig.ldapBaseDn;
								namingEnum.close();
							}else{
								throw new RuntimeException("no gateway available for this user");
							}
						}catch (NamingException e1){
							
						}
					}
					if (null!=gwDn){
						//create new user
						Attributes attributes=new BasicAttributes();
						Attribute objectClass=new BasicAttribute("objectClass");
						objectClass.add("inetOrgPerson");
						attributes.put(objectClass);
						Attribute sn=new BasicAttribute("sn");
						Attribute cnAtr=new BasicAttribute("cn");
						String cnString = w.getMsgDest().getAddress().replace("+", "");
						sn.add(cnString);
						cnAtr.add(cnString);
						attributes.put(sn);
						attributes.put(cnAtr);
						attributes.put("manager", gwDn);
						attributes.put((w.getMsgDest().getAddressType()==MsgType.SMS)?"mobile":"mail", w.getMsgDest().getAddress());
						attributes.put("preferredLocale", data.getLocaleString());
						try {
							ctx.createSubcontext("cn="+cnString+",ou=accounts,"+MessagingServletConfig.ldapBaseDn, attributes);
							//and say hi to new user
							DataSet create = new DataSet()
								.setAction(Action.SIGNUP)
								.setTo(new MessageAddress()
									.setAddress(w.getMsgDest().getAddressObject())
									.setAddressType(w.getMsgDest().getAddressType())
									.setGateway(PhoneNumberUtil.getInstance().format(w.getMsgDest().getPhoneNumber(),PhoneNumberFormat.E164)))
								.setCn(cnString)
								.setLocale(data.getLocale())
								.setService(data.getService());
							responseList.add(create);
						} catch (NamingException e1) {
							e1.printStackTrace();
							throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
						}
					}
				}else if (w.getMsgDest().getAddressType()==MsgType.EMAIL){
					//how to set the email gateway?
					throw new RuntimeException("not implemented");
				}else{
					throw new RuntimeException("not implemented");
				}				
			} catch (Exception e) {
				e.printStackTrace();
				throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
			}
			if (cn!=null){
				//set our payment destination
				if (null == w.getPayDest()){
					w.setPayDest(new PaymentAddress());
				}
				w.getPayDest()
					.setAddress(cn)
					.setAddressType(PaymentType.ACCOUNT);
				w.getMsgDest()
					.setGateway(gwMobile);
			}
		}
		//set the fee
		w.setFee(data.getGwFee());
		w.setFeeAccount(data.getGwDn());
		//check that transaction amount is > fee 
		//(otherwise tx history gets screwed up)
		if (w.getAmount().compareTo(w.getFee())<=0){
			data.setAction(Action.BELOW_FEE);
			w.setAmount(w.getFee().add(new BigDecimal("0.00001").setScale(8)));
			return responseList;
		}
		//save the transaction id to db
		Transaction t = new Transaction().setKey(Transaction.generateKey()).setState(State.STARTED);
		cache.put(new Element(t.getKey(), t));
		data.setTxKey(t.getKey());
		return responseList;
	}

	@POST
	@Path("/WithdrawalReqOther")
	public List<DataSet> withdrawalReqOther(){
		return withdrawalReq();
	}
	
	@POST
	@Path("/WithdrawalConf")
	public void withdrawalConf(){
		Element e = cache.get(responseList.get(0).getPayload());
		Transaction tx = (Transaction)e.getObjectValue();
        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());
        manualCompletionClient.complete(null);
	}
	
	

}
