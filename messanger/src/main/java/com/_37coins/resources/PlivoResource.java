package com._37coins.resources;

import java.util.List;
import java.util.Map.Entry;

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

import com._37coins.MessagingServletConfig;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Transaction;
import com._37coins.persistence.dto.Transaction.State;
import com._37coins.plivo.GetDigits;
import com._37coins.plivo.Redirect;
import com._37coins.plivo.Response;
import com._37coins.plivo.Speak;
import com._37coins.plivo.Wait;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;

@Path(PlivoResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PlivoResource {
	public final static String PATH = "/plivo";
	
	private final GenericRepository dao;
	
	private final AmazonSimpleWorkflow swfService;
	
	@Inject public PlivoResource(
			ServletRequest request,
			AmazonSimpleWorkflow swfService) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		dao = (GenericRepository)httpReq.getAttribute("gr");
		this.swfService = swfService;
	}
	
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/{accountId}/{workflowId}/answer")
	public Response check(
			@PathParam("accountId") String accountId, 
			@PathParam("workflowId") String workflowId){
		Response rv = null;
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
		if (a.getPin()!=null){
			//only check pin
			rv = new Response()
				.add(new Speak().setText("Welcome to 37 coins!"))
				.add(new GetDigits()
					.setAction(MessagingServletConfig.basePath+"/plivo/"+accountId+"/"+workflowId+"/confirm")
					.setNumDigits(5)
					.setRedirect(true)
					.setSpeak(new Speak()
						.setText("Please enter your 4-digit pin number, followed by the hash key.")));
		}else{
			//create a new pin
			rv = new Response()
				.add(new Speak().setText("Welcome to 37 coins! To secure your transactions, this call will set up a 4 digit pin number."))
				.add(new Wait())
				.add(new GetDigits()
					.setAction(MessagingServletConfig.basePath+ "/plivo/"+accountId+"/"+workflowId+"/create")
					.setNumDigits(5)
					.setRedirect(false)
					.setSpeak(new Speak()
						.setText("Please choose and enter a 4-digit pin number, followed by the hash key.")))
				.add(new GetDigits()
					.setAction(MessagingServletConfig.basePath+ "/plivo/"+accountId+"/"+workflowId+"/confirm")
					.setNumDigits(5)
					.setRedirect(true)
					.setSpeak(new Speak()
						.setText("Ok! Please repeat your 4-digit pin, followed by the hash key.")));
		}
		return rv;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/{accountId}/{workflowId}/hangup")
	public void hangup(
			MultivaluedMap<String, String> params,
			@PathParam("accountId") String accountId, 
			@PathParam("workflowId") String workflowId){
		System.out.println("hangup:");
		System.out.println("accountId: " + accountId);
		System.out.println("workflowId: " + workflowId);
		for (Entry<String, List<String>> e: params.entrySet()){
			System.out.println(e.getKey() + ": "+e.getValue());
		}
		RNQuery q = new RNQuery().addFilter("key", workflowId);
		Transaction tx = dao.queryEntity(q, Transaction.class);
		if (tx.getState() == State.STARTED){
			System.out.println("pin never received");
			ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
	        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());
	        manualCompletionClient.complete(false);
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/{accountId}/{workflowId}/create")
	public void create(
			@PathParam("accountId") String accountId, 
			@PathParam("workflowId") String workflowId,
			@FormParam("Digits") String digits){
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
		a.setPin(Integer.parseInt(digits));
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Path("/{accountId}/{workflowId}/confirm")
	public Response confirm(
			@PathParam("accountId") String accountId, 
			@PathParam("workflowId") String workflowId,
			@FormParam("Digits") String digits){
		Account a = dao.getObjectById(Long.parseLong(accountId), Account.class);
        Response rv =null;
		if (null!=digits && null!=a.getPin() && Integer.parseInt(digits) == a.getPin()){
			System.out.println("pin match");
			RNQuery q = new RNQuery().addFilter("key", workflowId);
			Transaction tx = dao.queryEntity(q, Transaction.class);
			tx.setState(State.CONFIRMED);
		    ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
		    ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());		 
			manualCompletionClient.complete(true);
			rv = new Response().add(new Speak().setText("Please Remember your pin for future transactions."));
		}else{
			System.out.println("pins don't match");
			a.setPin(null);
			rv = new Response()
				.add(new Speak().setText("Pins don't match, please try again."))
				.add(new Redirect().setText(MessagingServletConfig.basePath+ "/plivo/"+accountId+"/"+workflowId+"/answer"));
		}
		return rv;
	}
}
