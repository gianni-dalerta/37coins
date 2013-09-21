package com._37coins.plivo;

import javax.xml.bind.annotation.XmlRootElement;


//<Response>
//<GetDigits action="http://www.example.com/gather/" method="GET">
//  <Speak>Please enter your 4-digit pin number, followed by the hash key</Speak>
//</GetDigits>
//<Speak>Input not received. Thank you</Speak>
//</Response>

@XmlRootElement
public class Response {
	
	private GetDigits getDigits;
	
	private Speak speak;

	public GetDigits getGetDigits() {
		return getDigits;
	}

	public Response setGetDigits(GetDigits getDigits) {
		this.getDigits = getDigits;
		return this;
	}

	public Speak getSpeak() {
		return speak;
	}

	public Response setSpeak(Speak speak) {
		this.speak = speak;
		return this;
	}
	
}
