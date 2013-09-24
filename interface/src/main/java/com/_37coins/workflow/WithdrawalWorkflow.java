package com._37coins.workflow;

import com._37coins.workflow.pojo.DataSet;
import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 3700)
public interface WithdrawalWorkflow {
	
    @Execute(version = "0.5")
    void executeCommand(DataSet data);

}
