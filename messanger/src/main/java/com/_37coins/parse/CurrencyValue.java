package com._37coins.parse;

public class CurrencyValue {
	@Override
	public String toString() {
		return "CurrencyValue [integerPart=" + integerPart + ", decimalPart="
				+ decimalPart + ", currency=" + currency + "]";
	}

	String integerPart;

	/**
	 * Gets the integer part of the value without separators.
	 * 
	 * @return
	 */
	public String getIntegerPart() {
		return integerPart;
	}

	public void setIntegerPart(String integerPart) {
		this.integerPart = integerPart;
	}

	/**
	 * Gets the decimal part of the value without separators.
	 * 
	 * @return
	 */
	public String getDecimalPart() {
		return decimalPart;
	}

	public void setDecimalPart(String decimalPart) {
		this.decimalPart = decimalPart;
	}

	/**
	 * Gets the currency symbol.
	 * 
	 * @return
	 */
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	String decimalPart;
	String currency;
}