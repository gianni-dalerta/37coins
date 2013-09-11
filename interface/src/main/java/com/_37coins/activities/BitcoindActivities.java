package com._37coins.activities;

import java.math.BigDecimal;
import java.util.List;

import com._37coins.bcJsonRpc.pojo.Transaction;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@Activities
public interface BitcoindActivities {
	
    @Activity(name = "SendTransaction", version = "0.3")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    String sendTransaction(BigDecimal amount, BigDecimal fee, Long fromId, Long toId, String toAddress);
    
    @Activity(name = "getAccountBalance", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    BigDecimal getAccountBalance(Long accountId);    
    
    @Activity(name = "getNewAddress", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    String getNewAddress(Long accountId);
    
    @Activity(name = "getAccount", version = "0.2")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    Long getAccount(String bcAddress);
    
    @Activity(name = "getAccountTransactions", version = "0.1")
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 30, defaultTaskStartToCloseTimeoutSeconds = 30)
    List<Transaction> getAccountTransactions(Long accountId);
    
}
