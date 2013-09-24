package com._37coins.plivo;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name="Redirect")
public class Redirect {

	private String text;

	public String getText() {
		return text;
	}

	@XmlValue
	public Redirect setText(String text) {
		this.text = text;
		return this;
	}
	
}
