package com._37coins.web;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import edu.vt.middleware.password.CharacterCharacteristicsRule;
import edu.vt.middleware.password.DigitCharacterRule;
import edu.vt.middleware.password.LengthRule;
import edu.vt.middleware.password.LowercaseCharacterRule;
import edu.vt.middleware.password.NonAlphanumericCharacterRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.PasswordValidator;
import edu.vt.middleware.password.QwertySequenceRule;
import edu.vt.middleware.password.Rule;
import edu.vt.middleware.password.RuleResult;
import edu.vt.middleware.password.UppercaseCharacterRule;
import edu.vt.middleware.password.WhitespaceRule;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AccountPolicy {
	
	private String emailRegex;
	
	private boolean emailMxLookup;
	
	private Integer pwMinLength = 6;
	
	private Integer pwMaxLength = 12;
	
	private Integer minDigit = 1;
	
	private Integer minNonAlpha = 1;
	
	private Integer minUpper = 1;
	
	private Integer minLower = 1;

	public String getEmailRegex() {
		return emailRegex;
	}

	public AccountPolicy setEmailRegex(String emailRegex) {
		this.emailRegex = emailRegex;
		return this;
	}

	public boolean isEmailMxLookup() {
		return emailMxLookup;
	}

	public AccountPolicy setEmailMxLookup(boolean emailMxLookup) {
		this.emailMxLookup = emailMxLookup;
		return this;
	}

	public Integer getPwMinLength() {
		return pwMinLength;
	}

	public AccountPolicy setPwMinLength(Integer pwMinLength) {
		this.pwMinLength = pwMinLength;
		return this;
	}

	public Integer getPwMaxLength() {
		return pwMaxLength;
	}

	public AccountPolicy setPwMaxLength(Integer pwMaxLength) {
		this.pwMaxLength = pwMaxLength;
		return this;
	}

	public Integer getMinDigit() {
		return minDigit;
	}

	public AccountPolicy setMinDigit(Integer minDigit) {
		this.minDigit = minDigit;
		return this;
	}

	public Integer getMinNonAlpha() {
		return minNonAlpha;
	}

	public AccountPolicy setMinNonAlpha(Integer minNonAlpha) {
		this.minNonAlpha = minNonAlpha;
		return this;
	}

	public Integer getMinUpper() {
		return minUpper;
	}

	public AccountPolicy setMinUpper(Integer minUpper) {
		this.minUpper = minUpper;
		return this;
	}

	public Integer getMinLower() {
		return minLower;
	}

	public AccountPolicy setMinLower(Integer minLower) {
		this.minLower = minLower;
		return this;
	}
	
	//#################### VALIDATION
	
	//Regular Expression Test
	public static boolean isValidEmail(String email){
		String regex = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\b";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(email);
		if(m.find()){
			return true;
		}
		else{
			return false;
		}
	}
	
	//from http://www.rgagnon.com/javadetails/java-0452.html
	//MX (Mail Exchange) Domain Test
	//also used http://www.tomred.net/tutorials/tomred-java-extended-email-validation-using-dns-mx-lookup.html
	public static boolean isValidMX(String email) throws NamingException{
			String hostName = email.substring(email.indexOf("@") + 1, email.length());
			// is it one of the common domains. 
			String [] hosts = {"gmail.com","hotmail.com","googlemail.com","yahoo.com"};
			for (String host : hosts)  
				if(hostName.trim().equalsIgnoreCase(host))
					return true;
		    Hashtable<String,String> env = new Hashtable<>();
		    env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		    DirContext ictx = new InitialDirContext( env );
		    Attributes attrs = ictx.getAttributes(hostName, new String[] { "MX" });
		    Attribute attr = attrs.get( "MX" );

		    if( attr == null ){
		    	return false;
		    }
		    else{
		    	return true;
		    }
	}

	public boolean validatePassword(String pw){
		List<Rule> ruleList = new ArrayList<Rule>();
		// password must be between 8 and 16 chars long
		LengthRule lengthRule = new LengthRule(this.getPwMinLength(), this.getPwMaxLength());
		// don't allow whitespace
		WhitespaceRule whitespaceRule = new WhitespaceRule();
		// control allowed characters
		CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
		// require at least 1 digit in passwords
		charRule.getRules().add(new DigitCharacterRule(this.getMinDigit()));
		// require at least 1 non-alphanumeric char
		charRule.getRules().add(new NonAlphanumericCharacterRule(this.getMinNonAlpha()));
		// require at least 1 upper case char
		charRule.getRules().add(new UppercaseCharacterRule(this.getMinUpper()));
		// require at least 1 lower case char
		charRule.getRules().add(new LowercaseCharacterRule(this.getMinLower()));
		// don't allow qwerty sequences
		QwertySequenceRule qwertySeqRule = new QwertySequenceRule();
		
		ruleList.add(lengthRule);
		ruleList.add(whitespaceRule);
		ruleList.add(charRule);
		ruleList.add(qwertySeqRule);

		PasswordValidator validator = new PasswordValidator(ruleList);
		PasswordData passwordData = new PasswordData(new Password(pw));

		RuleResult result = validator.validate(passwordData);
		return result.isValid();
	}
	

}
