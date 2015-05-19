package com.paymentkit.views;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.TextView;

import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.FieldHolder.CardEntryListener;

public class CVVEditText extends EditText {
	
	private static final String TAG = CVVEditText.class.getSimpleName();
	
	private CardEntryListener mListener;

	public CVVEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}
	
	public CVVEditText(Context context) {
		super(context);
		setup();
	}
	
	private void setup() {
		addTextChangedListener(mTextWatcher);
		setOnFocusChangeListener(mFocusListener);
		//setOnEditorActionListener(mEditorActionListener);
	}
	
	public void setCardEntryListener(CardEntryListener listener) {
		mListener = listener;
	}
	
	private OnEditorActionListener mEditorActionListener = new EditText.OnEditorActionListener() {
	    @Override
	    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	        if (actionId == EditorInfo.IME_ACTION_DONE ||
	                event.getAction() == KeyEvent.ACTION_DOWN &&
	                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
	        	clearFocus();
	        	ViewUtils.hideSoftKeyboard((Activity)getContext());
	            return true;
	        }
	        return false;
	    }
	};
	
	private OnFocusChangeListener mFocusListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if(hasFocus) {
				mListener.onCVVEntry();
			} else {
				mListener.onCVVEntryComplete();
			}
		}
	};
	
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
					mListener.onBackFromCVV();
					return false;
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
	
	private TextWatcher mTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() == FieldHolder.CVV_MAX_LENGTH) {
				//mListener.onCVVEntryComplete();
				ViewUtils.hideSoftKeyboard((Activity)getContext());
				clearFocus();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	};

}
