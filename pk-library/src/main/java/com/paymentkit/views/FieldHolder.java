package com.paymentkit.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.paymentkit.CardType;
import com.paymentkit.R;
import com.paymentkit.ValidateCreditCard;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.CardIcon.CardFace;

/**
 * @author Brendan Weinstein
 */
public class FieldHolder extends RelativeLayout {

  public static final int AMEX_CARD_LENGTH = 17;
  public static final int NON_AMEX_CARD_LENGTH = 19;

  private static final int RE_ENTRY_ALPHA_OUT_DURATION = 100;
  private static final int RE_ENTRY_ALPHA_IN_DURATION = 500;
  private static final int RE_ENTRY_OVERSHOOT_DURATION = 500;

  private CardNumHolder mCardHolder;
  private ExpirationEditText mExpirationEditText;
  private CVVEditText mCVVEditText;
  private CardIcon mCardIcon;
  private LinearLayout mExtraFields;

  protected OnCardValidListener mCardValidListener;

  public FieldHolder(Context context) {
    super(context);
    setup();
  }

  public FieldHolder(Context context, AttributeSet attrs) {
    super(context, attrs);
    setup();
  }

  public CVVEditText getCVVEditText() {
    return mCVVEditText;
  }

  public CardIcon getCardIcon() {
    return mCardIcon;
  }

  public ExpirationEditText getExpirationEditText() {
    return mExpirationEditText;
  }

  public CardNumHolder getCardNumHolder() {
    return mCardHolder;
  }

  public String getCVV() {
    return mCVVEditText.getText().toString();
  }

  public String getExprMonth() {
    return mExpirationEditText.getMonth();
  }

  public String getExprYear() {
    return mExpirationEditText.getYear();
  }

  public String getExprYearAbv() {
    return mExpirationEditText.getYearAbv();
  }

  /**
   * Returns a string with only numeric characters. *
   */
  public String getCardNumber() {
    String formattedNumber = mCardHolder.getCardField().getText().toString();
    return ValidateCreditCard.numericOnlyString(formattedNumber);
  }

  public String getCardType() {
    return mCardIcon.getCardType().getName();
  }

  public boolean isFieldsValid() {
    if (!mCardHolder.isCardNumValid()) {
      return false;
    } else if (!mExpirationEditText.isValid()) {
      return false;
    } else if (!mCVVEditText.isValid()) {
      return false;
    }
    return true;
  }

  /**
   * this listener is a great place to call:
   * ViewUtils.hideSoftKeyboard((Activity)getContext());
   * clearFocus();
   * <p/>
   * Or to send focus to your next view.
   */
  public void setOnCardValidListener(OnCardValidListener listener) {
    mCardValidListener = listener;
  }

  public void lockCardNumField() {
    transitionToExtraFields();
  }

  private void setup() {
    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.pk_field_holder, this, true);
    mCardHolder = (CardNumHolder) findViewById(R.id.card_num_holder);
    mCardIcon = (CardIcon) findViewById(R.id.card_icon);
    mExtraFields = (LinearLayout) findViewById(R.id.extra_fields);
    mExpirationEditText = (ExpirationEditText) findViewById(R.id.expiration);
    mCVVEditText = (CVVEditText) findViewById(R.id.security_code);
    mCardHolder.setCardEntryListener(mCardEntryListener);
    setupViews();
  }

  private void setupViews() {
    setExtraFieldsAlpha();
    setCardEntryListeners();
    setNecessaryFields();
  }

  private void setNecessaryFields() {
    setClipChildren(false);
    setAddStatesFromChildren(true);
    setFocusable(true);
    setFocusableInTouchMode(true);
  }

  private void setExtraFieldsAlpha() {
    ViewHelper.setAlpha(mExtraFields, 0.0f);
    mExtraFields.setVisibility(View.GONE);
  }

  private void setCardEntryListeners() {
    mExpirationEditText.setCardEntryListener(mCardEntryListener);
    mCVVEditText.setCardEntryListener(mCardEntryListener);
  }

  private void validateCard() {
    String stringNumber = getCardNumber();
    long cardNumber = !TextUtils.isEmpty(stringNumber) ? Long.parseLong(stringNumber) : 0;
    if (ValidateCreditCard.isValid(cardNumber)) {
      CardType cardType = ValidateCreditCard.matchCardType(cardNumber);
      mCardIcon.setCardType(cardType);
      transitionToExtraFields();
    } else {
      mCardHolder.indicateInvalidCardNum();
    }
  }

  protected void transitionToExtraFields() {
    /** CREATE LAST 4 DIGITS OVERLAY */
    mCardHolder.createOverlay();

    /** MOVE CARD NUMBER TO LEFT AND ALPHA OUT */
    AnimatorSet set = new AnimatorSet();
    ViewUtils.setHardwareLayer(mCardHolder);
    ObjectAnimator translateAnim = ObjectAnimator.ofFloat(mCardHolder, "translationX", -mCardHolder.getLeftOffset());
    translateAnim.setDuration(500);

    ObjectAnimator alphaOut = ObjectAnimator.ofFloat(mCardHolder.getCardField(), "alpha", 0.0f);
    alphaOut.setDuration(500);
    alphaOut.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator anim) {
        mCardHolder.getCardField().setVisibility(View.GONE);
        ViewUtils.setLayerTypeNone(mCardHolder);
      }
    });

    /** ALPHA IN OTHER FIELDS */
    mExtraFields.setVisibility(View.VISIBLE);
    ObjectAnimator alphaIn = ObjectAnimator.ofFloat(mExtraFields, "alpha", 1.0f);
    alphaIn.setDuration(500);
    set.playTogether(translateAnim, alphaOut, alphaIn);
    set.start();

    mExpirationEditText.requestFocus();
  }

  protected void transitionToCardNumField() {
    mCardIcon.flipTo(CardFace.FRONT);
    AnimatorSet set = new AnimatorSet();

    mCardHolder.getCardField().setVisibility(View.VISIBLE);
    ObjectAnimator alphaOut = ObjectAnimator.ofFloat(mExtraFields, "alpha", 0.0f);
    alphaOut.setDuration(RE_ENTRY_ALPHA_OUT_DURATION);
    alphaOut.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator anim) {
        mExtraFields.setVisibility(View.GONE);
        mCardHolder.destroyOverlay();
        mCardHolder.getCardField().requestFocus();
        mCardHolder.getCardField().setSelection(mCardHolder.getCardField().length());
      }
    });

    ObjectAnimator alphaIn = ObjectAnimator.ofFloat(mCardHolder.getCardField(), "alpha", 0.5f, 1.0f);
    alphaIn.setDuration(RE_ENTRY_ALPHA_IN_DURATION);

    ObjectAnimator overShoot = ObjectAnimator.ofFloat(mCardHolder, "translationX", -mCardHolder.getLeftOffset(), 0.0f);
    overShoot.setInterpolator(new OvershootInterpolator());
    overShoot.setDuration(RE_ENTRY_OVERSHOOT_DURATION);

    set.playTogether(alphaOut, alphaIn, overShoot);
    set.start();
  }

  /**
   * @return true if it focuses on an invalid field. *
   */
  private boolean focusOnInvalidField() {
    if (!mCardHolder.isCardNumValid()) {
      transitionToCardNumField();
      return true;
    }
    if (!mExpirationEditText.isValid()) {
      mExpirationEditText.requestFocus();
      mExpirationEditText.indicateInvalidDate();
      return true;
    }
    if (!mCVVEditText.isValid()) {
      mCVVEditText.requestFocus();
      mCVVEditText.indicateInvalidCVV();
      return true;
    }
    return false;
  }

  public interface OnCardValidListener {
    void cardIsValid();
  }

  protected interface CardEntryListener {
    void onCardNumberInputComplete();

    void onEdit();

    void onCardNumberInputReEntry();

    void onCVVFocus(boolean hasFocus);

    void onCVVEntry();

    void onCVVEntryComplete();

    void onBackFromCVV();

  }

  protected CardEntryListener mCardEntryListener = new CardEntryListener() {
    @Override
    public void onCardNumberInputComplete() {
      validateCard();
    }

    @Override
    public void onEdit() {
      mCardHolder.resetTextColor(); // In case the text color is an an error state.
      CardType newCardType = ValidateCreditCard.getCardType(mCardHolder.getCardField().getText().toString());
      if (!mCardIcon.isCardType(newCardType)) {
        if (newCardType == CardType.AMERICAN_EXPRESS) {
          mCardHolder.getCardField().setMaxCardLength(AMEX_CARD_LENGTH);
          mCVVEditText.setCVVMaxLength(CVVEditText.CCV_AMEX_LENGTH);
        } else {
          mCardHolder.getCardField().setMaxCardLength(NON_AMEX_CARD_LENGTH);
          mCVVEditText.setCVVMaxLength(CVVEditText.CCV_LENGTH);
        }
        mCardIcon.setCardType(newCardType);
      }
    }

    @Override
    public void onCardNumberInputReEntry() {
      transitionToCardNumField();
    }

    @Override
    public void onCVVFocus(boolean hasFocus) {
      if (hasFocus) {
        mCardIcon.flipTo(CardFace.BACK);
      } else {
        mCardIcon.flipTo(CardFace.FRONT);
      }
    }

    @Override
    public void onCVVEntry() {
      mCVVEditText.requestFocus();
    }

    @Override
    public void onCVVEntryComplete() {
      //if (!focusOnInvalidField()) {
        mCardIcon.flipTo(CardFace.FRONT);
        if (mCardValidListener != null) {
          mCardValidListener.cardIsValid();
        }
      //}
    }

    @Override
    public void onBackFromCVV() {
      mExpirationEditText.requestFocus();
    }

  };
}
