package com._37coins.sendMail;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;


import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MailServletConfig;
import com._37coins.crypto.Sha1Hex;
import com._37coins.persistence.dto.MailAddress;
import com._37coins.persistence.dto.SendJournal;
import com._37coins.pojo.SendAction;
import com._37coins.pojo.ServiceEntry;
import com._37coins.pojo.ServiceList;
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
	final EmailFactory emailFactory;
	final SendAction sendAction;
	final List<ServiceEntry> categories;
	public static Logger log = LoggerFactory.getLogger(MailTransporter.class);

	@Inject
	public MailTransporter(EmailFactory emailFactory,
			List<ServiceEntry> categories, SendAction sendAction,
			MailServiceClient client) {
		this.client = client;
		this.emailFactory = emailFactory;
		this.sendAction = sendAction;
		this.categories = categories;
	}

	public void sendMessage(Map<String, Object> mr) throws IOException,
			TemplateException, AddressException, MessagingException {
		log.debug("To send message with following data: "
				+ new ObjectMapper().writeValueAsString(mr));
		GenericRepository dao = new GenericRepository();
		MailAddress ma = dao.queryEntity(
				new RNQuery().addFilter("address", (String) mr.get("msgAddress")),
				MailAddress.class, false);
		// create a record for the email
		if (null == ma) {
			ma = MailAddress.prepareNewMail((String) mr.get("msgAddress"),
					new ServiceList((String) mr.get("service"), categories));
			dao.add(ma);
		}
		// check email to be send (template) categories
		String category = sendAction.getCategory((String) mr.get("action"));
		if (!ma.containsCategory(category)) {
			// receiver has not subscribed this category
			log.debug("receiver " + mr.get("msgAddress")
					+ " has not subscribed this category " + category);
			return;
		}
		// parse locale
		mr.put("locale", new Locale((String) mr.get("locale")));
		// prepare send journal
		try {
			String sendHash = new Sha1Hex().makeSHA1Hash(new Date().toString() + mr.get("msgAddress")+ category);
			SendJournal sj = new SendJournal().setDestination(ma).setHash(
					sendHash);
			dao.add(sj);
			mr.put("sendHash", sendHash);
		} catch (NoSuchAlgorithmException e) {
			log.error("sendMessage:", e);
		}

		client.send(emailFactory.constructSubject(mr, sendAction), (String) mr.get("msgAddress"),
				MailServletConfig.senderMail,
				emailFactory.constructTxt(mr, sendAction),
				emailFactory.constructHtml(mr, sendAction));
		dao.closePersistenceManager();
	}

}
