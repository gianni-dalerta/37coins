package com._37coins.activities;

import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@Activities
public interface CoreActivities {

	@Activity(name = "findAccountByMsgAddress", version = "0.1")
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10)
    public Map<String,Object> findAccountByMsgAddress(Map<String,Object> data);

	@Activity(name = "createDbAccount", version = "0.1")
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10)
	public Map<String, Object> createDbAccount(Map<String, Object> data);
	
	@Activity(name = "readAccount", version = "0.1")
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10)
	public Map<String, Object> readAccount(Map<String, Object> data);

}
