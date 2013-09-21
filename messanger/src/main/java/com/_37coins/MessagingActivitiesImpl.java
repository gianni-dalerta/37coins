package com._37coins;

import java.net.URLEncoder;
import java.util.Set;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.activities.MessagingActivities;
import com._37coins.envaya.QueueClient;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.persistence.dto.Transaction;
import com._37coins.sendMail.MailTransporter;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.Withdrawal;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContext;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.annotations.ManualActivityCompletion;
import com.google.inject.Inject;

public class MessagingActivitiesImpl implements MessagingActivities {
	ActivityExecutionContextProvider contextProvider = new ActivityExecutionContextProviderImpl();
	
	@Inject
	MailTransporter mt;
	
	@Inject
	QueueClient qc;
	
	@Inject
	GenericRepository dao;

	@Override
	public void sendMessage(DataSet rsp) {
		try {
			if (rsp.getTo().getAddressType() == MsgType.EMAIL){
				mt.sendMessage(rsp);
			}else{
				String runId = contextProvider.getActivityExecutionContext().getWorkflowExecution().getRunId();
				String taskId = contextProvider.getActivityExecutionContext().getTask().getActivityId();
				qc.send(rsp,MessagingServletConfig.queueUri, rsp.getTo().getGateway(),"amq.direct",runId+"::"+taskId);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@ManualActivityCompletion
	public void sendConfirmation(DataSet rsp, String workflowId) {
		ActivityExecutionContext executionContext = contextProvider.getActivityExecutionContext();
		String taskToken = executionContext.getTaskToken();
		try{
			RNQuery q = new RNQuery().addFilter("key", workflowId);
			Transaction tt = dao.queryEntity(q, Transaction.class);
			tt.setTaskToken(taskToken);
			dao.flush();
			String confLink = MessagingServletConfig.basePath + "/rest/withdrawal/approve?key="+URLEncoder.encode(tt.getKey(),"UTF-8");
			Withdrawal w = (Withdrawal)rsp.getPayload();
			w.setConfKey(tt.getKey());
			w.setConfLink(confLink);
			sendMessage(rsp);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			dao.closePersistenceManager();
		}
	}


	@Override
	public DataSet readMessageAddress(DataSet data) {
		try{
			Account a = dao.getObjectById(data.getAccountId(), Account.class);
			MsgAddress ma = pickMsgAddress(a.getMsgAddresses());
			MessageAddress to =  new MessageAddress()
				.setAddress(ma.getAddress())
				.setAddressType(ma.getType())
				.setGateway(ma.getGateway().getAddress());
			return data.setTo(to)
				.setLocale(ma.getLocale())
				.setService("37coins");
		}finally{
			dao.closePersistenceManager();
		}
	}

	public MsgAddress pickMsgAddress(Set<MsgAddress> list){
		//TODO: get a strategy here
		return list.iterator().next();
	}


}
