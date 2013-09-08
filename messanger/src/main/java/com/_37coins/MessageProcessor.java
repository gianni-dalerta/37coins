package com._37coins;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;

public class MessageProcessor {
	public static Logger log = LoggerFactory.getLogger(MessageProcessor.class);
	public static final String BC_ADDR_REGEX = "^[mn13][1-9A-Za-z][^OIl]{20,40}";
	public static final String PHONE_REGEX = "^(\\+|\\d)[0-9]{7,16}$";
	public static final String EMAIL_REGEX = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";

	public enum Action {
		CREATE("create"), // create a new account
		DEPOSIT("deposit"), // request a bitcoin address to receive a payment
		SEND("send"), // send a payment
		REQUEST("request"), // send a payment
		SEND_CONFIRM("confirm"), BALANCE("balance"), // request the balance
		HELP("help");

		private String text;

		Action(String text) {
			this.text = text;
		}

		@JsonValue
		final String value() {
			return this.text;
		}

		public String getText() {
			return this.text;
		}

		@JsonCreator
		public static Action fromString(String text) {
			if (text != null) {
				for (Action b : Action.values()) {
					if (text.equalsIgnoreCase(b.text)) {
						return b;
					}
				}
			}
			return null;
		}
	}

	private Map<String, Tag> wordMap = new HashMap<>();

	public MessageProcessor() {
		this(null);
	}

	public MessageProcessor(ServletContext sc) {
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
			loader = MessageProcessor.class.getClassLoader();
		}else{
			try{
				URL bundle = (null != sc) ? sc.getResource("/WEB-INF/classes/37coins_en.properties")
						: ClassLoader.getSystemClassLoader().getResource("37coins_en.properties");
				File root = new File(bundle.getFile()).getParentFile();
				files = root.listFiles();
				URL[] urls = {root.toURI().toURL()};
				loader = new URLClassLoader(urls);
			}catch(Exception e){}
		}
		// find all available locales
		List<Locale> locales = new ArrayList<>();
		for (File file : files) {
			if (file.getName().matches(".*37coins_..\\.properties")) {
				int l = file.getName().length();
				locales.add(new Locale(file.getName().substring(l - 13, l - 11)));
			}
		}
		// create a map of command words to actions and locales
		for (Locale locale : locales) {
			try {
				ResourceBundle rb = ResourceBundle.getBundle("37coins", locale,loader);
				for (Action a : Action.values()) {
					String cmdList = rb.getString(a.getText() + "Cmd");
					for (String cmd : cmdList.split(",")) {
						wordMap.put(cmd, new Tag(a,
								locale));
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

	public boolean readReceiver(Map<String, Object> data, String receiver) {
		if (receiver == null | receiver.length() < 3) {
			return false;
		}
		if (receiver.matches(PHONE_REGEX)) {
			data.put("receiverPhone", receiver);
			return true;
		}
		if (receiver.matches(EMAIL_REGEX)) {
			data.put("receiverEmail", receiver);
			return true;
		}
		if (receiver.matches(BC_ADDR_REGEX)) {
			try {
				Base58.decodeChecked(receiver);
				data.put("receiver", receiver);
				return true;
			} catch (AddressFormatException e) {
				return false;
			}
		}
		return false;
	}

	public boolean readAmount(Map<String, Object> data, String amount) {
		CurrencyValue cv = parseCurrency(amount);
		if (cv.getDecimalPart() != null) {
			BigDecimal rv;
			if (cv.getDecimalPart() != null) {
				rv = new BigDecimal(cv.getIntegerPart() + "."
						+ cv.getDecimalPart());
			} else {
				rv = new BigDecimal(cv.getIntegerPart());
			}
			if (cv.getCurrency() == null || cv.getCurrency().length() == 0) {
				data.put("amount", rv);
			} else {
				data.put("amountCur", rv);
				data.put("sumbolCur", cv.getCurrency());
			}
			return true;
		} else {
			return false;
		}
	}

	public Map<String, Object> process(Address[] addresses, String subject) {
		String sender = null;
		if (null == addresses || addresses.length != 1
				|| !verify(addresses[0].toString())) {
			log.error("not a good sender address, exiting!");
			return null;
		} else {
			sender = ((InternetAddress) addresses[0]).getAddress();
		}
		return process(sender, subject);
	}

	public Map<String, Object> process(String sender, String subject) {
		Map<String, Object> c = new HashMap<>();
		String[] ca = subject.trim().split(" ");
		if (ca.length < 1) {
			return sendError(sender, "error000");
		}
		// read language
		c.put("locale", readLanguage(ca[0]));
		// replace other language occurences of command
		Action action = replaceCommand(ca[0]);
		if (null == action || null == c.get("locale")) {
			return sendError(sender, "error000");
		}
		c.put("action", action.getText());
		c.put("msgAddress", sender);
		switch (action) {
		case BALANCE:
			break;
		case CREATE:
			break;
		case DEPOSIT:
			break;
		case HELP:
			Map<String, Object> r = new HashMap<>();
			r.put("locale", new Locale("en"));
			r.put("msgAddress", sender);
			r.put("action", "help");
			return r;
		case SEND_CONFIRM:
			// TODO: think about it
			break;
		case REQUEST:
		case SEND:
			int pos = (ca[1].length() > ca[2].length()) ? 1 : 2;
			if (!readReceiver(c, ca[pos])) {
				return sendError(sender, "error002");
			}
			if (!readAmount(c, ca[(pos == 1) ? 2 : 1])) {
				return sendError(sender, "error002");
			}
			break;
		}
		return c;
	}

	public Map<String, Object> sendError(String sender, String errMsg) {
		Map<String, Object> r = new HashMap<>();
		r.put("locale", new Locale("en"));
		r.put("msgAddress", sender);
		r.put("action", errMsg);
		return r;
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
