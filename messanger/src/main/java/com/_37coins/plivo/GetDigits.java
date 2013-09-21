package com._37coins.plivo;

import java.net.URL;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement 
public class GetDigits {
	
	private URL action;
	
	private String method;
	
	private Speak speak;

	public URL getAction() {
		return action;
	}

	@XmlAttribute
	public GetDigits setAction(URL action) {
		this.action = action;
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

	public Speak getSpeak() {
		return speak;
	}

	public GetDigits setSpeak(Speak speak) {
		this.speak = speak;
		return this;
	}
}
