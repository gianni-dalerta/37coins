package com._37coins;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;


import com._37coins.activities.MailActivities;
import com._37coins.envaya.QueueClient;
import com._37coins.sendMail.MailTransporter;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContext;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.annotations.ManualActivityCompletion;
import com.google.inject.Inject;

public class MailActivitiesImpl implements MailActivities {
	ActivityExecutionContextProvider contextProvider = new ActivityExecutionContextProviderImpl();
	
	@Inject
	MailTransporter mt;
	
	@Inject
	QueueClient qc;

	@Override
	public void sendMail(Map<String,Object> rsp) {
		try {
			if (((String)rsp.get("source")).equalsIgnoreCase("email")){
				mt.sendMessage(rsp);
			}else{
				qc.send(rsp,MailServletConfig.queueUri, (String)rsp.get("gateway"),"amq.direct");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@ManualActivityCompletion
	public String requestWithdrawalConfirm(Map<String, Object> cmd) {
		try {
			cmd.put("confLink", getConfLink());
			mt.sendMessage(cmd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		  
		return null;
	}

	@Override
	@ManualActivityCompletion
	public String requestWithdrawalReview(Map<String, Object> cmd) {
		
		return null;
	}
	
	public String getConfLink() throws UnsupportedEncodingException{
		ActivityExecutionContext executionContext = contextProvider.getActivityExecutionContext();
		String taskToken = executionContext.getTaskToken();
		String confLink = null;
		confLink = MailServletConfig.basePath + "/rest/withdrawal/approve?taskToken="+URLEncoder.encode(taskToken,"UTF-8");
		return confLink;
	}

	@Override
	@ManualActivityCompletion
	public void sendConfirmation(Map<String, Object> rsp) {
		try {
			rsp.put("confLink", getConfLink());
			mt.sendMessage(rsp);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
