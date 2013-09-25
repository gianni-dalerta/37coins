package com._37coins.conversion;

import java.math.BigDecimal;

import com._37coins.workflow.pojo.MessageAddress;

public class CallPrices {
	
	//TODO: implement parsing ~/Downloads/plivo_outbound_rates.csv
	
	static public BigDecimal getUsdPrice(MessageAddress destination){
		return new BigDecimal("0.2").setScale(2);
	}

}
