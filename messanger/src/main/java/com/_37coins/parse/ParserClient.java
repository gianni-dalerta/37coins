package com._37coins.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.mail.internet.AddressException;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.google.i18n.phonenumbers.NumberParseException;

public class ParserClient extends Thread {
	
	private String from;
	private String gateway;
	private String message;
	private int localPort;
	private final CommandParser commandParser;
	private ParserAction pa;
	
	@Inject
	public ParserClient(CommandParser commandParser){
		this.commandParser = commandParser;
	}
	
	public void start(String from, String gateway, String message, int localPort, ParserAction pa){
		this.from = from;
		this.gateway = gateway;
		this.message = message;
		this.localPort = localPort;
		this.pa = pa;
		this.start();
	}

	@Override
	public void run() {
		Action action = commandParser.processCommand(message);
		Locale locale = commandParser.guessLocale(message);
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost req = new HttpPost("http://127.0.0.1:"+localPort+"/parser/"+((null!=action)?action.getText():"UnknownCommand"));
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("from", from));
		nvps.add(new BasicNameValuePair("gateway", gateway));
		nvps.add(new BasicNameValuePair("message", message));
		if (null!=locale){
			req.addHeader("Accept-Language", locale.toString().replace("_", "-"));
		}
		List<DataSet> results = null;
		try {
			req.setEntity(new UrlEncodedFormEntity(nvps));
			CloseableHttpResponse rsp = httpclient.execute(req);
			if (rsp.getStatusLine().getStatusCode()==200){
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false); 
		        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL);
				results = mapper.readValue(rsp.getEntity().getContent(),new TypeReference<List<DataSet>>() { });
				Collections.reverse(results);
			}
			if (null==results){
				results = Arrays.asList(new DataSet().setAction(Action.FORMAT_ERROR).setTo(MessageAddress.fromString(from, gateway)).setLocale(locale));
			}
		} catch (IOException |AddressException |NumberParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (DataSet result: results){
			switch(result.getAction()){
			case WITHDRAWAL_REQ_OTHER:
			case WITHDRAWAL_REQ:
				pa.handleWithdrawal(result);
				break;
			case BALANCE:
			case TRANSACTION:
			case DEPOSIT_REQ:
				pa.handleDeposit(result);
				break;
			case WITHDRAWAL_CONF:
				pa.handleConfirm(result);
		        break;
			default:
				pa.handleResponse(result);
				break;
			}
		}
	}
	

	
	
}
