package com._37coins.sendMail;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.smtp.SMTPTransport;

public class SmtpEmailClient implements MailServiceClient {
	public static Logger log = LoggerFactory.getLogger(SmtpEmailClient.class);
	private final String host;
	private final String user;
	private final String password;
	private final Session session;
	
	public SmtpEmailClient(String host, String user, String password){
		this.host = host;
		this.user = user;
		this.password = password;
        Properties props = System.getProperties();
        props.put("mail.smtps.host",host);
        props.put("mail.smtps.auth","true");
        session = Session.getInstance(props, null);
	}

	@Override
	public void send(String subject, String receiver, String sender,
			String text, String html) throws AddressException, MessagingException {
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(sender));;
        msg.setRecipients(Message.RecipientType.TO,
        InternetAddress.parse(receiver, false));
        msg.setSubject(subject);
        Multipart mp = new MimeMultipart("alternative");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(text, "utf-8");
        mp.addBodyPart(textPart);
        //order matters: from low fidelity to high fidelity
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=utf-8");
        mp.addBodyPart(htmlPart);
        msg.setSentDate(new Date());
        msg.setContent(mp);
        Transport t = (SMTPTransport)session.getTransport("smtps");
        if (!t.isConnected()){
            t.connect(host, user, password);
        }
        t.sendMessage(msg, msg.getAllRecipients());
        log.debug("Response: " + ((SMTPTransport)t).getLastServerResponse());
        t.close();
	}

}
