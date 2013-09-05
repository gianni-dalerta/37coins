package com._37coins.sendMail;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.google.inject.Inject;

public class AmazonEmailClient implements MailServiceClient {

	final AmazonSimpleEmailServiceClient service;

	@Inject
	public AmazonEmailClient(AmazonSimpleEmailServiceClient service) {
		this.service = service;
	}

	@Override
	public void send(String subject, String to, String from, String text,
			String html) {
		Content textContent = new Content().withData(text);
		Content htmlContent = new Content().withData(html);

		// combine
		Content subjContent = new Content().withData(subject);
		Message msg = new Message().withSubject(subjContent);
		Body body = new Body().withHtml(htmlContent).withText(textContent);
		msg.setBody(body);

		SendEmailRequest request = new SendEmailRequest().withSource(from);

		List<String> toAddresses = new ArrayList<String>();
		toAddresses.add(to);
		Destination dest = new Destination().withToAddresses(toAddresses);
		request.setDestination(dest);
		request.setMessage(msg);
		
		service.sendEmail(request);
	}

}
