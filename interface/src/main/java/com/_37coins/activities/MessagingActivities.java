package com._37coins.activities;

import com._37coins.workflow.pojo.DataSet;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@Activities
public interface MessagingActivities {
	
    @Activity(name = "SendMessage", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10)
    void sendMessage(DataSet rsp);
    
    @Activity(name = "SendConfirmation", version = "0.5")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 3600)	
	void sendConfirmation(DataSet rsp, String workflowId);

    @Activity(name = "ReadMessageAddress", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 10)
    DataSet readMessageAddress(DataSet data);
    
}
