package com._37coins;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.activities.MessagingActivities;
import com._37coins.envaya.QueueClient;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.sendMail.MailTransporter;
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

	@Override
	public void sendMail(Map<String,Object> rsp) {
		try {
			if (((String)rsp.get("source")).equalsIgnoreCase("email")){
				mt.sendMessage(rsp);
			}else{
				String runId = contextProvider.getActivityExecutionContext().getWorkflowExecution().getRunId();
				String taskId = contextProvider.getActivityExecutionContext().getTask().getActivityId();
				qc.send(rsp,MessagingServletConfig.queueUri, (String)rsp.get("gateway"),"amq.direct",runId+"::"+taskId);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public void notifyMoveReceiver(Map<String, Object> data) {
		data.put("action", "received");
		String receiver = null;
		if (data.get("receiverEmail")!=null){
			receiver = (String)data.get("receiverEmail");
			data.put("source", "email");
		}else{
			receiver = (String)data.get("receiverPhone");
			data.put("source", "sms");
		}
		data.put("msgAddress", receiver);
		try {
			if (((String)data.get("source")).equalsIgnoreCase("email")){
				mt.sendMessage(data);
			}else{
				String runId = contextProvider.getActivityExecutionContext().getWorkflowExecution().getRunId();
				String taskId = contextProvider.getActivityExecutionContext().getTask().getActivityId();
				qc.send(data,MessagingServletConfig.queueUri, (String)data.get("gateway"),"amq.direct",runId+"::"+taskId);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@ManualActivityCompletion
	public String requestWithdrawalConfirm(Map<String, Object> cmd) {
		try {
			cmd.put("confLink", getConfLink());
			mt.sendMessage(cmd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		  
		return null;
	}

	@Override
	@ManualActivityCompletion
	public String requestWithdrawalReview(Map<String, Object> cmd) {
		
		return null;
	}
	
	public String getConfLink() throws UnsupportedEncodingException{
		ActivityExecutionContext executionContext = contextProvider.getActivityExecutionContext();
		String taskToken = executionContext.getTaskToken();
		String confLink = null;
		confLink = MessagingServletConfig.basePath + "/rest/withdrawal/approve?taskToken="+URLEncoder.encode(taskToken,"UTF-8");
		return confLink;
	}

	@Override
	@ManualActivityCompletion
	public void sendConfirmation(Map<String, Object> rsp) {
		try {
			rsp.put("confLink", getConfLink());
			mt.sendMessage(rsp);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public String findAccountByMsgAddress(String msgAddress) {
		GenericRepository dao = new GenericRepository();
		RNQuery q = new RNQuery().addFilter("address", msgAddress);
		MsgAddress ma = dao.queryEntity(q, MsgAddress.class,false);
		if (null==ma){
			dao.closePersistenceManager();
			throw new RuntimeException("account not found");
		}
		Long id =  ma.getOwner().getId();
		dao.closePersistenceManager();
		return id.toString();
	}
	
	@Override
	public String createDbAccount(String msgAddress) {
		GenericRepository dao = new GenericRepository();
		MsgAddress ma = new MsgAddress()
			.setAddress((String)data.get("msgAddress"))
			.setOwner(new Account());
		dao.add(ma);
		dao.flush();
		Long id =  ma.getOwner().getId();
		dao.closePersistenceManager();
		return id.toString();
	}
	
	public String getOrCreateAccount(String msgAddress){
		GenericRepository dao = new GenericRepository();
		RNQuery q = new RNQuery().addFilter("address", msgAddress);
		MsgAddress ma = dao.queryEntity(q, MsgAddress.class,false);
		if (null==ma){
			ma = new MsgAddress()
				.setAddress(msgAddress)
				.setOwner(new Account());
			dao.add(ma);
			dao.flush();
		}
		Long id =  ma.getOwner().getId();
		dao.closePersistenceManager();
		return id.toString();
	}
	
	public String addMsgAddresstoAccount(String msgAddress, String newMsgAddress) {
		GenericRepository dao = new GenericRepository();
		RNQuery q = new RNQuery().addFilter("address", msgAddress);
		MsgAddress ma = dao.queryEntity(q, MsgAddress.class,false);
		if (null==ma){
			dao.closePersistenceManager();
			throw new RuntimeException("account not found");
		}
		ma.getOwner().addMsgAddress(newMsgAddress);
		Long id =  ma.getOwner().getId();
		dao.closePersistenceManager();
		return id.toString();
	}
	
	@Override
	public List<String> readAccount(String accId) {
		GenericRepository dao = new GenericRepository();
		Account account = dao.getObjectById(Long.parseLong(accId), Account.class);
		List<String> rv = new ArrayList<>();
		for (MsgAddress a :account.getMsgAddresses()){
			rv.add(a.getAddress());
		}
		dao.closePersistenceManager();
		return rv;
	}

	@Override
	public Map<String, Object> findReceiverAccount(Map<String, Object> data) {
		String address = (String)data.get("receiverPhone");
		address = (address==null)?(String)data.get("receiverEmail"):address;
		if (address!=null){
			GenericRepository dao = new GenericRepository();
			RNQuery q = new RNQuery().addFilter("address", address);
			MsgAddress ma = dao.queryEntity(q, MsgAddress.class,false);
			if (null==ma){
				dao.closePersistenceManager();
				throw new RuntimeException("account not found");
			}
			data.put("receiverAccount", ma.getOwner().getId().toString());
			dao.closePersistenceManager();
		}else{
			throw new RuntimeException("Parameter missing");
		}
		return data;
	}


}
