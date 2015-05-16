package com.paymentkit;

public class PaymentCard {
	
	private String mLastFourDigits;
	private CardType mCardType;
	private String mExpMonth;
	private String mExpYear;
	
	public String getExpirationMonth() {
		return mExpMonth;
	}
	
	public String getExpirationYear() {
		return mExpYear;
	}
	
	public CardType getCardType() {
		return mCardType;
	}
	
	public String getLastFourDigits() {
		return mLastFourDigits;
	}

}
