package com._37coins.resources;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MessagingServletConfig;
import com._37coins.envaya.QueueClient;
import com._37coins.parse.ParserAction;
import com._37coins.parse.ParserClient;
import com._37coins.persistence.dto.Transaction;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;
import com.google.inject.Injector;

import freemarker.template.TemplateException;

@Path(EnvayaSmsResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class EnvayaSmsResource {
	public final static String PATH = "/envayasms";
	public static Logger log = LoggerFactory.getLogger(EnvayaSmsResource.class);

	private final QueueClient qc;
	
	private final NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	
	private final WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;
	
	private final AmazonSimpleWorkflow swfService;
	
	private final InitialLdapContext ctx;
	
	private final ParserClient parserClient;
	
	private final Cache cache;
	
	@Inject public EnvayaSmsResource(ServletRequest request,
			QueueClient qc,
			Injector i,
			ParserClient parserClient,
			Cache cache,
			NonTxWorkflowClientExternalFactoryImpl nonTxFactory,
			WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory,
			AmazonSimpleWorkflow swfService) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		ctx = (InitialLdapContext)httpReq.getAttribute("ctx");
		this.qc = qc;
		this.cache = cache;
		this.swfService = swfService;
		this.parserClient = parserClient;
		this.nonTxFactory = nonTxFactory;
		this.withdrawalFactory = withdrawalFactory;
	}
	
	@POST
	@Path("/{cn}/sms/")
	public Map<String, Object> receive(MultivaluedMap<String, String> params,
			@HeaderParam("X-Request-Signature") String sig,
			@PathParam("cn") String cn,
			@Context UriInfo uriInfo){
		Map<String, Object> rv = new HashMap<>();
		try{
			String dn = "cn="+cn+",ou=gateways,"+MessagingServletConfig.ldapBaseDn;
			Attributes atts = ctx.getAttributes(dn,new String[]{"departmentNumber"});
			String envayaToken = (atts.get("departmentNumber")!=null)?(String)atts.get("departmentNumber").get():null;
			String calcSig = calculateSignature(uriInfo.getRequestUri().toString(), params, envayaToken);
			if (!sig.equals(calcSig)){
				throw new WebApplicationException("signature missmatch",
						javax.ws.rs.core.Response.Status.UNAUTHORIZED);
			}
			switch (params.getFirst("action")) {
				case "status":
					log.info("id " + params.get("id"));
					log.info("status " + params.get("status"));
					log.info("error " + params.get("error"));
					break;
				case "amqp_started":
					log.info("consumerTag " + params.get("consumer_tag"));
					break;
				case "incoming":
					if (params.getFirst("message_type").equalsIgnoreCase("sms")) {
						parserClient.start(params.getFirst("from"), params.getFirst("phone_number"), params.getFirst("message"), 
						new ParserAction() {
							@Override
							public void handleWithdrawal(DataSet data) {
								//save the transaction id to db
								Transaction t = new Transaction().setKey(Transaction.generateKey()).setState(Transaction.State.STARTED);
								cache.put(new Element(t.getKey(), t));
								withdrawalFactory.getClient(t.getKey()).executeCommand(data);
							}
							@Override
							public void handleResponse(DataSet data) {
								try {
									qc.send(data, MessagingServletConfig.queueUri,
											(String) data.getTo().getGateway(), "amq.direct",
											"SmsResource" + System.currentTimeMillis());
								} catch (KeyManagementException
										| NoSuchAlgorithmException
										| IOException | TemplateException
										| URISyntaxException e) {
									e.printStackTrace();
								}
							}
							
							@Override
							public void handleDeposit(DataSet data) {
								nonTxFactory.getClient(data.getAction()+"-"+data.getCn()).executeCommand(data);
							}
							
							@Override
							public void handleConfirm(DataSet data) {
								Element e = cache.get(data.getPayload());
								Transaction tx = (Transaction)e.getObjectValue();
						        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
						        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());
						        manualCompletionClient.complete(null);
							}
						});
						
					}
				break;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		rv.put("events", new ArrayList<String>());
		return rv;
	}
	
	public static String calculateSignature(String url, MultivaluedMap<String,String> paramMap, String pw) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		if (null==url||null==paramMap||null==pw){
			return null;
		}
		List<String> params = new ArrayList<>();
		for (Entry<String,List<String>> m :paramMap.entrySet()){
			if (m.getValue().size()>0){
				params.add(m.getKey()+"="+m.getValue().get(0));
			}
		}
		Collections.sort(params);
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		for (String s : params){
			sb.append(",");
			sb.append(s);
		}
		sb.append(",");
		sb.append(pw);
		String value = sb.toString();

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(value.getBytes("utf-8"));

        return new String(Base64.encodeBase64(md.digest()));     
	}

}
