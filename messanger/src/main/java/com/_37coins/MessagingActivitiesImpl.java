package com._37coins;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com._37coins.activities.MessagingActivities;
import com._37coins.envaya.QueueClient;
import com._37coins.sendMail.MailTransporter;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.Response;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContext;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.annotations.ManualActivityCompletion;
import com.google.inject.Inject;

public class MessagingActivitiesImpl implements MessagingActivities {
	ActivityExecutionContextProvider contextProvider = new ActivityExecutionContextProviderImpl();
	
	@Inject
	MailTransporter mt;
	
	@Inject
	QueueClient qc;

	@Override
	public void sendMessage(Response rsp) {
		try {
			if (rsp.getTo().getAddressType() == MsgType.EMAIL){
				mt.sendMessage(rsp);
			}else{
				String runId = contextProvider.getActivityExecutionContext().getWorkflowExecution().getRunId();
				String taskId = contextProvider.getActivityExecutionContext().getTask().getActivityId();
				qc.send(rsp,MessagingServletConfig.queueUri, rsp.getTo().getGateway(),"amq.direct",runId+"::"+taskId);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	public String getConfLink() throws UnsupportedEncodingException{
		ActivityExecutionContext executionContext = contextProvider.getActivityExecutionContext();
		String taskToken = executionContext.getTaskToken();
		String confLink = null;
		confLink = MessagingServletConfig.basePath + "/rest/withdrawal/approve?taskToken="+URLEncoder.encode(taskToken,"UTF-8");
		return confLink;
	}

	@Override
	@ManualActivityCompletion
	public void sendConfirmation(Response rsp) {
		try {
			rsp.setPayload(getConfLink());
			mt.sendMessage(rsp);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}




}
