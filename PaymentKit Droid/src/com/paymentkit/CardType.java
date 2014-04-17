package com.paymentkit;

public enum CardType {
	VISA(1, "Visa", false, 3),
	MASTERCARD(2, "Mastercard", false, 3), 
	AMERICAN_EXPRESS(3, "American Express", false, 4),
	DISCOVER(4,"Discover", false, 3),
	JCB(5, "JCB", false, 3),
	DINERS_CLUB(6, "Diners Club", false, 3),
	UNKNOWN_CARD(7, "Unknown", true, 3),
	TOO_MANY_DIGITS(8, "Too Many Digits", true, 3),
	NOT_ENOUGH_DIGITS(9, "Not Enough Digits", true, 3);

	private int mVal;
	private String mName;
	private boolean mIsError;
	private int mMaxCVVLength;

	CardType(int val, String name, boolean isError, int maxCVVLength) {
		this.mVal = val;
		this.mName = name;
		this.mIsError = isError;
	}

	public int getValue() {
		return mVal;
	}

	public String getName() {
		return mName;
	}

	public boolean isError() {
		return mIsError;
	}
	
	public int getMaxCVVLength() {
		return mMaxCVVLength;
	}

	public static CardType fromString(String text) {
		if (text != null) {
			int num = Integer.parseInt(text);
			for (CardType c : CardType.values()) {
				if (c.mVal == num) {
					return c;
				}
			}
		}
		return CardType.UNKNOWN_CARD;
	}

}