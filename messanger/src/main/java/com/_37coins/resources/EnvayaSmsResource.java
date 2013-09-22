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

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.google.inject.Injector;
import com.google.inject.name.Named;

@Path(EnvayaSmsResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class EnvayaSmsResource {
	public static Logger log = LoggerFactory.getLogger(EnvayaSmsResource.class);
	public final static String PATH = "/envayasms";

	private final MessageParser mp;

	private final QueueClient qc;
	
	private final NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	
	private final WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;
	
	private final AmazonSimpleWorkflow swfService;
	
	private final GenericRepository dao;
	
	@Inject public EnvayaSmsResource(ServletRequest request,
			MessageParser mp,
			QueueClient qc,
			Injector i,
			NonTxWorkflowClientExternalFactoryImpl nonTxFactory,
			WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory, @Named("wfClient")
			AmazonSimpleWorkflow swfService) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		dao = (GenericRepository)httpReq.getAttribute("gr");
		this.mp =mp;
		this.qc = qc;
		this.nonTxFactory = nonTxFactory;
		this.withdrawalFactory = withdrawalFactory;
		this.swfService = swfService;
	}
	
	private boolean isValid(MultivaluedMap<String,String> params, String sig, String url){
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
			if (dao!=null){
				dao.closePersistenceManager();
			}
		}
	}
	
	
	@POST
	@Path("/{address}/sms/")
	public Map<String, Object> receive(MultivaluedMap<String, String> params,
			@HeaderParam("X-Request-Signature") String sig,
			@Context UriInfo uriInfo){
		if (!isValid(params, sig, uriInfo.getRequestUri().toString())){
			throw new WebApplicationException("signature missmatch",
					javax.ws.rs.core.Response.Status.FORBIDDEN);
		}
		Map<String, Object> rv = new HashMap<>();
		switch (params.getFirst("action")) {
		case "status":
			log.info("id " + params.get("id"));
			log.info("status " + params.get("status"));
			log.info("error " + params.get("error"));
			break;
		case "amqp_started":
			log.info("consumerTag " + params.get("consumerTag"));
			break;
		case "incoming":
			if (params.getFirst("messageType").equalsIgnoreCase("sms")) {
				MessageAddress md=null;
				try {
					md = MessageAddress.fromString(params.getFirst("from"), params.getFirst("phoneNumber")).setGateway(params.getFirst("phoneNumber"));
				} catch (Exception e1) {
					e1.printStackTrace();
					throw new WebApplicationException(e1);
				}
				
				//implement actions
				RequestInterpreter ri = new RequestInterpreter(mp, dao, swfService) {							
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
				ri.process(md, params.getFirst("message"));
			}
			break;
		default:
			throw new WebApplicationException("not implemented",
					javax.ws.rs.core.Response.Status.NOT_FOUND);
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
		System.out.println(value);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(value.getBytes("utf-8"));

        return new String(Base64.encodeBase64(md.digest()));     
	}

}
