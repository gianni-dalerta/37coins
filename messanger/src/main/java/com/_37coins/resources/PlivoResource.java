package com._37coins.resources;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.MessageFactory;
import com._37coins.MessagingServletConfig;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Transaction;
import com._37coins.persistence.dto.Transaction.State;
import com._37coins.plivo.GetDigits;
import com._37coins.plivo.Redirect;
import com._37coins.plivo.Response;
import com._37coins.plivo.Speak;
import com._37coins.plivo.Wait;
import com._37coins.web.GatewayUser;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;

import freemarker.template.TemplateException;

@Path(PlivoResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PlivoResource {
	public final static String PATH = "/plivo";
	
	private final GenericRepository dao;
	
	private final AmazonSimpleWorkflow swfService;
	
	private final MessageFactory msgFactory;
	
	@Inject public PlivoResource(
			ServletRequest request,
			AmazonSimpleWorkflow swfService,
			MessageFactory msgFactory) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		dao = (GenericRepository)httpReq.getAttribute("gr");
		this.swfService = swfService;
		this.msgFactory = msgFactory;
	}
	
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/answer/{accountId}/{workflowId}/{locale}")
	public Response answer(
			@PathParam("accountId") String accountId,
			@PathParam("workflowId") String workflowId,
			@PathParam("locale") String locale){
		Response rv = null;
		DataSet ds = new DataSet().setLocale(new Locale(locale));
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
		if (a.getPin()!=null){
			//only check pin
			try {
				rv = new Response()
					.add(new Speak().setText(msgFactory.getText("VoiceHello",ds)))
					.add(new GetDigits()
						.setAction(MessagingServletConfig.basePath+"/plivo/check/"+accountId+"/"+workflowId+"/"+locale)
						.setNumDigits(5)
						.setRedirect(true)
						.setSpeak(new Speak()
							.setText(msgFactory.getText("VoiceEnter",ds))));
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}
		}else{
			//create a new pin
			try {
				rv = new Response()
					.add(new Speak().setText(msgFactory.getText("VoiceHello",ds)+ msgFactory.getText("VoiceSetup",ds)))
					.add(new Wait())
					.add(new GetDigits()
						.setAction(MessagingServletConfig.basePath+ "/plivo/create/"+accountId)
						.setNumDigits(5)
						.setRedirect(false)
						.setSpeak(new Speak()
							.setText(msgFactory.getText("VoiceCreate",ds))))
					.add(new GetDigits()
						.setAction(MessagingServletConfig.basePath+ "/plivo/confirm/"+accountId+"/"+workflowId+"/"+locale)
						.setNumDigits(5)
						.setRedirect(true)
						.setSpeak(new Speak()
							.setText(msgFactory.getText("VoiceConfirm",ds))));
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}
		}
		return rv;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/hangup/{workflowId}")
	public void hangup(
			MultivaluedMap<String, String> params,
			@PathParam("workflowId") String workflowId){
		RNQuery q = new RNQuery().addFilter("key", workflowId);
		Transaction tx = dao.queryEntity(q, Transaction.class);
		if (tx.getState() == State.STARTED){
			ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
	        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());
	        manualCompletionClient.complete(Action.TX_CANCELED);
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/create/{accountId}")
	public void create(
			@PathParam("accountId") String accountId, 
			@FormParam("Digits") String digits){
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
		a.setPin(Integer.parseInt(digits));
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/check/{accountId}/{workflowId}/{locale}")
	public Response check(
			@PathParam("accountId") String accountId,
			@PathParam("workflowId") String workflowId,
			@PathParam("locale") String locale,
			@FormParam("Digits") String digits){
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
		DataSet ds = new DataSet().setLocale(new Locale(locale));
        Response rv =null;
		if (null!=digits && null!=a.getPin() && Integer.parseInt(digits) == a.getPin()){
			RNQuery q = new RNQuery().addFilter("key", workflowId);
			Transaction tx = dao.queryEntity(q, Transaction.class);
			tx.setState(State.CONFIRMED);
		    ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
		    ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());		 
			manualCompletionClient.complete(Action.WITHDRAWAL_REQ);
			try {
				rv = new Response().add(new Speak().setText(msgFactory.getText("VoiceMatch",ds)));
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}
		}else{
			int wrongCount = a.getPinWrongCount();
			a.setPinWrongCount(wrongCount+1);
			String callText;
			try {
				callText = msgFactory.getText("VoiceFail",new DataSet().setPayload(new Integer(3-wrongCount)));
				rv = new Response()
				.add(new Speak().setText(callText))
				.add(new Redirect().setText(MessagingServletConfig.basePath+ "/plivo/answer/"+accountId+"/"+workflowId+"/"+locale));
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}
		}
		return rv;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/confirm/{accountId}/{workflowId}/{locale}")
	public Response confirm(
			@PathParam("accountId") String accountId, 
			@PathParam("locale") String locale,
			@PathParam("workflowId") String workflowId,
			@FormParam("Digits") String digits){
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
		DataSet ds = new DataSet().setLocale(new Locale(locale));
        Response rv =null;
		if (null!=digits && null!=a.getPin() && Integer.parseInt(digits) == a.getPin()){
			RNQuery q = new RNQuery().addFilter("key", workflowId);
			Transaction tx = dao.queryEntity(q, Transaction.class);
			tx.setState(State.CONFIRMED);
		    ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
		    ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());		 
			manualCompletionClient.complete(Action.WITHDRAWAL_REQ);
			try {
				rv = new Response().add(new Speak().setText(msgFactory.getText("VoiceSuccess",ds)));
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}
		}else{
			a.setPin(null);
			try {
				rv = new Response()
					.add(new Speak().setText(msgFactory.getText("VoiceMisMatch",ds)))
					.add(new Redirect().setText(MessagingServletConfig.basePath+ "/plivo/answer/"+accountId+"/"+workflowId+"/"+locale));
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}
		}
		return rv;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/register/{code}/{locale}")
	public Response register(
			@PathParam("code") String code,
			@PathParam("locale") String locale){
		Response rv = null;
		String spokenCode = "";
		for (char c : code.toCharArray()){
			spokenCode+=c+", ";
		}
		DataSet ds = new DataSet()
			.setLocale(locale)
			.setPayload(spokenCode);
		try {	
			String text = msgFactory.getText("VoiceRegister",ds);
			rv = new Response().add(new Speak()
				.setText(text)
				.setLanguage(ds.getLocaleString()));
		} catch (IOException | TemplateException e) {
			e.printStackTrace();
		}
		return rv;
	}
}
