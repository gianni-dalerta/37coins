package com._37coins.resources;

import java.io.UnsupportedEncodingException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;

@Api(value = WithdrawalResource.PATH, description = "a resource to test database access.")
@Path(WithdrawalResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class WithdrawalResource {
	public final static String PATH = "/withdrawal";
	public final static String HTML_RESPONSE_DONE = "<html><head><title>Confirmation</title></head><body>Your withrawal request has been confirmed.</body></html>";

	@Inject @Named("wfClient")
	protected AmazonSimpleWorkflow swfService;

	@GET
	@Path("/approve")
	@ApiOperation(value = "aprove withdrawal", notes = "")
    @ApiErrors(value = { @ApiError(code = 500, reason = "Internal Server Error.")})
	public Representation aprove(@QueryParam("taskToken") String taskToken) throws UnsupportedEncodingException{
        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(taskToken);
        manualCompletionClient.complete(null);
		return new StringRepresentation(HTML_RESPONSE_DONE,
				org.restlet.data.MediaType.TEXT_HTML);
	}
	
	@GET
	@Path("/deny")
	@ApiOperation(value = "deny withdrawal", notes = "")
    @ApiErrors(value = { @ApiError(code = 500, reason = "Internal Server Error.")})
	public Representation deny(@QueryParam("taskToken") String taskToken){
        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(taskToken);
        manualCompletionClient.fail(new Throwable("denied by user or admin"));
		return new StringRepresentation(HTML_RESPONSE_DONE,
				org.restlet.data.MediaType.TEXT_HTML);
	}
}
