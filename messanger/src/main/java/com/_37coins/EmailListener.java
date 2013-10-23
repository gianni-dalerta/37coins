package com._37coins;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import com._37coins.parse.ParserAction;
import com._37coins.parse.ParserClient;
import com._37coins.persistence.dto.Transaction;
import com._37coins.sendMail.MailTransporter;
import com._37coins.workflow.NonTxWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.WithdrawalWorkflowClientExternalFactoryImpl;
import com._37coins.workflow.pojo.DataSet;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory;
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl;
import com.google.inject.Inject;

import freemarker.template.TemplateException;

public class EmailListener implements MessageCountListener {

	@Inject
	WithdrawalWorkflowClientExternalFactoryImpl withdrawalFactory;

	@Inject
	NonTxWorkflowClientExternalFactoryImpl nonTxFactory;

	@Inject
	MailTransporter mt;
	
	@Inject
	ParserClient parserClient;
	
	@Inject
	Cache cache;

	@Inject
	AmazonSimpleWorkflow swfService;

	@Override
	public void messagesRemoved(MessageCountEvent e) {
	}

	@Override
	public void messagesAdded(MessageCountEvent e) {

		for (Message m : e.getMessages()) {
				// parse from
			String from = null;
			try{
				if (null == m.getFrom() || m.getFrom().length != 1) {
					throw new MessagingException("could not parse from field");
				}
				from = ((InternetAddress) m.getFrom()[0]).getAddress();
				parserClient.start(from, MessagingServletConfig.imapUser, m.getSubject(), MessagingServletConfig.localPort,
				new ParserAction() {
					@Override
					public void handleWithdrawal(DataSet data) {
						//save the transaction id to db
						Transaction t = new Transaction().setKey(Transaction.generateKey()).setState(Transaction.State.STARTED);
						cache.put(new Element(t.getKey(), t));
						withdrawalFactory.getClient(t.getKey()).executeCommand(data);
					}
					@Override
					public void handleResponse(DataSet data) {
						try {
							mt.sendMessage(data);
						} catch (IOException | TemplateException | MessagingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					@Override
					public void handleDeposit(DataSet data) {
						nonTxFactory.getClient(data.getAction()+"-"+data.getCn()).executeCommand(data);
					}
					
					@Override
					public void handleConfirm(DataSet data) {
						Element e = cache.get(data.getPayload());
						Transaction tx = (Transaction)e.getObjectValue();
				        ManualActivityCompletionClientFactory manualCompletionClientFactory = new ManualActivityCompletionClientFactoryImpl(swfService);
				        ManualActivityCompletionClient manualCompletionClient = manualCompletionClientFactory.getClient(tx.getTaskToken());
				        manualCompletionClient.complete(null);
					}
				});

			} catch (MessagingException ex) {
				ex.printStackTrace();
			}
		}
	}

}
