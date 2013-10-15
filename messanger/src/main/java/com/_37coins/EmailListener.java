package com._37coins;

import javax.mail.Message;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import org.restnucleus.dao.GenericRepository;

import com._37coins.parse.CommandParser;
import com._37coins.sendMail.MailTransporter;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.google.inject.Inject;

public class EmailListener implements MessageCountListener{
	
	@Inject 
	WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;
	
	@Inject 
	NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	
	@Inject
	MailTransporter mt;
	
	@Inject
	CommandParser mp;
	
	@Inject
	AmazonSimpleWorkflow swfService;
	
	@Inject
	GenericRepository dao;

	@Override
	public void messagesRemoved(MessageCountEvent e) {
	}

	@Override
	public void messagesAdded(MessageCountEvent e) {
		
		for (Message m : e.getMessages()) {
			try{
			//parse from
			String from = null;
			if (null == m.getFrom() || m.getFrom().length != 1) {
				DataSet rsp = new DataSet();
				mt.sendMessage(rsp);
				return;
			} else {
				from = ((InternetAddress) m.getFrom()[0]).getAddress();
			}
			throw new RuntimeException("not implemented");
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

}
