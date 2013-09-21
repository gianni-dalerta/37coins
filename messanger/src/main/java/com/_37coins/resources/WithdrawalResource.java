package com._37coins.resources;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Transaction;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;
import com.google.inject.name.Named;

@Path(WithdrawalResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class WithdrawalResource {
	public final static String PATH = "/withdrawal";
	public final static String HTML_RESPONSE_DONE = "<html><head><title>Confirmation</title></head><body>Your withrawal request has been confirmed.</body></html>";

	@Inject @Named("wfClient")
	protected AmazonSimpleWorkflow swfService;
	
	@Inject
	GenericRepository dao;

	@GET
	@Path("/approve")
	public Response aprove(@QueryParam("key") String key) throws UnsupportedEncodingException{
		RNQuery q = new RNQuery().addFilter("key", key);
		Transaction tt = dao.queryEntity(q, Transaction.class);
        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tt.getTaskToken());
        manualCompletionClient.complete(null);
        dao.delete(tt.getId(), Transaction.class);
        return Response.ok(HTML_RESPONSE_DONE,MediaType.TEXT_HTML_TYPE).build();
	}
	
	@GET
	@Path("/deny")
	public Response deny(@QueryParam("taskToken") String key){
		RNQuery q = new RNQuery().addFilter("key", key);
		Transaction tt = dao.queryEntity(q, Transaction.class);
        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tt.getTaskToken());
        manualCompletionClient.fail(new Throwable("denied by user or admin"));
        dao.delete(tt.getId(), Transaction.class);
		return Response.ok(HTML_RESPONSE_DONE,MediaType.TEXT_HTML_TYPE).build();
	}
}
