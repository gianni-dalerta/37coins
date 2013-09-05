package com._37coins.sendMail;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

public interface MailServiceClient {
	
	public void send (String subject, String receiver, String sender, String text, String html)  throws AddressException, MessagingException;

}
