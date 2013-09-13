package com._37coins.sendMail;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MessagingServletConfig;
import com._37coins.crypto.Sha1Hex;
import com._37coins.persistence.dto.MsgAddress;
import com._37coins.persistence.dto.SendJournal;
import com._37coins.pojo.SendAction;
import com._37coins.pojo.ServiceEntry;
import com._37coins.workflow.pojo.Response;
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

	public void sendMessage(Response rsp) throws IOException,
			TemplateException, AddressException, MessagingException {
		log.debug("To send message with following data: "
				+ new ObjectMapper().writeValueAsString(rsp));
		GenericRepository dao = new GenericRepository();
		try{
			MsgAddress ma = dao.queryEntity(
					new RNQuery().addFilter("address", (String) rsp.getTo().getAddress()),
					MsgAddress.class, false);
			// create a record for the email
			if (null == ma) {
	//			ma = new MsgAddress().setActiveCategories(categories);
	//					MsgAddress.prepareNewMail(rsp.getTo().getAddress(),
	//					new ServiceList(rsp.getService(), categories));
	//			dao.add(ma);
				throw new RuntimeException("not implemented");
			}
			// check email to be send (template) categories
			String category = sendAction.getCategory(rsp.getAction().getText());
			if (!ma.containsCategory(category)) {
				// receiver has not subscribed this category
				log.debug("receiver " + rsp.getTo().getAddress()
						+ " has not subscribed this category " + category);
				return;
			}
			// prepare send journal
			try {
				String sendHash = new Sha1Hex().makeSHA1Hash(System.currentTimeMillis() + rsp.getTo().getAddress() + category);
				SendJournal sj = new SendJournal().setDestination(ma).setHash(
						sendHash);
				dao.add(sj);
				//mr.put("sendHash", sendHash);
			} catch (NoSuchAlgorithmException e) {
				log.error("sendMessage:", e);
			}
			
			client.send(emailFactory.constructSubject(rsp, sendAction), rsp.getTo().getAddress(),
					MessagingServletConfig.senderMail,
					emailFactory.constructTxt(rsp, sendAction),
					emailFactory.constructHtml(rsp, sendAction));
		} finally{
			dao.closePersistenceManager();	
		}
	}

}
