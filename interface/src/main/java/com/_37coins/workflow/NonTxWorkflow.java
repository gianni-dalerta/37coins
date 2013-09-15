package com._37coins.workflow;

import com._37coins.workflow.pojo.DataSet;
import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 60)
public interface NonTxWorkflow {
	
    @Execute(version = "0.3")
    void executeCommand(DataSet data);

}
