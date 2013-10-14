package com._37coins.resources;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MessagingServletConfig;
import com._37coins.envaya.QueueClient;
import com._37coins.parse.CommandParser;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;

@Path(EnvayaSmsResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class EnvayaSmsResource {
	public static Logger log = LoggerFactory.getLogger(EnvayaSmsResource.class);
	public final static String PATH = "/envayasms";

	private final CommandParser commandParser;

	private final QueueClient qc;
	
	private final NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	
	private final WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;
	
	final private InitialLdapContext ctx;
	
	@Inject public EnvayaSmsResource(ServletRequest request,
			CommandParser commandParser,
			QueueClient qc,
			Injector i,
			NonTxWorkflowClientExternalFactoryImpl nonTxFactory,
			WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		ctx = (InitialLdapContext)httpReq.getAttribute("ctx");
		this.commandParser =commandParser;
		this.qc = qc;
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
			String dn = "cn="+cn+","+MessagingServletConfig.ldapBaseDn;
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
					//TODO: execute this in a new thread
					Action action = commandParser.processCommand(params.getFirst("message"));
					Locale locale = commandParser.guessLocale(params.getFirst("message"));
					CloseableHttpClient httpclient = HttpClients.createDefault();
					HttpPost req = new HttpPost("http://localhost/parse/"+action.getText());
					List <NameValuePair> nvps = new ArrayList <NameValuePair>();
					nvps.add(new BasicNameValuePair("from", params.getFirst("from")));
					nvps.add(new BasicNameValuePair("gateway", params.getFirst("phone_number")));
					nvps.add(new BasicNameValuePair("message", params.getFirst("message")));
					req.addHeader("Accept-Language", locale.toString().replace("_", "-"));
					req.setEntity(new UrlEncodedFormEntity(nvps));
					CloseableHttpResponse rsp = httpclient.execute(req);
					DataSet result = new ObjectMapper().readValue(rsp.getEntity().getContent(),DataSet.class);
					String next = "";
					switch(next){
					case "withdrawal":
						String workflowId = "???";
						if (workflowId.equalsIgnoreCase("???"))
							throw new RuntimeException("implement workflowId passing");
						withdrawalFactory.getClient(workflowId).executeCommand(result);
						break;
					case "deposit":
						nonTxFactory.getClient(result.getAction()+"-"+result.getCn()).executeCommand(result);
						break;
					case "respond":
						qc.send(result, MessagingServletConfig.queueUri,
								(String) result.getTo().getGateway(), "amq.direct",
								"SmsResource" + System.currentTimeMillis());
					}
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
