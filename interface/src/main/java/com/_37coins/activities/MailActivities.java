package com._37coins.activities;

import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@Activities
public interface MailActivities {
	
    @Activity(name = "SendMail", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10)
    void sendMail(Map<String,Object> rsp);
    
    @Activity(name = "SendConfirmation", version = "0.3")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 3600)	
	void sendConfirmation(Map<String,Object> rsp);
    
    @Activity(name = "RequestWithdrawalConfirm", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10000)	
    String requestWithdrawalConfirm(Map<String,Object> cmd);
    
    @Activity(name = "RequestWithdrawalReview", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10000)	
    String requestWithdrawalReview(Map<String,Object> cmd);
    
}
