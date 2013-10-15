package com._37coins.parse;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import javax.inject.Singleton;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com._37coins.BasicAccessAuthFilter;
import com._37coins.MessagingServletConfig;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.Withdrawal;
import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class InterpreterFilter implements Filter {

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		List<DataSet> responseList = (List<DataSet>)httpReq.getAttribute("dsl");
		DataSet responseData = responseList.get(0);
		InitialLdapContext ctx = (InitialLdapContext)httpReq.getAttribute("ctx");
		Withdrawal w = (responseData.getPayload() instanceof Withdrawal)?(Withdrawal)responseData.getPayload():null;
		if (responseData.getAction()==Action.WITHDRAWAL_REQ_OTHER){
			MessageAddress temp = w.getMsgDest();
			w.setMsgDest(responseData.getTo());
			responseData.setTo(temp);
		}
		//get user from directory
		try{
			//read the user
			Attributes atts = BasicAccessAuthFilter.searchUnique("(&(objectClass=person)(mobile="+responseData.getTo().getAddress()+"))", ctx).getAttributes();
			boolean pwLocked = (atts.get("pwdAccountLockedTime")!=null)?true:false;
			String locale = (atts.get("preferredLanguage")!=null)?(String)atts.get("preferredLanguage").get():null;
			String gwDn = (atts.get("manager")!=null)?(String)atts.get("manager").get():null;
			String cn = (atts.get("cn")!=null)?(String)atts.get("cn").get():null;
			//check if account is disabled
			if (pwLocked){
				responseData.setAction(Action.ACCOUNT_BLOCKED);
				respond(responseList,response);
				return;
			}
			responseData.setCn(cn);
				//read the gateway
			Attributes gwAtts = ctx.getAttributes(gwDn,new String[]{"mobile", "cn", "description"});
			BigDecimal gwFee = (gwAtts.get("description")!=null)?new BigDecimal((String)gwAtts.get("description").get()).setScale(8):null;
			String gwMobile = (gwAtts.get("mobile")!=null)?(String)gwAtts.get("mobile").get():null;
			String gwCn = (gwAtts.get("cn")!=null)?(String)gwAtts.get("cn").get():null;
			responseData.setGwFee(gwFee).getTo().setGateway(gwMobile);
			//if existing user, don't forgive wrong commands
			if (responseData.getAction()==null){
				responseData.setLocale(locale);//because we did not recognize the command, we also don't know the language
				responseData.setAction(Action.UNKNOWN_COMMAND);
				respond(responseList,response);
				return;
			}
			//update language if outdated in directory
			if (responseData.getLocale()!=new DataSet().setLocale(locale).getLocale()){
				Attributes a = new BasicAttributes();
				a.put("preferredLanguage", responseData.getLocaleString());
				ctx.modifyAttributes("cn="+cn+",ou=accounts,"+MessagingServletConfig.ldapBaseDn, DirContext.REPLACE_ATTRIBUTE, a);
			}
			responseData.setGwCn(gwCn);
		}catch (NameNotFoundException e){//new user
			try{
				//search the gateway from directory
				Attributes atts = BasicAccessAuthFilter.searchUnique("(&(objectClass=person)(mobile="+responseData.getTo().getGateway()+"))", ctx).getAttributes();
				String gwCn = (atts.get("cn")!=null)?(String)atts.get("cn").get():null;
				BigDecimal gwFee = (atts.get("description")!=null)?new BigDecimal((String)atts.get("description").get()).setScale(8):null;
				//build a new user and same
				Attributes attributes=new BasicAttributes();
				Attribute objectClass=new BasicAttribute("objectClass");
				objectClass.add("inetOrgPerson");
				attributes.put(objectClass);
				Attribute sn=new BasicAttribute("sn");
				Attribute cn=new BasicAttribute("cn");
				String cnString = responseData.getTo().getAddress().replace("+", "");
				sn.add(cnString);
				cn.add(cnString);
				attributes.put(sn);
				attributes.put(cn);
				attributes.put("manager", "cn="+gwCn+",ou=gateways,"+MessagingServletConfig.ldapBaseDn);
				attributes.put((responseData.getTo().getAddressType()==MsgType.SMS)?"mobile":"mail", responseData.getTo().getAddress());
				attributes.put("preferredLanguage", responseData.getLocaleString());
				ctx.createSubcontext("cn="+cnString+",ou=accounts,"+MessagingServletConfig.ldapBaseDn, attributes);
				responseData.setCn(cnString).setGwCn(gwCn).setGwFee(gwFee);
				//respond to new user with welcome message
				DataSet create = new DataSet()
					.setAction(Action.SIGNUP)
					.setTo(responseData.getTo())
					.setCn(responseData.getCn())
					.setLocale(responseData.getLocale())
					.setService(responseData.getService());
				responseList.add(create);
				httpReq.setAttribute("create", create);
				//nothing to do if this was the first message and had no meaning 
				if (responseData.getAction()==null){
					return;
				}
			}catch(NamingException e1){
				e1.printStackTrace();
				httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} catch (NamingException e) {
			e.printStackTrace();
			httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		chain.doFilter(request, response);
	}
	
	public void respond(List<DataSet> dsl, ServletResponse response){
		OutputStream os = null;
		try {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			os = httpResponse.getOutputStream();
			new ObjectMapper().writeValue(os, dsl);
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {if (null!=os)os.close();} catch (IOException e) {}
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
