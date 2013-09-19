package com._37coins.resources;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;
import org.restnucleus.log.Log;
import org.slf4j.Logger;

import com._37coins.MessagingServletConfig;
import com._37coins.envaya.QueueClient;
import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.persistence.dto.Gateway;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.MessageAddress;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.wordnik.swagger.annotations.Api;

@Api(value = EnvayaSmsResource.PATH, description = "a resource to receive sms messages.")
@Path(EnvayaSmsResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class EnvayaSmsResource {
	public final static String PATH = "/envayasms";

	@Inject
	MessageParser mp;

	@Inject
	QueueClient qc;

	@Inject
	Injector i;
	
	@Inject
	NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	
	@Inject
	WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;
	
	@Inject @Named("wfClient")
	AmazonSimpleWorkflow swfService;
	
	@Log
	Logger log;
	
	private boolean isValid(Map<String,String> params, String sig, String url){
		GenericRepository dao = new GenericRepository();
		try{
			//read query key
			String patternStr=".*"+EnvayaSmsResource.PATH+"/(.*)/";
			Pattern p = Pattern.compile(patternStr);
			Matcher m = p.matcher(url);
			m.find();
			String address = m.group(1);
			//read password
			
			RNQuery q = new RNQuery().addFilter("address", address);
			Gateway gw = dao.queryEntity(q, Gateway.class);
			String pw = gw.getPassword();
			//check signature
			String calcSig=null;
			calcSig = calculateSignature(url, params, pw);
			return sig.equals(calcSig);
		}catch(Exception e){
			return false;
		}finally{
			dao.closePersistenceManager();
		}
	}
	
	
	@POST
	@Path("/{address}/sms/")
	public Map<String, Object> receive(Representation rep,
			@HeaderParam("X-Request-Signature") String sig,
			@Context UriInfo uriInfo){
		Map<String,String> params = new Form(rep).getValuesMap();
		if (!isValid(params, sig, uriInfo.getRequestUri().toString())){
			throw new WebApplicationException("signature missmatch",
					javax.ws.rs.core.Response.Status.FORBIDDEN);
		}
		Map<String, Object> rv = new HashMap<>();
		switch (params.get("action")) {
		case "status":
			log.info("id " + params.get("id"));
			log.info("status " + params.get("status"));
			log.info("error " + params.get("error"));
			break;
		case "amqp_started":
			log.info("consumerTag " + params.get("consumerTag"));
			break;
		case "incoming":
			if (params.get("messageType").equalsIgnoreCase("sms")) {
				MessageAddress md=null;
				try {
					md = MessageAddress.fromString(params.get("from"), params.get("phoneNumber")).setGateway(params.get("phoneNumber"));
				} catch (Exception e1) {
					e1.printStackTrace();
					throw new WebApplicationException(e1);
				}
				
				//implement actions
				RequestInterpreter ri = new RequestInterpreter(mp, swfService) {							
					@Override
					public void startWithdrawal(DataSet data, String workflowId) {
						withdrawalFactory.getClient(workflowId).executeCommand(data);
					}
					@Override
					public void startDeposit(DataSet data) {
						nonTxFactory.getClient(data.getAction()+"-"+data.getAccountId()).executeCommand(data);
					}
					@Override
					public void respond(DataSet rsp) {
						try {
							qc.send(rsp, MessagingServletConfig.queueUri,
									(String) rsp.getTo().getGateway(), "amq.direct",
									"SmsResource" + System.currentTimeMillis());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};

				//interprete received message/command
				ri.process(md, params.get("message"));
			}
			break;
		default:
			throw new WebApplicationException("not implemented",
					javax.ws.rs.core.Response.Status.NOT_FOUND);
		}
		rv.put("events", new ArrayList<String>());
		return rv;
	}
	
	public static String calculateSignature(String url, Map<String,String> paramMap, String pw) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		if (null==url||null==paramMap||null==pw){
			return null;
		}
		List<String> params = new ArrayList<>();
		for (Entry<String,String> m :paramMap.entrySet()){
			params.add(m.getKey()+"="+m.getValue());
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
		System.out.println(value);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(value.getBytes("utf-8"));

        return new String(Base64.encodeBase64(md.digest()));     
	}

}
