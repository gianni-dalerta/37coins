package com._37coins.parse;

import java.io.IOException;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Singleton;
import javax.mail.internet.AddressException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.money.BigMoney;

import com._37coins.resources.ParserResource;
import com._37coins.workflow.pojo.DataSet;
import com._37coins.workflow.pojo.DataSet.Action;
import com._37coins.workflow.pojo.MessageAddress;
import com._37coins.workflow.pojo.PaymentAddress;
import com._37coins.workflow.pojo.PaymentAddress.PaymentType;
import com._37coins.workflow.pojo.Withdrawal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.i18n.phonenumbers.NumberParseException;

@Singleton
public class ParserFilter implements Filter {
	public static final String BC_ADDR_REGEX = "^[mn13][1-9A-Za-z][^OIl]{20,40}";

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest) request;
		// parse parameters
		String from = httpReq.getParameter("from");
		String gateway = httpReq.getParameter("gateway");
		String message = httpReq.getParameter("message");
		// Parse the locale
		String acceptLng = httpReq.getHeader("Accept-Language");
		Locale locale = null;
		if (null!=acceptLng){
			String str = acceptLng.split(",")[0];
			String[] arr = str.trim().replace("-", "_").split(";");
			
			String[] l = arr[0].split("_");
			switch (l.length) {
			case 2:
				locale = new Locale(l[0], l[1]);
				break;
			case 3:
				locale = new Locale(l[0], l[1], l[2]);
				break;
			default:
				locale = new Locale(l[0]);
				break;
			}
		}
		if (null==locale){
			locale = new Locale("eo_UY"); //esperanto
		}
		// parse action
		String url = httpReq.getRequestURL().toString();
		String actionString = url.substring(
				url.indexOf(ParserResource.PATH) + ParserResource.PATH.length() + 1, url.length());
		try {
			// parse message address
			MessageAddress md = MessageAddress.fromString(from, gateway)
					.setGateway(gateway);
			// parse message into dataset
			DataSet responseData = process(md, message, locale,Action.fromString(actionString));
			List<DataSet> responseList = new ArrayList<>();
			responseList.add(responseData);
			//use it
			if (responseData.getAction()==Action.UNKNOWN_COMMAND||CommandParser.reqCmdList.contains(responseData.getAction())){
				httpReq.setAttribute("dsl", responseList);
				chain.doFilter(request, response);
			}else{
				respond(responseList,response);
			}
		} catch (Exception e) {
			e.printStackTrace();
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	public void respond(List<DataSet> dsl, ServletResponse response){
		OutputStream os = null;
		try {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			os = httpResponse.getOutputStream();
			new ObjectMapper().writeValue(os, dsl);
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {if (null!=os)os.close();} catch (IOException e) {}
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	public boolean readReceiver(Withdrawal w, String receiver, MessageAddress to) {
		if (receiver == null | receiver.length() < 3) {
			return false;
		}
		if (receiver.matches(BC_ADDR_REGEX)) {
			try {
				Base58.decodeChecked(receiver);
				w.setPayDest(new PaymentAddress().setAddress(receiver)
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
		if (!amount.contains("BTC")) {
			amount = "BTC " + amount;
		}
		try {
			BigMoney money = BigMoney.parse(amount);
			w.setAmount(money.getAmount().setScale(8, RoundingMode.CEILING));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public DataSet process(MessageAddress sender, String subject,
			Locale locale, Action action) {
		subject = subject.trim().replaceAll(" +", " ");
		String[] ca = subject.split(" ");
		DataSet data = new DataSet().setLocale(locale).setAction(action)
				.setTo(sender);

		if (data.getAction() == Action.WITHDRAWAL_REQ
				|| data.getAction() == Action.WITHDRAWAL_REQ_OTHER) {
			int pos = (ca[1].length() > ca[2].length()) ? 1 : 2;
			Withdrawal w = new Withdrawal();
			if (!readReceiver(w, ca[pos], data.getTo())
					|| !readAmount(w, ca[(pos == 1) ? 2 : 1])) {
				data.setAction(Action.FORMAT_ERROR);
				return data;
			}
			if (ca.length > 3) {
				int i = subject.indexOf(' ', 1 + subject.indexOf(' ',
						1 + subject.trim().indexOf(' ')));
				w.setComment(subject.replaceAll("::", "").substring(
						i + 1,
						(i + 1 + 20 > subject.length()) ? subject.length()
								: i + 1 + 20));
			}
			data.setPayload(w);
		}
		if (data.getAction() == Action.WITHDRAWAL_CONF) {
			data.setPayload(ca[1]);
		}
		return data;
	}

}
