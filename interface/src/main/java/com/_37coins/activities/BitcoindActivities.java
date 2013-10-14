package com._37coins.activities;

import java.math.BigDecimal;
import java.util.List;

import com._37coins.bcJsonRpc.pojo.Transaction;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@Activities
public interface BitcoindActivities {
	
    @Activity(name = "SendTransaction", version = "0.6")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    String sendTransaction(BigDecimal amount, BigDecimal fee, String fromCn, String toCn, String toAddress, String workflowId, String comment);
    
    @Activity(name = "getAccountBalance", version = "0.3")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    BigDecimal getAccountBalance(String cn);    
    
    @Activity(name = "getNewAddress", version = "0.3")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    String getNewAddress(String cn);
    
    @Activity(name = "getAccount", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Long getAccount(String bcAddress);
    
    @Activity(name = "getAccountTransactions", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    List<Transaction> getAccountTransactions(String cn);

    @Activity(name = "getTransactionVolume", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
	BigDecimal getTransactionVolume(String cn, int hours);
    
}
