package com._37coins.conversion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConversionService {
	
	private final Map<Currency,BigDecimal> rates;
	
	public ConversionService(){
		rates = new HashMap<>();
		// set on sep. 25th
		//TODO: write a feed subscriber
		rates.put(Currency.getInstance(Locale.US), new BigDecimal("128.84").setScale(2));
		rates.put(Currency.getInstance(Locale.GERMANY), new BigDecimal("95.54").setScale(2));
		rates.put(Currency.getInstance(Locale.UK), new BigDecimal("84.59").setScale(2));
		rates.put(Currency.getInstance(Locale.CHINA), new BigDecimal("744.30").setScale(2));
		rates.put(Currency.getInstance(Locale.CANADA), new BigDecimal("127.83").setScale(2));
	}
	
	public BigDecimal convertToBtc(BigDecimal amount, Currency currency){
		BigDecimal rate = rates.get(currency);
		return amount.setScale(8).divide(rate.setScale(8),RoundingMode.HALF_EVEN);
	}
	
	
	public BigDecimal convertFromBtc(BigDecimal amount, Currency targetCurrency){
		BigDecimal rate = rates.get(targetCurrency).setScale(2);
		return amount.multiply(rate).setScale(2);
	}

}
