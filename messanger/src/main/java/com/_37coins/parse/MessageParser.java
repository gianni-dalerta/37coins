package com._37coins.parse;

import java.io.File;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.servlet.ServletContext;

import org.joda.money.BigMoney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Withdrawal;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.i18n.phonenumbers.NumberParseException;

public class MessageParser {
	public static Logger log = LoggerFactory.getLogger(MessageParser.class);
	public static final String BC_ADDR_REGEX = "^[mn13][1-9A-Za-z][^OIl]{20,40}";
	public static final String RB_NAME = "37coins";
	public static final List<Action> reqCmdList = Arrays.asList(
			Action.BALANCE,
			Action.DEPOSIT_REQ,
			Action.HELP, 
			Action.TRANSACTION, 
			Action.WITHDRAWAL_CONF, 
			Action.WITHDRAWAL_REQ, 
			Action.WITHDRAWAL_REQ_OTHER);

	private Map<String, Tag> wordMap = new HashMap<>();

	public MessageParser() {
		this(null);
	}

	public MessageParser(ServletContext sc) {
		// figure out class loader
		File[] files= null;
		ClassLoader loader = null;
		if(null!=sc && !sc.getServerInfo().contains("jetty")){
			Set<String> paths = sc.getResourcePaths("/WEB-INF/classes/");
			files = new File[paths.size()];
			int i = 0;
			for (String path : paths){
				files[i] = new File(path);
				i++;
			}
			loader = MessageParser.class.getClassLoader();
		}else{
			try{
				URL bundle = (null != sc) ? sc.getResource("/WEB-INF/classes/"+RB_NAME+"_en.properties")
						: ClassLoader.getSystemClassLoader().getResource(RB_NAME+"_en.properties");
				File root = new File(bundle.getFile()).getParentFile();
				files = root.listFiles();
				URL[] urls = {root.toURI().toURL()};
				loader = new URLClassLoader(urls);
			}catch(Exception e){}
		}
		// find all available locales
		List<Locale> locales = new ArrayList<>();
		for (File file : files) {
			if (file.getName().matches(".*"+RB_NAME+"_..\\.properties")) {
				int l = file.getName().length();
				locales.add(new Locale(file.getName().substring(l - 13, l - 11)));
			}
		}
		// create a map of command words to actions and locales
		for (Locale locale : locales) {
			try {
				ResourceBundle rb = ResourceBundle.getBundle(RB_NAME, locale,loader);
				for (Action a : reqCmdList) {
					String cmdList = rb.getString(a.getText() + "Cmd");
					for (String cmd : cmdList.split(",")) {
						wordMap.put(cmd, new Tag(a, locale));
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public boolean verify(String address) {
		// TODO: make sure it's a decent address
		return true;
	}

	public Locale readLanguage(String cmd) {
		for (String pos : wordMap.keySet()) {
			if (cmd.equalsIgnoreCase(pos)) {
				return wordMap.get(pos).getLocale();
			}
		}
		return null;
	}

	public Action replaceCommand(String cmd) {
		for (String pos : wordMap.keySet()) {
			if (cmd.equalsIgnoreCase(pos)) {
				return wordMap.get(pos).getAction();
			}
		}
		return null;
	}

	public boolean readReceiver(Withdrawal w, String receiver, MessageAddress to) {
		if (receiver == null | receiver.length() < 3) {
			return false;
		}
		if (receiver.matches(BC_ADDR_REGEX)) {
			try {
				Base58.decodeChecked(receiver);
				w.setPayDest(new PaymentAddress()
					.setAddress(receiver)
					.setAddressType(PaymentType.BTC));
				return true;
			} catch (AddressFormatException e) {
				return false;
			}
		}
		try {
			w.setMsgDest(MessageAddress.fromString(receiver, to));
			return true;
		} catch (AddressException | NumberParseException e1) {
			return false;
		}
	}

	public boolean readAmount(Withdrawal w, String amount) {
		if (!amount.contains("BTC")){
			amount = "BTC "+amount;
		}
		try {
			BigMoney money = BigMoney.parse(amount);
			w.setAmount(money.getAmount().setScale(8,RoundingMode.CEILING));
			return true;
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public DataSet process(MessageAddress sender, String subject) {
		subject = subject.trim().replaceAll(" +", " ");
		String[] ca = subject.split(" ");
		DataSet data = new DataSet()
			.setLocale(readLanguage(ca[0]))
			.setAction(replaceCommand(ca[0]))
			.setTo(sender);
		
		if (data.getAction() == Action.WITHDRAWAL_REQ
				|| data.getAction() == Action.WITHDRAWAL_REQ_OTHER){
			int pos = (ca[1].length() > ca[2].length()) ? 1 : 2;
			Withdrawal w = new Withdrawal();
			if (!readReceiver(w, ca[pos], data.getTo()) 
					|| !readAmount(w, ca[(pos == 1) ? 2 : 1])) {
				data.setAction(Action.FORMAT_ERROR);
				return data;
			}
			if (ca.length > 3){
				int i = subject.indexOf(' ', 1+subject.indexOf(' ', 1+subject.trim().indexOf(' ')));
				w.setComment(subject.replaceAll("::", "").substring(i+1, (i+1+20>subject.length())?subject.length():i+1+20));
			}
			data.setPayload(w);
		}
		if (data.getAction() == Action.WITHDRAWAL_CONF){
			data.setPayload(ca[1]);
		}
		return data;
	}
}
