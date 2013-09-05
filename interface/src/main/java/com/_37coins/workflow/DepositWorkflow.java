package com._37coins.workflow;

import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 60)
public interface DepositWorkflow {
	
    @Execute(version = "0.1")
    void executeCommand(Map<String, Object> data);

}
