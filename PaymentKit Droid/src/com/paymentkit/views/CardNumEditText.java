package com.paymentkit.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
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

	enum TextEvent {
		KEY_PRESS, FORMATTER
	};

	private TextEvent mLastEvent = TextEvent.KEY_PRESS;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		mLastEvent = TextEvent.KEY_PRESS;
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	TextWatcher mCardNumberTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			setTextColor(Color.DKGRAY);
			mCardEntryListener.onEdit();
			if (length() == mMaxCardLength && mTextAdded) {
				mCardEntryListener.onCardNumberInputComplete();
			}
			if (mLastEvent == TextEvent.KEY_PRESS) {
				int curPos = getSelectionEnd();
				formatText(getText().toString().replaceAll(" ", ""));
				positionCursor(curPos);
			}
		}
		
		private void formatText(String strippedStr) {
			if(mMaxCardLength == FieldHolder.NON_AMEX_CARD_LENGTH) {
				format16Text(strippedStr);
			} else if(mMaxCardLength == FieldHolder.AMEX_CARD_LENGTH) {
				format15Text(strippedStr);
			}
		}

		private void positionCursor(int curPos) {
			if (mMaxCardLength == FieldHolder.NON_AMEX_CARD_LENGTH) {
				if (mTextAdded && (curPos == 4 || curPos == 9 || curPos == 14)) {
					setSelection(curPos + 1);
				} else {
					setSelection(curPos);
				}
			} else if(mMaxCardLength == FieldHolder.AMEX_CARD_LENGTH) {
				if (mTextAdded && (curPos == 4 || curPos == 11)) {
					setSelection(curPos + 1);
				} else {
					setSelection(curPos);
				}
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

	/*
	 * 4-4-4-4
	 */
	private void format16Text(String strippedStr) {
		int len = strippedStr.length();
		StringBuilder sb = new StringBuilder();
		for (int lh = 1; lh <= len; lh++) {
			sb.append(strippedStr.charAt(lh - 1));
			if (lh % 4 == 0 && lh < 16) {
				sb.append(" ");
			}
		}
		mLastEvent = TextEvent.FORMATTER;
		setText(sb.toString());
	}

	/*
	 * 4-6-5
	 */
	private void format15Text(String strippedStr) {
		int len = strippedStr.length();
		StringBuilder sb = new StringBuilder();
		for(int lh=1; lh <= len; lh++) {
			sb.append(strippedStr.charAt(lh-1));
			if(lh == 4 || lh == 10) {
				sb.append(" ");
			}
		}
		mLastEvent = TextEvent.FORMATTER;
		setText(sb.toString());
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new ZanyInputConnection(super.onCreateInputConnection(outAttrs), true);
	}

	private class ZanyInputConnection extends InputConnectionWrapper {

		public ZanyInputConnection(InputConnection target, boolean mutable) {
			super(target, mutable);
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
				int curPos = getSelectionEnd();
				mLastEvent = TextEvent.KEY_PRESS;
				if (mMaxCardLength == FieldHolder.NON_AMEX_CARD_LENGTH && (curPos == 5 || curPos == 10 || curPos == 15)) {
					CardNumEditText.this.setSelection(curPos - 1);
					return true;
				} else if(mMaxCardLength == FieldHolder.AMEX_CARD_LENGTH && (curPos == 5 || curPos == 12)) {
					CardNumEditText.this.setSelection(curPos - 1);
					return true;
				}
			}
			return super.sendKeyEvent(event);
		}

		@Override
		public boolean deleteSurroundingText(int beforeLength, int afterLength) {
			// magic: in latest Android, deleteSurroundingText(1, 0) will be
			// called for backspace
			if (beforeLength == 1 && afterLength == 0) {
				// backspace
				return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
						&& sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
			}

			return super.deleteSurroundingText(beforeLength, afterLength);
		}
	}

	public String getLast4Digits() {
		String text = getText().toString().replaceAll("\\s", "");
		return text.substring(text.length() - 4, text.length());
	}

}
