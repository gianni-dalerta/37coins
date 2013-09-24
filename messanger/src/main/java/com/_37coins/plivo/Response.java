package com._37coins.plivo;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;


//<Response>
//<GetDigits action="http://www.example.com/gather/" method="GET">
//  <Speak>Please enter your 4-digit pin number, followed by the hash key</Speak>
//</GetDigits>
//<Speak>Input not received. Thank you</Speak>
//</Response>

@XmlRootElement(name="Response")
public class Response {
	
	private List<Object> elements;

	public List<Object> getElements() {
		return elements;
	}
	
	@XmlElements({ 
	    @XmlElement(name="GetDigits", type=GetDigits.class),
	    @XmlElement(name="Speak", type=Speak.class),
	    @XmlElement(name="Redirect", type=Redirect.class),
	    @XmlElement(name="Wait", type=Wait.class)
	})
	public Response setElements(List<Object> elements) {
		this.elements = elements;
		return this;
	}

	public Response add(Object o){
		if (elements==null){
			elements = new ArrayList<>();
		}
		elements.add(o);
		return this;
	}
	
}
