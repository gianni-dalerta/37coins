package com._37coins.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com._37coins.MessageProcessor;
import com._37coins.MessageProcessor.Action;
import com._37coins.workflow.DepositWorkflowClientExternal;
import com._37coins.workflow.WithdrawalWorkflowClientExternal;
import com.google.inject.Inject;
import com.google.inject.Injector;
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
	protected MessageProcessor mp;
	
	@Inject
	Injector i;

	@SuppressWarnings("rawtypes")
	@POST
	@ApiOperation(value = "aprove withdrawal", notes = "")
    @ApiErrors(value = { @ApiError(code = 500, reason = "Internal Server Error.")})
	public Map<String,Object> receive(
			@HeaderParam("X-Request-Signature") String signature,
			@FormParam("test") String test,
			@FormParam("version") int version,
			@FormParam("phone_number") String phoneNumber,
			@FormParam("log") String logMsg,
			@FormParam("network") String network,
			@FormParam("settings_version") int settingsVersion,
			@FormParam("now") long now,
			@FormParam("battery") int battery,
			@FormParam("power") int power,
			@FormParam("action") String action,
			//incoming
			@FormParam("from") String from,
			@FormParam("message_type") String messageType,
			@FormParam("message") String message,
			@FormParam("timestamp") long timestamp,
			//send_status
			@FormParam("id") String id,
			@FormParam("status") String status,
			@FormParam("error") String error,
			//amqp_started
			@FormParam("consumer_tag") String consumerTag) {
		Map<String,Object> rv = new HashMap<>();
		if (action.equalsIgnoreCase("incoming") 
				&& messageType.equalsIgnoreCase("sms")){
			Map<String, Object> o = mp.process(from, message);
			o.put("source","sms");
			if (null!=o.get("action")){
				o.put("service","37coins");
				o.put("gateway", phoneNumber);
				switch (Action.fromString((String)o.get("action"))) {
				case CREATE:
				case BALANCE:
				case DEPOSIT:
					o.put("action", o.get("action"));
					i.getInstance(DepositWorkflowClientExternal.class)
							.executeCommand(o);
					break;
				case SEND_CONFIRM:
				case SEND:
					o.put("action", o.get("action"));
					if (test!=null){
						i.getInstance(
							WithdrawalWorkflowClientExternal.class)
							.executeCommand(o);
					}
					break;
				}
			}else{
				System.out.println("could not parse message: " + message);
			}
		}else if (action.equalsIgnoreCase("send_status")){
			System.out.println("id " + id);
			System.out.println("status " + status);
			System.out.println("error " + error);
		}else if (action.equalsIgnoreCase("amqp_started")){
			System.out.println("consumerTag " + consumerTag);
		}
			rv.put("events", new ArrayList());
		return rv;
	}
	
}
