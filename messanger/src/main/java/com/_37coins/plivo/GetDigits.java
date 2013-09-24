package com._37coins.plivo;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="GetDigits")
public class GetDigits {
	
	private URL action;
	
	private Speak speak;
	
	private int numDigits;
	
	private String method;
	
	private boolean redirect;

	public URL getAction() {
		return action;
	}

	public GetDigits setAction(String action) {
		try {
			this.action = new URL(action);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	@XmlAttribute
	public GetDigits setAction(URL action) {
		this.action = action;
		return this;
	}

	public Speak getSpeak() {
		return speak;
	}
	
	@XmlElement(name="Speak")
	public GetDigits setSpeak(Speak speak) {
		this.speak = speak;
		return this;
	}

	public int getNumDigits() {
		return numDigits;
	}

	@XmlAttribute
	public GetDigits setNumDigits(int numDigits) {
		this.numDigits = numDigits;
		return this;
	}

	public boolean isRedirect() {
		return redirect;
	}

	@XmlAttribute
	public GetDigits setRedirect(boolean redirect) {
		this.redirect = redirect;
		return this;
	}

	public String getMethod() {
		return method;
	}

	@XmlAttribute
	public GetDigits setMethod(String method) {
		this.method = method;
		return this;
	}
}
