package com._37coins.workflow.pojo;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@JsonInclude(Include.NON_NULL)
public class MessageAddress {
	public static final String PHONE_REGEX = "^(\\+|\\d)[0-9]{7,16}$";
	public static final String EMAIL_REGEX = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
	
	public static MessageAddress fromString(String address,String gateway) throws AddressException, NumberParseException{
		if (address.matches(PHONE_REGEX)) {
			//prepare the gateway
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
			gateway = (gateway.charAt(0)!='+')?"+"+gateway:gateway;
			MessageAddress referrer = new MessageAddress()
				.setAddressType(MsgType.SMS)
				.setPhoneNumber(phoneUtil.parse(gateway, "ZZ"));
			return fromString(address, referrer);
		}else if (address.matches(EMAIL_REGEX)) {
			return new MessageAddress()
				.setAddressType(MsgType.EMAIL)
				.setEmail(new InternetAddress(address));
		}
		throw new RuntimeException("no match");
	}
	
	public static MessageAddress fromString(String address,MessageAddress gateway) throws AddressException, NumberParseException{
		if (address.matches(PHONE_REGEX)) {
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
			PhoneNumber pn = null;
			if (address.charAt(0)=='+'||
					address.substring(0, 2).equalsIgnoreCase("00")||
					(address.substring(0, 2).equalsIgnoreCase("011")&&address.length()>11)){
				pn = phoneUtil.parse(address, "ZZ");
			}else{
				if (gateway.getAddressType()!=MsgType.SMS){
					throw new RuntimeException("from email you can only send to phone numbers with country code.");
				}
				pn = phoneUtil.parse( address, phoneUtil.getRegionCodeForCountryCode(gateway.getPhoneNumber().getCountryCode()));
			}
			PhoneNumberType pnt = phoneUtil.getNumberType(pn);
			if (pnt==PhoneNumberType.UNKNOWN 
					||pnt==PhoneNumberType.FIXED_LINE_OR_MOBILE
					||pnt==PhoneNumberType.MOBILE){
				return new MessageAddress()
					.setAddressType(MsgType.SMS)
					.setPhoneNumber(pn);
			}else{
				throw new RuntimeException("number not mobile number");
			}
		}else if (address.matches(EMAIL_REGEX)) {
			return new MessageAddress()
				.setAddressType(MsgType.EMAIL)
				.setEmail(new InternetAddress(address));
		}
		throw new RuntimeException("no match");
	}
	
	public enum MsgType {
		SMS,
		EMAIL,
		UNKNOWN;
	}
	
	private PhoneNumber phoneNumber;
	
	private InternetAddress email;
	
	private MsgType addressType;
	
	private String gateway;

	@JsonIgnore
	public String getAddress() {
		if (addressType==MsgType.SMS){
			return PhoneNumberUtil.getInstance().format(phoneNumber,PhoneNumberFormat.E164);
		}
		if (addressType==MsgType.EMAIL){
			return email.toString();
		}
		return null;
	}
	
	@JsonIgnore
	public Object getAddressObject(){
		if (addressType==MsgType.SMS){
			return phoneNumber;
		}
		if (addressType==MsgType.EMAIL){
			return email;
		}
		return null;
	}
	
	@JsonIgnore
	public MessageAddress setAddress(Object obj){
		if (obj instanceof PhoneNumber){
			phoneNumber = (PhoneNumber)obj;
		}else if (obj instanceof InternetAddress){
			email = (InternetAddress)obj;
		}else if (obj instanceof String){
			String address = (String)obj;
			if (address.matches(PHONE_REGEX)) {
				try {
					phoneNumber = PhoneNumberUtil.getInstance().parse(address, "ZZ");
					addressType = MsgType.SMS;
				} catch (NumberParseException e) {
					e.printStackTrace();
				}
			}else if (address.matches(EMAIL_REGEX)) {
				try {
					email = new InternetAddress(address);
					addressType = MsgType.EMAIL;
				} catch (AddressException e) {
					e.printStackTrace();
				}
			}
		}
		return this;
	}
	
	public MsgType getAddressType() {
		return addressType;
	}

	public MessageAddress setAddressType(MsgType addressType) {
		this.addressType = addressType;
		return this;
	}

	public String getGateway() {
		return gateway;
	}

	public MessageAddress setGateway(String gateway) {
		this.gateway = gateway;
		return this;
	}

	public PhoneNumber getPhoneNumber() {
		return phoneNumber;
	}

	public MessageAddress setPhoneNumber(PhoneNumber phoneNumber) {
		this.phoneNumber = phoneNumber;
		return this;
	}

	public InternetAddress getEmail() {
		return email;
	}

	public MessageAddress setEmail(InternetAddress email) {
		this.email = email;
		return this;
	}

}
