package com.paymentkit.views;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.paymentkit.views.FieldHolder.CardEntryListener;


public class CardNumEditText extends EditText {

	private static final String TAG = CardNumEditText.class.getSimpleName();

	private CardEntryListener mCardEntryListener;

	private int mMaxCardLength = FieldHolder.NON_AMEX_CARD_LENGTH;

	public CardNumEditText(Context context) {
		super(context);
		setup();
	}

	public CardNumEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	private void setup() {
		addTextChangedListener(mCardNumberTextWatcher);
	}

	public void setCardEntryListener(CardEntryListener listener) {
		mCardEntryListener = listener;
	}
	
	public int getMaxCardLength() {
		return mMaxCardLength;
	}

	public void setMaxCardLength(int maxCardLength) {
		mMaxCardLength = maxCardLength;
		InputFilter[] filters = new InputFilter[1];
		filters[0] = new InputFilter.LengthFilter(maxCardLength);
		setFilters(filters);
	}

	/* Card Number Input Field Text Watcher */
	private boolean mTextAdded = true;
	private int mPrevLength = 0;

	TextWatcher mCardNumberTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			setTextColor(Color.DKGRAY);
			mCardEntryListener.onEdit();
			int l = length();
			if ((l == 4 || l == 9 || l == 14) && mTextAdded) {
				setText(getText() + " ");
				setSelection(l + 1);
			} else if ((l == 5 || l == 10 || l == 15) && mTextAdded && getText().charAt(length() - 1) != ' ') {
				String text = getText().subSequence(0, length() - 1) + " " + getText().charAt(length() - 1);
				setText(text);
				setSelection(length());
			} else if (l == mMaxCardLength && mTextAdded) {
				mCardEntryListener.onComplete();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			mPrevLength = length();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (length() - mPrevLength > 0) {
				mTextAdded = true;
			} else {
				mTextAdded = false;
			}
		}
	};

	public String getLast4Digits() {
		String text = getText().toString().replaceAll("\\s", "");
		return text.substring(text.length() - 4, text.length());
	}

}
