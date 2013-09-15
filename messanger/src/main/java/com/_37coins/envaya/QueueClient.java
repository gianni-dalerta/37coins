package com._37coins.envaya;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.StringEscapeUtils;

import com._37coins.workflow.pojo.Response;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import freemarker.template.TemplateException;

public class QueueClient {
	
	private Connection connection = null;
	private Channel channel = null;
	final MessageFactory msgFactory;
	
	@Inject
	public QueueClient(MessageFactory msgFactory){
		this.msgFactory = msgFactory;
	}
	
	private void connect(String uri, String exchangeName)  throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(uri);
		connection = factory.newConnection();
		channel = connection.createChannel();
	}
	
	public void send(Response rsp, String uri, String gateway, String exchangeName, String id) throws IOException, TemplateException, KeyManagementException, NoSuchAlgorithmException, URISyntaxException {
		if (null==connection || !connection.isOpen()){
			connect(uri, exchangeName);
		}
		String message = StringEscapeUtils.escapeJava(msgFactory.construct(rsp));
		String msg = "{\"event\":\"send\",\"messages\":[{\"id\":\""+id+"\",\"to\":\""+rsp.getTo().getAddress()+"\",\"message\":\""+message+"\"}]}";
		channel.basicPublish(exchangeName,gateway,null, msg.getBytes());
	}
	
	public void close(){
		try {
			channel.close();
		} catch (IOException e) {
		} finally{
			try {
				connection.close();
			} catch (IOException e) {
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
