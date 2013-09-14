package com._37coins.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.restnucleus.log.Log;
import org.slf4j.Logger;

import com._37coins.MessagingServletConfig;
import com._37coins.envaya.QueueClient;
import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Response;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;

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

	@SuppressWarnings("rawtypes")
	@POST
	@ApiOperation(value = "aprove withdrawal", notes = "")
	@ApiErrors(value = { @ApiError(code = 500, reason = "Internal Server Error.") })
	public Map<String, Object> receive(
			@HeaderParam("X-Request-Signature") String signature,
			@FormParam("version") int version,
			@FormParam("phone_number") String phoneNumber,
			@FormParam("log") String logMsg,
			@FormParam("network") String network,
			@FormParam("settings_version") int settingsVersion,
			@FormParam("now") long now,
			@FormParam("battery") int battery,
			@FormParam("power") int power,
			@FormParam("action") String action,
			// incoming
			@FormParam("from") String from,
			@FormParam("message_type") String messageType,
			@FormParam("message") String message,
			@FormParam("timestamp") long timestamp,
			// send_status
			@FormParam("id") String id, @FormParam("status") String status,
			@FormParam("error") String error,
			// amqp_started
			@FormParam("consumer_tag") String consumerTag) {
		Map<String, Object> rv = new HashMap<>();
		switch (action) {
		case "status":
			log.info("id " + id);
			log.info("status " + status);
			log.info("error " + error);
			break;
		case "amqp_started":
			log.info("consumerTag " + consumerTag);
			break;
		case "incoming":
			if (messageType.equalsIgnoreCase("sms")) {
				MessageAddress md = new MessageAddress()
					.setAddress(from)
					.setAddressType(MsgType.SMS)
					.setGateway(phoneNumber);
				
				//implement actions
				RequestInterpreter ri = new RequestInterpreter(mp, swfService) {							
					@Override
					public void startWithdrawal(Request req) {
						withdrawalFactory.getClient().executeCommand(req);
					}
					@Override
					public void startDeposit(Request req) {
						nonTxFactory.getClient().executeCommand(req);
					}
					@Override
					public void respond(Response rsp) {
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
				ri.process(md, message);
			}
			break;
		default:
			throw new WebApplicationException("not implemented",
					javax.ws.rs.core.Response.Status.NOT_FOUND);
		}
		rv.put("events", new ArrayList());
		return rv;
	}

}
