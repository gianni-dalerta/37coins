package com._37coins.activities;

import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@Activities
public interface BitcoindActivities {
	
    @Activity(name = "SendTransaction", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Map<String,Object> sendTransaction(Map<String,Object> rsp);
    
    @Activity(name = "getAccountBalance", version = "0.1")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Map<String,Object> getAccountBalance(Map<String,Object> rsp);    
    
    @Activity(name = "createBcAccount", version = "0.1")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Map<String,Object> createBcAccount(Map<String,Object> rsp);
    
    @Activity(name = "getNewAddress", version = "0.1")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Map<String,Object> getNewAddress(Map<String,Object> rsp);
    
    @Activity(name = "getAccount", version = "0.1")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Map<String,Object> getAccount(Map<String,Object> rsp);
    
}
