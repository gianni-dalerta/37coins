package com._37coins.plivo;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Speak")
public class Wait {
	
	private Integer length;

	public Integer getLength() {
		return length;
	}

	@XmlAttribute
	public Wait setLength(Integer length) {
		this.length = length;
		return this;
	}
	
	

}
