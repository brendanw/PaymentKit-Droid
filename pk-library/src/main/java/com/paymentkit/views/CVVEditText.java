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

import com.nineoldandroids.animation.ObjectAnimator;
import com.paymentkit.util.AnimUtils;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.FieldHolder.CardEntryListener;

public class CVVEditText extends EditText {

  public static final int CCV_LENGTH = 3;
  public static final int CCV_AMEX_LENGTH = 4;
  private int cvvMaxLength = CCV_LENGTH;

  private static final String TAG = CVVEditText.class.getSimpleName();

  private CardEntryListener mListener;
  private ZanyInputConnection mInputConnection;

  private ObjectAnimator shakeAnim;

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
    //setOnFocusChangeListener(mFocusListener);
    setOnKeyListener(new ZanyKeyListener());
  }

  public boolean isValid() {
    return cvvMaxLength == getText().toString().length();
  }

  public void setCardEntryListener(CardEntryListener listener) {
    mListener = listener;
  }

  public void setCVVMaxLength(int val) {
    cvvMaxLength = val;
    InputFilter[] filters = new InputFilter[1];
    filters[0] = new InputFilter.LengthFilter(val);
    setFilters(filters);
  }

  private OnFocusChangeListener mFocusListener = new OnFocusChangeListener() {
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      mListener.onCVVFocus(hasFocus);
    }
  };

  public void indicateInvalidCVV() {
    if (shakeAnim != null) shakeAnim.end();
    shakeAnim = AnimUtils.getShakeAnimation(this, true);
    shakeAnim.start();
  }

  private TextWatcher mTextWatcher = new TextWatcher() {
    @Override
    public void afterTextChanged(Editable s) {
      if (s.length() == cvvMaxLength) {
        mListener.onCVVEntryComplete();
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

  public void setTextWithoutValidation(CharSequence cvv) {
    removeTextChangedListener(mTextWatcher);

    ViewUtils.replaceAllText(getText(), cvv);

    addTextChangedListener(mTextWatcher);
  }

  /////////////////
  // Input Methods
  /////////////////

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    mInputConnection = new ZanyInputConnection(super.onCreateInputConnection(outAttrs), true);
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
          case KeyEvent.KEYCODE_DEL:
            shouldConsume = mInputConnection.handleDelete();
            break;
          // On a hardware keyboard IME_ACTION_NEXT will come as an enter key.
          case KeyEvent.KEYCODE_ENTER:
            shouldConsume = mInputConnection.handleNextPress();
            break;
        }
      }
      // soft-keyboard uses downs while hard uses ups.
      return shouldConsume || event.getAction() == KeyEvent.ACTION_UP;
    }
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
      boolean shouldConsume = false;
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        switch (event.getKeyCode()) {
          case KeyEvent.KEYCODE_DEL:
            shouldConsume = handleDelete();
            break;
          case KeyEvent.KEYCODE_ENTER:
            shouldConsume = handleNextPress();
            break;
        }
      }
      return shouldConsume ? true : super.sendKeyEvent(event);
    }

    @Override
    public boolean performEditorAction(int editorAction) {
      boolean shouldConsume = false;
      switch (editorAction) {
        case EditorInfo.IME_ACTION_DONE:
          shouldConsume = true;
          mListener.onCVVEntryComplete();
      }
      return shouldConsume ? true : super.performEditorAction(editorAction);
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

    private boolean handleNextPress() {
      mListener.onCVVEntryComplete();
      return true;
    }

    private boolean handleDelete() {
      if (getSelectionStart() == 0) {
        mListener.onBackFromCVV();
        return true;
      }
      return false;
    }
  }

}
