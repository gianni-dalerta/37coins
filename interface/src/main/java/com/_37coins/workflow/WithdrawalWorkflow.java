package com._37coins.workflow;

import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 3700)
public interface WithdrawalWorkflow {
	
    @Execute(version = "0.2")
    void executeCommand(Map<String,Object> data);

}
