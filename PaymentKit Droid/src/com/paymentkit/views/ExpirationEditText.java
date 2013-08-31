package com.paymentkit.views;

import java.util.Calendar;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import com.paymentkit.views.FieldHolder.CardEntryListener;

/**
 * 
 * @author bweinstein
 * 
 */
public class ExpirationEditText extends EditText {

	private static final String TAG = ExpirationEditText.class.getSimpleName();

	private CardEntryListener mListener;

	public ExpirationEditText(Context context) {
		super(context);
		setup();
	}

	public ExpirationEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	private void setup() {
		addTextChangedListener(mTextWatcher);
	}

	public void setCardEntryListener(CardEntryListener listener) {
		mListener = listener;
	}

	/* Expiration Input Field Text Watcher */
	private boolean mTextAdded = true; // denotes if a character was added or
										// removed in last text change
	private int mPrevLength = 0;

	TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (mTextAdded && length() == 1) {

				// tens place of month validation
				int tensPlace = Integer.parseInt(getText().subSequence(0, 1)
						.toString());
				if (tensPlace > 1) {
					setText("");
					setSelection(length());
				}

			} else if (mTextAdded && length() == 2) {

				// one places of month validation
				int tensPlace = Integer.parseInt(getText().subSequence(0, 1)
						.toString());
				int onesPlace = Integer.parseInt(getText().subSequence(1, 2)
						.toString());
				if ((tensPlace == 1 && onesPlace > 2)
						|| (tensPlace == 0 && onesPlace == 0)) {
					setText(getText().subSequence(0, 1));
					setSelection(length());
				}

			} else if (mTextAdded && length() == 4) {

				// tens place of year validation
				Integer year = Calendar.getInstance().get(Calendar.YEAR); // 2013
				int curTensPlace = Integer.parseInt(year.toString().substring(
						2, 3)); // 1
				int inputTensPlace = Integer.parseInt(Character
						.toString(getText().charAt(3))); // 5

				if (inputTensPlace < curTensPlace) {
					setText(getText().subSequence(0, length() - 1));
					setSelection(length());
				}

			} else if (mTextAdded && length() == 5) {

				// ones place of year validation
				Integer year = Calendar.getInstance().get(Calendar.YEAR); // 2013
				int curMonth = Calendar.getInstance().get(Calendar.MONTH);
				int curYear = Integer.parseInt(year.toString().substring(2, 4));
				int inputMonth = Integer.parseInt(getText().toString()
						.substring(0, 2));
				int inputYear = Integer.parseInt(getText().toString()
						.substring(3, 5));
				if (inputYear < curYear
						|| ((inputMonth < curMonth) && inputYear == curYear)) {
					setText(getText().subSequence(0, 4));
					setSelection(length());
				} else {
					mListener.onCVVEntry();
				}

			} else if (!mTextAdded && (length() == 3)) {

				setText(getText().subSequence(0, 2));
				setSelection(length());

			} else if (mTextAdded && length() == 3
					&& getText().charAt(2) != '/') {

				setText(getText().subSequence(0, 2) + "/" + getText().charAt(2));
				setSelection(length());

			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			mPrevLength = length();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (length() - mPrevLength > 0) {
				mTextAdded = true;
			} else {
				mTextAdded = false;
			}
		}
	};

	public String getMonth() {
		String text = getText().toString();
		int index = text.indexOf("/");
		return text.substring(0, index);
	}

	public String getYear() {
		String text = getText().toString();
		int index = text.indexOf("/");
		String yearStr = "20" + text.substring(index + 1, text.length());
		return yearStr;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new ZanyInputConnection(super.onCreateInputConnection(outAttrs),
				true);
	}

	/*
	 * See "android EditText delete(backspace) key event" on stackoverflow
	 */
	private class ZanyInputConnection extends InputConnectionWrapper {

		public ZanyInputConnection(InputConnection target, boolean mutable) {
			super(target, mutable);
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN
					&& event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
				if(getSelectionStart() == 0) {
					mListener.onReEntry();
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
				return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DEL))
						&& sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
								KeyEvent.KEYCODE_DEL));
			}

			return super.deleteSurroundingText(beforeLength, afterLength);
		}
	}

}
