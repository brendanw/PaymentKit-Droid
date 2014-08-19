package com.paymentkit.views;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import com.paymentkit.ValidateCreditCard;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.FieldHolder.CardEntryListener;

public class CardNumEditText extends EditText {

	private static final String TAG = CardNumEditText.class.getSimpleName();

	private CardEntryListener mCardEntryListener;

	private int mMaxCardLength = FieldHolder.NON_AMEX_CARD_LENGTH;
    private ZanyInputConnection mInputConnection;

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
        setOnKeyListener(new ZanyKeyListener());
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

    public String getLast4Digits() {
        String text = getText().toString().replaceAll("\\s", "");
        return text.substring(text.length() - 4, text.length());
    }

    public void setTextWithoutValidation(CharSequence text) {
        removeTextChangedListener(mCardNumberTextWatcher);

        ViewUtils.replaceAllText(getText(), text);

        addTextChangedListener(mCardNumberTextWatcher);
    }

	TextWatcher mCardNumberTextWatcher = new TextWatcher() {
        /* Card Number Input Field Text Watcher */
        private boolean mTextAdded = true;
        private int mPrevLength = 0;

		@Override
		public void afterTextChanged(Editable s) {
            // Notify listener, will change card type.
            mCardEntryListener.onEdit();

            // Remove our selves while we edit this text.
            removeTextChangedListener(this);

            int oldSelectionPosition = getSelectionEnd();
            boolean removedTwoFromEnd = formatText(s);
            fixSelectionPosition(s, oldSelectionPosition, removedTwoFromEnd);

            if (isCardNumberInputComplete()) {
                mCardEntryListener.onCardNumberInputComplete();
            }

            addTextChangedListener(this);
		}

        private boolean isCardNumberInputComplete() {
            return length() == mMaxCardLength && mTextAdded;
        }

        /** This fixes the awkwardness when the user adds or deletes around a space. **/
		private void fixSelectionPosition(Editable s, int oldPos, boolean removedTwoFromEnd) {
            int newPos = getSelectionEnd();
            if (removedTwoFromEnd) {
                setSelection(newPos + 1);
                return;
            }

            if (cursorIsOnASpace(oldPos, newPos, s)) {
                if (mTextAdded) {
                    setSelection(newPos + 1);
                } else {
                    setSelection(newPos - 1);
                }
            }
		}

        private boolean cursorIsOnASpace(int oldPos, int newPos, Editable s) {
            String str = s.toString();
            String selected =  newPos > 0 ? str.substring(newPos - 1, newPos) : "";
            return oldPos == newPos && " ".equals(selected);
        }

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			mPrevLength = length();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		    mTextAdded = length() - mPrevLength > 0;
		}
	};

    /////////////////////
    // Formatting Methods
    /////////////////////

    /** @return true if the char in the last group was moved to an earlier group. **/
    private boolean formatText(Editable editable) {
        String oldString = editable.toString();

        String newString = null;
        String strippedString = ValidateCreditCard.numericOnlyString(oldString);
        if(mMaxCardLength == FieldHolder.NON_AMEX_CARD_LENGTH) {
            newString = format16Text(strippedString);
        } else if(mMaxCardLength == FieldHolder.AMEX_CARD_LENGTH) {
            newString = format15Text(strippedString);
        }

        if (newString != null) {
            ViewUtils.replaceAllText(editable, newString);
        }
        return haveLastMemberOfGroupBeenRemoved(newString, oldString);
    }

    private boolean haveLastMemberOfGroupBeenRemoved(String newString, String oldString) {
        return newString.length() + 1 == oldString.length()
                && oldString.length() > 2
                && oldString.substring(oldString.length() - 2, oldString.length() -1).equals(" ");
    }

    /*
     * 4-4-4-4
     */
    private String format16Text(String strippedStr) {
		int len = strippedStr.length();
		StringBuilder sb = new StringBuilder();
		for (int lh = 1; lh <= len; lh++) {
			sb.append(strippedStr.charAt(lh - 1));
			if (lh % 4 == 0 && lh < 16 && lh != len) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	/*
	 * 4-6-5
	 */
	private String format15Text(String strippedStr) {
		int len = strippedStr.length();
		StringBuilder sb = new StringBuilder();
		for(int lh=1; lh <= len; lh++) {
			sb.append(strippedStr.charAt(lh-1));
			if((lh == 4 || lh == 10) && lh != len) {
				sb.append(" ");
			}
		}
        return sb.toString();
	}

    /////////////////
    // Input Methods
    /////////////////

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        mInputConnection = new ZanyInputConnection(super.onCreateInputConnection(outAttrs), true);;
		return mInputConnection;
	}

    /**
     * invoked when a hardware key event is dispatched to this view.
     * We will make all calls to our input connection.
     */
    private class ZanyKeyListener implements OnKeyListener {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            boolean shouldConsume = false;
            if (event.getAction() == KeyEvent.ACTION_DOWN && mInputConnection != null) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DEL: shouldConsume = mInputConnection.handleDelete(); break;
                    // On a hardware keyboard IME_ACTION_NEXT will come as an enter key.
                    case KeyEvent.KEYCODE_ENTER: shouldConsume = mInputConnection.handleNextPress(); break;
                }
            }
            // soft-keyboard uses downs while hard uses ups.
            return shouldConsume || event.getAction() == KeyEvent.ACTION_UP;
        }
    }

    private class ZanyInputConnection extends InputConnectionWrapper {

		public ZanyInputConnection(InputConnection target, boolean mutable) {
			super(target, mutable);
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
            boolean shouldConsume = false;
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DEL: shouldConsume = handleDelete(); break;
                    // On a hardware keyboard IME_ACTION_NEXT may come as an enter key.
                    case KeyEvent.KEYCODE_ENTER: shouldConsume = handleNextPress(); break;
                }
			}
			return shouldConsume ? true : super.sendKeyEvent(event);
		}

        @Override
        public boolean performEditorAction(int editorAction) {
            boolean shouldConsume = false;
            switch (editorAction) {
                case EditorInfo.IME_ACTION_NEXT: shouldConsume = handleNextPress(); break;
            }
            return shouldConsume ? true : super.performEditorAction(editorAction);
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

        private boolean handleNextPress() {
            // User has requested that we validate their card number.
            mCardEntryListener.onCardNumberInputComplete();
            return true;
        }

        private boolean handleDelete() {
            int curPos = getSelectionEnd();

            if (mMaxCardLength == FieldHolder.NON_AMEX_CARD_LENGTH && (curPos == 5 || curPos == 10 || curPos == 15)) {
                CardNumEditText.this.setSelection(curPos - 1);
                return true;
            } else if(mMaxCardLength == FieldHolder.AMEX_CARD_LENGTH && (curPos == 5 || curPos == 12)) {
                CardNumEditText.this.setSelection(curPos - 1);
                return true;
            }
            return false;
        }
	}
}
