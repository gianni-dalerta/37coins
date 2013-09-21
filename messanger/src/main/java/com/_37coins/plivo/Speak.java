package com._37coins.plivo;

import javax.xml.bind.annotation.XmlValue;
 
public class Speak {
	
	private String text;

	public String getText() {
		return text;
	}

	@XmlValue
	public Speak setText(String text) {
		this.text = text;
		return this;
	}
	
	

}
