package com._37coins;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import com._37coins.parse.MessageParser;
import com._37coins.parse.RequestInterpreter;
import com._37coins.sendMail.MailTransporter;
import com._37coins.workflow.NonTxWorkflowClientExternal;
import com._37coins.workflow.WithdrawalWorkflowClientExternal;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com._37coins.workflow.pojo.Request;
import com._37coins.workflow.pojo.Response;
import com.google.inject.Inject;

import freemarker.template.TemplateException;

public class EmailListener implements MessageCountListener{
	
	@Inject 
	WithdrawalWorkflowClientExternal withdrawalClient;
	
	@Inject 
	NonTxWorkflowClientExternal nonTxClient;
	
	@Inject
	MailTransporter mt;
	
	@Inject
	MessageParser mp;

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
				Response rsp = new Response();
				mt.sendMessage(rsp);
				return;
			} else {
				from = ((InternetAddress) m.getFrom()[0]).getAddress();
			}
			MessageAddress md = new MessageAddress()
			.setAddress(from)
			.setAddressType(MsgType.EMAIL)
			.setGateway(MessagingServletConfig.imapUser+"@"+MessagingServletConfig.imapHost);
			
			//implement actions
			RequestInterpreter ri = new RequestInterpreter(mp) {							
				@Override
				public void startWithdrawal(Request req) {
					withdrawalClient.executeCommand(req);
				}
				@Override
				public void startDeposit(Request req) {
					nonTxClient.executeCommand(req);
				}
				@Override
				public void respond(Response rsp) {
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
