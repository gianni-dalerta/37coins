package com._37coins.sendMail;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MessageFactory;
import com._37coins.MessagingServletConfig;
import com._37coins.workflow.pojo.DataSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import freemarker.template.TemplateException;

/**
 * Constructs and sends Email to SES
 * 
 * @author Johann Barbie
 * 
 */
public class MailTransporter {

	final MailServiceClient client;
	final MessageFactory emailFactory;
	public static Logger log = LoggerFactory.getLogger(MailTransporter.class);

	@Inject
	public MailTransporter(MessageFactory emailFactory,MailServiceClient client) {
		this.client = client;
		this.emailFactory = emailFactory;
	}

	public void sendMessage(DataSet rsp) throws IOException,
			TemplateException, AddressException, MessagingException {
		log.debug("To send message with following data: "
				+ new ObjectMapper().writeValueAsString(rsp));
			
		client.send(emailFactory.constructSubject(rsp), rsp.getTo().getAddress(),
				MessagingServletConfig.senderMail,
				emailFactory.constructTxt(rsp),
				emailFactory.constructHtml(rsp));
	}

}
