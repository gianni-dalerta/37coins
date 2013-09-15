package com._37coins.parse;

import java.io.File;
import java.math.BigDecimal;
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

import javax.servlet.ServletContext;

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

public class MessageParser {
	public static Logger log = LoggerFactory.getLogger(MessageParser.class);
	public static final String BC_ADDR_REGEX = "^[mn13][1-9A-Za-z][^OIl]{20,40}";
	public static final String PHONE_REGEX = "^(\\+|\\d)[0-9]{7,16}$";
	public static final String EMAIL_REGEX = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
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

	public boolean readReceiver(Withdrawal w, String receiver) {
		if (receiver == null | receiver.length() < 3) {
			return false;
		}
		if (receiver.matches(PHONE_REGEX)) {
			w.setMsgDest(new MessageAddress().setAddress(receiver));
			//TODO: retrieve gateway
			return true;
		}
		if (receiver.matches(EMAIL_REGEX)) {
			w.setMsgDest(new MessageAddress().setAddress(receiver));
			//TODO: retrieve gateway
			return true;
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
		return false;
	}

	public boolean readAmount(Withdrawal w, String amount) {
		CurrencyValue cv = parseCurrency(amount);
		if (cv.getDecimalPart() != null) {
			BigDecimal rv;
			if (cv.getDecimalPart() != null) {
				rv = new BigDecimal(cv.getIntegerPart() + "."
						+ cv.getDecimalPart());
			} else {
				rv = new BigDecimal(cv.getIntegerPart());
			}
			rv = rv.setScale(8, RoundingMode.UP);//rounding up, better send to much, isn't it?
			//null represents BITCOIN, probably not a good idea
			if (cv.getCurrency() != null && cv.getCurrency().length() > 0) {
				throw new RuntimeException("not implemented.");
			}
			w.setAmount(rv);
			return true;
		} else {
			return false;
		}
	}
	
	public DataSet process(MessageAddress sender, String subject) {
		String[] ca = subject.trim().split(" ");
		// read language
		DataSet data = new DataSet()
			.setLocale(readLanguage(ca[0]))
			.setAction(replaceCommand(ca[0]))
			.setTo(sender);
		
		if (data.getAction() == Action.WITHDRAWAL_REQ){
			int pos = (ca[1].length() > ca[2].length()) ? 1 : 2;
			Withdrawal w = new Withdrawal();
			if (!readReceiver(w, ca[pos]) 
					|| !readAmount(w, ca[(pos == 1) ? 2 : 1])) {
				data.setAction(Action.FORMAT_ERROR);
			}
			data.setPayload(w);
		}
		if (data.getAction() == Action.WITHDRAWAL_CONF){
			data.setPayload(ca[1]);
		}
		return data;
	}

	/**
	 * Parses a string that represents an amount of money.
	 * 
	 * @param s
	 *            A string to be parsed
	 * @return A currency value containing the currency, integer part, and
	 *         decimal part.
	 */
	public static CurrencyValue parseCurrency(String s) {
		if (s == null || s.length() == 0)
			throw new NumberFormatException("String is null or empty");
		int i = 0;
		int currencyLength = 0;
		String currency = "";
		String decimalPart = "";
		String integerPart = "";
		while (i < s.length()) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c) || (c >= '0' && c <= '9'))
				break;
			currencyLength++;
			i++;
		}
		if (currencyLength > 0) {
			currency = s.substring(0, currencyLength);
		}
		// Skip whitespace
		while (i < s.length()) {
			char c = s.charAt(i);
			if (!Character.isWhitespace(c))
				break;
			i++;
		}
		// Parse number
		int numberStart = i;
		int numberLength = 0;
		int digits = 0;
		// char lastSep=' ';
		while (i < s.length()) {
			char c = s.charAt(i);
			if (!((c >= '0' && c <= '9') || c == '.' || c == ','))
				break;
			numberLength++;
			if ((c >= '0' && c <= '9'))
				digits++;
			i++;
		}
		if (digits == 0)
			throw new NumberFormatException("No number");
		// Get the decimal part, up to 2 digits
		for (int j = numberLength - 1; j >= numberLength - 3 && j >= 0; j--) {
			char c = s.charAt(numberStart + j);
			if (c == '.' || c == ',') {
				// lastSep=c;
				int nsIndex = numberStart + j + 1;
				int nsLength = numberLength - 1 - j;
				decimalPart = s.substring(nsIndex, nsIndex + nsLength);
				numberLength = j;
				break;
			}
		}
		// Get the integer part
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < numberLength; j++) {
			char c = s.charAt(numberStart + j);
			if ((c >= '0' && c <= '9'))
				sb.append(c);
		}
		integerPart = sb.toString();
		if (currencyLength == 0) {
			// Skip whitespace
			while (i < s.length()) {
				char c = s.charAt(i);
				if (!Character.isWhitespace(c))
					break;
				i++;
			}
			int currencyStart = i;
			// Read currency
			while (i < s.length()) {
				char c = s.charAt(i);
				if (Character.isWhitespace(c) || (c >= '0' && c <= '9'))
					break;
				currencyLength++;
				i++;
			}
			if (currencyLength > 0) {
				currency = s.substring(currencyStart, currencyStart
						+ currencyLength);
			}
		}
		if (i != s.length())
			throw new NumberFormatException("Invalid currency string");
		CurrencyValue cv = new CurrencyValue();
		cv.setCurrency(currency);
		cv.setDecimalPart(decimalPart);
		cv.setIntegerPart(integerPart);
		return cv;
	}
}
