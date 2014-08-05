package com.paymentkit.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import com.nineoldandroids.animation.ObjectAnimator;
import com.paymentkit.util.AnimUtils;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.FieldHolder.CardEntryListener;

import java.util.Calendar;

/**
 * 
 * @author Brendan Weinstein
 * http://www.brendanweinstein.me
 *
 */
public class ExpirationEditText extends EditText {

	private static final String TAG = ExpirationEditText.class.getSimpleName();

	private CardEntryListener mListener;
    private ObjectAnimator shakeAnim;

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

	@Override
	public void onSelectionChanged(int start, int end) {
        int monthLength = getMonth().length();
        // We only care about the end
        if (end <= monthLength) {
            setSelection(monthLength);
            super.onSelectionChanged(monthLength, monthLength);
        } else {
            setSelection(length());
            super.onSelectionChanged(length(), length());
        }
	}

	public void setCardEntryListener(CardEntryListener listener) {
		mListener = listener;
	}

	/* Expiration Input Field Text Watcher */
	private boolean mTextAdded = true;
	private int mPrevLength = 0;

	TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
            removeTextChangedListener(this);

            validateAndFormatText(s);

            addTextChangedListener(this);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			mPrevLength = length();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
            mTextAdded = length() - mPrevLength > 0;
		}


        private void validateAndFormatText(Editable s) {
            CharSequence month = validateMonth(getMonth());
            CharSequence year = validateYear(getYearAbv());

            if (didValidationPass(month, year)) {
                mListener.onCVVEntry();
            } else if (month.length() == 2 || !TextUtils.isEmpty(year)) {
                ViewUtils.replaceAllText(s, month + "/" + year);
            } else {
                ViewUtils.replaceAllText(s, month);
            }
            // If the month len is 2 that means its good so send them to the year.
            if (month.length() == 2) setSelection(s.length());
        }

        private boolean didValidationPass(CharSequence month, CharSequence year) {
            return month.length() == 2 && year.length() == 2;
        }


        private CharSequence validateMonth(String monthStr) {
            if (TextUtils.isEmpty(monthStr)) return "";
            // tens place of month validation
            int tensPlace = Integer.parseInt(getText().subSequence(0, 1).toString());
            if (mTextAdded && monthStr.length() == 1) {
                if (tensPlace > 1) {
                    return truncateAndIndicateInvalid("");
                }
            } else if (mTextAdded && length() == 2) {
                // one places of month validation
                int onesPlace = Integer.parseInt(getText().subSequence(1, 2).toString());
                if ((tensPlace == 1 && onesPlace > 2) || (tensPlace == 0 && onesPlace == 0)) {
                    return truncateAndIndicateInvalid(monthStr);
                }
            }
            return monthStr;
        }

        private CharSequence validateYear(String yearStr) {
            if (mTextAdded && yearStr.length() == 1) {
                // tens place of year validation
                Integer year = Calendar.getInstance().get(Calendar.YEAR);
                int curTensPlace = Integer.parseInt(year.toString().substring(2, 3));
                int inputTensPlace = Integer.parseInt(yearStr.substring(0, 1));

                if (inputTensPlace < curTensPlace) {
                    return truncateAndIndicateInvalid("");
                }

            } else if (mTextAdded && yearStr.length() == 2) {

                // ones place of year validation
                int curMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
                int curYear = Calendar.getInstance().get(Calendar.YEAR) - 2000;
                int inputYear = Integer.parseInt(yearStr);
                if (!TextUtils.isEmpty(getMonth())) {
                    int inputMonth = Integer.parseInt(getMonth());
                    if (inputYear < curYear || ((inputMonth < curMonth) && inputYear == curYear)) {
                        return truncateAndIndicateInvalid(yearStr);
                    }
                } else {
                    if (inputYear < curYear) {
                        return truncateAndIndicateInvalid(yearStr);
                    }
                }
            }
            return yearStr;
        }
	};

    private CharSequence truncateAndIndicateInvalid(String string) {
        indicateInvalidDate();
        if (string != null && string.length() > 0) {
            return string.subSequence(0, 1);
        }
        return "";
    }

    public String getMonth() {
		String text = getText().toString();
		int index = text.indexOf("/");
        if (index != -1) {
            return text.substring(0, index);
        } else {
            return text;
        }
	}

    public String getYearAbv() {
        String text = getText().toString();
        int index = text.indexOf("/");
        if (index != -1 && index + 1 < length()) {
            return text.substring(index + 1);
        } else {
            return "";
        }
    }

	public String getYear() {
		String text = getText().toString();
        int index = text.indexOf("/");
		String yearStr = "20" + text.substring(index + 1, text.length());
		return yearStr;
	}

    public boolean isValid() {
        return getText().toString().length() == 5;
    }

    public void indicateInvalidDate() {
        if (shakeAnim != null) shakeAnim.end();
        shakeAnim = AnimUtils.getShakeAnimation(this, true);
        shakeAnim.start();
    }

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new ZanyInputConnection(super.onCreateInputConnection(outAttrs), true);
	}

    public void setTextWithoutValidation(CharSequence text) {
        removeTextChangedListener(mTextWatcher);

        ViewUtils.replaceAllText(getText(), text);

        addTextChangedListener(mTextWatcher);
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
			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                handleDelete();
			}
			return super.sendKeyEvent(event);
		}

        private void handleDelete() {
            if (getSelectionEnd() == 0) {
                mListener.onCardNumberInputReEntry();
            }
            int index = getText().toString().indexOf("/");
            if (index != -1 && getSelectionEnd() == (index + 1) && length() == (index + 1)){
                removeTextChangedListener(mTextWatcher);
                ViewUtils.replaceAllText(getText(), getText().toString().substring(0, length() - 1));
                addTextChangedListener(mTextWatcher);
            }
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
}
