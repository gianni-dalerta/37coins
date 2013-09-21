package com._37coins;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import org.restnucleus.dao.GenericRepository;

import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.sendMail.MailTransporter;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.MessageAddress;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import freemarker.template.TemplateException;

public class EmailListener implements MessageCountListener{
	
	@Inject 
	WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;
	
	@Inject 
	NonTxWorkflowClientExternalFactoryImpl nonTxFactory;
	
	@Inject
	MailTransporter mt;
	
	@Inject
	MessageParser mp;
	
	@Inject @Named("wfClient")
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
			String gw = MessagingServletConfig.imapUser+"@"+MessagingServletConfig.imapHost;
			MessageAddress md = MessageAddress.fromString(from, gw).setGateway(gw);
			
			//implement actions
			RequestInterpreter ri = new RequestInterpreter(mp,dao,swfService) {							
				@Override
				public void startWithdrawal(DataSet data, String workflowId) {
					withdrawalFactory.getClient(workflowId).executeCommand(data);
				}
				@Override
				public void startDeposit(DataSet data) {
					nonTxFactory.getClient(data.getAction()+"-"+data.getAccountId()).executeCommand(data);
				}
				@Override
				public void respond(DataSet rsp) {
					try {
						mt.sendMessage(rsp);
					} catch (IOException | TemplateException
							| MessagingException e) {
						e.printStackTrace();
					}
				}
			};

			//interprete received message/command
			ri.process(md, m.getSubject());
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

}
