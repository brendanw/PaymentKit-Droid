package com.paymentkit;

/*
 * [ValidateCreditCard.java]
 * 
 * PaymnetKit Note: Below is original documentation by Roedy Green. Some parts of this code
 * have been modified for PaymentKit.
 *
 * Summary: Handles calculations to validate credit card numbers and determine which credit card company they belong to.
 *
 * Copyright: (c) 1999-2013 Roedy Green, Canadian Mind Products, http://mindprod.com
 *
 * Licence: This software may be copied and used freely for any purpose but military.
 *          http://mindprod.com/contact/nonmil.html
 *
 * Requires: JDK 1.6+
 *
 * Created with: JetBrains IntelliJ IDEA IDE http://www.jetbrains.com/idea/
 *
 * Version History:
 *  1.0 1999-08-17 posted to comp.lang.java.programmer
 *  1.1 1999-08-17 add vendorToString, rename isValid
 *                 implement patterns for 13-16 digit numbers for toPrettyString
 *  1.2 1999-08-17 separate enumerations for too few and too many digits.
 *  1.3 1999-08-19 ignore dashes in numbers
 */

import static java.lang.System.out;

/**
 * Handles calculations to validate credit card numbers and determine which
 * credit card company they belong to.
 * 
 * 1. if a credit card number is valid 2. which credit card vendor handles that
 * number.
 * 
 * It validates the prefix and the checkdigit. It does not* contact the credit
 * card company to ensure that number has actually been issued and that the
 * account is in good standing.
 * 
 * It will also tell you which of the following credit card companies issued the
 * card: Amex, Diners Club, Carte Blanche, Discover, enRoute, JCB, MasterCard or
 * Visa.
 * 
 * @author Roedy Green, Canadian Mind Products
 * @version 1.3 1999-08-19 ignore dashes in numbers
 * @since 1999-08-17
 */
public final class ValidateCreditCard {

	private static final String TAG = ValidateCreditCard.class.getSimpleName();
	// ------------------------------ CONSTANTS ------------------------------

	/**
	 * true if debugging output wanted
	 */
	private static final boolean DEBUGGING = false;

	/**
	 * Used to speed up findMatchingRange by caching the last hit.
	 */
	private static int cachedLastFind;

	/**
	 * ranges of credit card number that belong to each company. buildRanges
	 * initialises.
	 */
	private static LCR[] ranges;

	// -------------------------- PUBLIC STATIC METHODS --------------------------

	/**
	 * Determine if the credit card number is valid, i.e. has good prefix and
	 * checkdigit. Does _not_ ask the credit card company if this card has been
	 * issued or is in good standing.
	 * 
	 * @param creditCardNumber
	 *          number on card.
	 * 
	 * @return true if card number is good.
	 */
	public static boolean isValid(long creditCardNumber) {
		CardType cardType = matchCardType(creditCardNumber);
		if (cardType.isError()) {
			return false;
		} else {
			{
				// there is a checkdigit to be validated
				/*
				 * Manual method MOD 10 checkdigit 706-511-227 7 0 6 5 1 1 2 2 7 2 * 2 *
				 * 2 * 2 --------------------------------- 7 + 0 + 6 +1+0+ 1 + 2 + 2 + 4
				 * = 23 23 MOD 10 = 3 10 - 3 = 7 -- the check digit Note digits of
				 * multiplication results must be added before sum. Computer Method MOD
				 * 10 checkdigit 706-511-227 7 0 6 5 1 1 2 2 7 Z Z Z Z
				 * --------------------------------- 7 + 0 + 6 + 1 + 1 + 2 + 2 + 4 + 7 =
				 * 30 30 MOD 10 had better = 0
				 */
				long number = creditCardNumber;
				int checksum = 0;
				// work right to left
				for (int place = 0; place < 16; place++) {
					int digit = (int) (number % 10);
					number /= 10;
					if ((place & 1) == 0) {
						// even position (0-based from right), just add digit
						checksum += digit;
					} else {// odd position (0-based from right), must double
									// and add
						checksum += z(digit);
					}
					if (number == 0) {
						break;
					}
				}// end for
					// good checksum should be 0 mod 10
				return (checksum % 10) == 0;
			}
		}
	}

	public static CardType getCardType(String number) {
		number = numericOnlyString(number);
		if (number.toString().length() < 2)
			return CardType.UNKNOWN_CARD;
		String firstChars = number.toString().substring(0, 2);
		int range = Integer.parseInt(firstChars);
		if (range >= 40 && range <= 49) {
			return CardType.VISA;
		} else if (range >= 50 && range <= 59) {
			return CardType.MASTERCARD;
		} else if (range == 34 || range == 37) {
			return CardType.AMERICAN_EXPRESS;
		} else if (range == 60 || range == 62 || range == 64 || range == 65) {
			return CardType.DISCOVER;
		} else if (range == 35) {
			return CardType.JCB;
		} else if (range == 30 || range == 36 || range == 38 || range == 39) {
			return CardType.DINERS_CLUB;
		} else {
			return CardType.UNKNOWN_CARD;
		}
	}

	/**
	 * Finds a matching range in the ranges array for a given creditCardNumber.
	 * 
	 * @param creditCardNumber
	 *          number on card.
	 * 
	 * @return index of matching range, or NOT_ENOUGH_DIGITS or UNKNOWN_VENDOR on
	 *         failure.
	 */
	public static CardType matchCardType(long creditCardNumber) {
		if (creditCardNumber < 1000000000000L) {
			return CardType.NOT_ENOUGH_DIGITS;
		}
		if (creditCardNumber > 9999999999999999L) {
			return CardType.TOO_MANY_DIGITS;
		}
		// check the cached index first, where we last found a number.
		if (ranges[cachedLastFind].low <= creditCardNumber && creditCardNumber <= ranges[cachedLastFind].high) {
			return ranges[cachedLastFind].cardType;
		}
		for (int i = 0; i < ranges.length; i++) {
			if (ranges[i].low <= creditCardNumber && creditCardNumber <= ranges[i].high) {
				// we have a match
				cachedLastFind = i;
				return ranges[i].cardType;
			}
		}// end for
		return CardType.UNKNOWN_CARD;
	}// end matchVendor

	/**
	 * convert a String to a long. The routine is very forgiving. It ignores
	 * invalid chars, lead trail, embedded spaces, decimal points etc, AND minus
	 * signs.
	 * 
	 * @param numStr
	 *          the String containing the number to be converted to long.
	 * 
	 * @return long value of the string found, ignoring junk characters. May be
	 *         negative.
	 * @throws NumberFormatException
	 *           if the number is too big to fit in a long.
	 * @see com.mindprod.common11.ST#parseDirtyLong(String)
	 */
	public static long parseDirtyLong(String numStr) {
		numStr = numericOnlyString(numStr);
		// strip commas, spaces, + etc, AND -
		// StringBuilder is better than FastCat for char by char work.
		StringBuilder b = new StringBuilder(numStr.length());
		for (int i = 0, n = numStr.length(); i < n; i++) {
			char c = numStr.charAt(i);
			if ('0' <= c && c <= '9') {
				b.append(c);
			}
		}// end for
		numStr = b.toString();
		if (numStr.length() == 0) {
			return 0;
		}
		return Long.parseLong(numStr);
	}// end parseDirtyLong

    public static String numericOnlyString(String string) {
        return string.replaceAll("[\\D]", "");
    }

	// From http://www.icverify.com/
	// Vendor Prefix len checkdigit
	// MASTERCARD 51-55 16 mod 10
	// VISA 4 13, 16 mod 10
	// AMEX 34,37 15 mod 10
	// Diners Club/
	// Carte Blanche
	// 300-305 14
	// 36 14
	// 38 14 mod 10
	// Discover 6011 16 mod 10
	// enRoute 2014 15
	// 2149 15 any
	// JCB 3 16 mod 10
	// JCB 2131 15
	// 1800 15 mod 10

	/**
	 * Convert a creditCardNumber as long to a formatted String. Currently it
	 * breaks 16-digit numbers into groups of 4.
	 * 
	 * @param creditCardNumber
	 *          number on card.
	 * 
	 * @return String representation of the credit card number.
	 */
	public static String toPrettyString(long creditCardNumber) {
		String plain = Long.toString(creditCardNumber);
		// int i = findMatchingRange(creditCardNumber);
		int length = plain.length();
		switch (length) {
		case 12:
			// 12 pattern 3-3-3-3
			return plain.substring(0, 3) + ' ' + plain.substring(3, 6) + ' ' + plain.substring(6, 9) + ' ' + plain.substring(9, 12);
		case 13:
			// 13 pattern 4-3-3-3
			return plain.substring(0, 4) + ' ' + plain.substring(4, 7) + ' ' + plain.substring(7, 10) + ' ' + plain.substring(10, 13);
		case 14:
			// 14 pattern 2-4-4-4
			return plain.substring(0, 2) + ' ' + plain.substring(2, 6) + ' ' + plain.substring(6, 10) + ' ' + plain.substring(10, 14);
		case 15:
			// 15 pattern 3-4-4-4
			return plain.substring(0, 3) + ' ' + plain.substring(3, 7) + ' ' + plain.substring(7, 11) + ' ' + plain.substring(11, 15);
		case 16:
			// 16 pattern 4-4-4-4
			return plain.substring(0, 4) + ' ' + plain.substring(4, 8) + ' ' + plain.substring(8, 12) + ' ' + plain.substring(12, 16);
		case 17:
			// 17 pattern 1-4-4-4-4
			return plain.substring(0, 1) + ' ' + plain.substring(1, 5) + ' ' + plain.substring(5, 9) + ' ' + plain.substring(9, 13) + ' '
					+ plain.substring(13, 17);
		default:
			// 0..11, 18+ digits long
			// plain
			return plain;
		}// end switch
	}// end toPrettyString

	// -------------------------- STATIC METHODS --------------------------

	static {
		// now that all enum constants defined
		buildRanges();
	}

	/**
	 * build table of which ranges of credit card number belong to which vendor
	 */
	private static void buildRanges() {
		// careful, no lead zeros allowed
		// low high len vendor
		ranges = new LCR[] { new LCR(4000000000000L, 4999999999999L/* 13 */, CardType.VISA),
				new LCR(340000000000000L, 349999999999999L/* 15 */, CardType.AMERICAN_EXPRESS),
				new LCR(370000000000000L, 379999999999999L/* 15 */, CardType.AMERICAN_EXPRESS),
				new LCR(4000000000000000L, 4999999999999999L/* 16 */, CardType.VISA),
				new LCR(5100000000000000L, 5599999999999999L/* 16 */, CardType.MASTERCARD),
				new LCR(6011000000000000L, 6011999999999999L/* 16 */, CardType.DISCOVER) };
	}

	/**
	 * used in computing checksums, doubles and adds resulting digits.
	 * 
	 * @param digit
	 *          the digit to be doubled, and digit summed.
	 * 
	 * @return // 0->0 1->2 2->4 3->6 4->8 5->1 6->3 7->5 8->7 9->9
	 */
	private static int z(int digit) {
		if (digit == 0) {
			return 0;
		} else {
			return (digit * 2 - 1) % 9 + 1;
		}
	}

	// --------------------------- main() method ---------------------------

	/**
	 * Test driver
	 * 
	 * @param args
	 *          not used
	 */
	public static void main(String[] args) {
		if (DEBUGGING) {
			out.println(matchCardType(0));// not enough digits
			out.println(matchCardType(6011222233334444L));// Discover
			out.println(matchCardType(6010222233334444L));// unknown vendor
			out.println(matchCardType(4000000000000L));// Visa
			out.println(matchCardType(4999999999999L));// Visa
			out.println(isValid(0));// false
			out.println(isValid(6010222233334444L));// false
			out.println(isValid(4000000000000L));// false
			out.println(isValid(4000000000006L));// true
			out.println(isValid(4000000000009L));// false
			out.println(isValid(4999999999999L));// false
			out.println(isValid(378888888888858L));// true, Amex
			out.println(isValid(4888888888888838L));// true, Visa;
			out.println(isValid(5588888888888838L));// true, MC
			out.println(isValid(6011222233334444L));// true, Discover
			out.println(parseDirtyLong("123,444 999=z/99"));// 12344499999
			out.println(toPrettyString(0));// 0
			out.println(toPrettyString(6011222233334444L));// 6011 2222 3333 4444
			out.println(toPrettyString(6010222233334444L));// 6010 2222 3333 4444
			out.println(toPrettyString(4000000000000L));// 4000 000 000 000
			out.println(toPrettyString(4000000000006L));// 4000 000 000 006
			out.println(toPrettyString(4000000000009L));// 4000 000 000 009
			out.println(toPrettyString(3123456789012341L));// 3123 4567 8901 2341
			out.println(toPrettyString(999999999990L));// 999 999 999 990
			out.println(toPrettyString(4000000000006L));// 4000 000 000 006
			out.println(toPrettyString(30000000000004L));// 30 0000 0000 0004
			out.println(toPrettyString(180000000000002L));// 180 0000 0000 0002
			out.println(toPrettyString(3000000000000004L));// 3000 0000 0000 0004
			out.println(toPrettyString(3000000000000005L));// 3000 0000 0000 0005
			out.println(toPrettyString(13000000000000005L));// 1 3000 0000 0000 0005
			out.println(CardType.VISA.getName());// Visa
			out.println(CardType.UNKNOWN_CARD.getName());// Error: unknown credit card
																										// company
		}// end if debugging
	}// end main
}

/**
 * Describes a single Legal Card Range
 */
final class LCR {
	// ------------------------------ FIELDS ------------------------------

	/**
	 * enumeration credit card service
	 */
	public final CardType cardType;

	/**
	 * low and high bounds on range covered by this vendor
	 */
	public final long high;

	/**
	 * low bounds on range covered by this vendor
	 */
	public final long low;

	// -------------------------- PUBLIC INSTANCE METHODS
	// --------------------------

	/**
	 * public constructor
	 * 
	 * @param low
	 *          lowest credit card number in range.
	 * @param high
	 *          highest credit card number in range
	 * @param cardType
	 *          enum constant for vendor
	 */
	public LCR(long low, long high, CardType cardType) {
		this.low = low;
		this.high = high;
		this.cardType = cardType;
	}// end public constructor
}
