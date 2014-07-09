package com.paymentkit.views;

import android.content.Context;
import android.text.InputFilter;
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
import com.paymentkit.CardType;
import com.paymentkit.R;
import com.paymentkit.ValidateCreditCard;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.CardIcon.CardFace;

/**
 * 
 * @author Brendan Weinstein
 *
 */
public class FieldHolder extends RelativeLayout {
	
	public static int CVV_MAX_LENGTH = 3;
	
	protected static final int AMEX_CARD_LENGTH = 17;
	public static final int NON_AMEX_CARD_LENGTH = 19;
	
	private static final int RE_ENTRY_ALPHA_OUT_DURATION = 100;
	private static final int RE_ENTRY_ALPHA_IN_DURATION = 500;
	private static final int RE_ENTRY_OVERSHOOT_DURATION = 500;
	
	private CardNumHolder mCardHolder;
	private ExpirationEditText mExpirationEditText;
	private CVVEditText mCVVEditText;
	private CardIcon mCardIcon;
	private LinearLayout mExtraFields;
	
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
	}
	
	private void setExtraFieldsAlpha() {
		ObjectAnimator setAlphaZero = ObjectAnimator.ofFloat(mExtraFields, "alpha", 0.0f);
		setAlphaZero.setDuration(0);
		setAlphaZero.start();
		mExtraFields.setVisibility(View.GONE);
	}

	private void setCardEntryListeners() {
		mExpirationEditText.setCardEntryListener(mCardEntryListener);
		mCVVEditText.setCardEntryListener(mCardEntryListener);
	}

	private void validateCard() {
		long cardNumber = Long.parseLong(mCardHolder.getCardField().getText().toString().replaceAll("\\s", ""));
		if (ValidateCreditCard.isValid(cardNumber)) {
			CardType cardType = ValidateCreditCard.matchCardType(cardNumber);
			mCardIcon.setCardType(cardType);
			transitionToExtraFields();
		} else {
			mCardHolder.indicateInvalidCardNum();
		}
	}

	private void transitionToExtraFields() {
		// CREATE LAST 4 DIGITS OVERLAY
		mCardHolder.createOverlay();

		// MOVE CARD NUMBER TO LEFT AND ALPHA OUT
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

		// ALPHA IN OTHER FIELDS
		mExtraFields.setVisibility(View.VISIBLE);
		ObjectAnimator alphaIn = ObjectAnimator.ofFloat(mExtraFields, "alpha", 1.0f);
		alphaIn.setDuration(500);
		set.playTogether(translateAnim, alphaOut, alphaIn);
		set.start();

		mExpirationEditText.requestFocus();
	}
	
	public interface CardEntryListener {
		public void onCardNumberInputComplete();

		public void onEdit();

		public void onCardNumberInputReEntry();

		public void onCVVEntry();

		public void onCVVEntryComplete();

		public void onBackFromCVV();

	}
	
	private void setCVVMaxLength(int val) {
		CVV_MAX_LENGTH = val;
		InputFilter[] filters = new InputFilter[1];
		filters[0] = new InputFilter.LengthFilter(val);
		mCVVEditText.setFilters(filters);
	}

	CardEntryListener mCardEntryListener = new CardEntryListener() {
		@Override
		public void onCardNumberInputComplete() {
			validateCard();
		}

		@Override
		public void onEdit() {
			CardType newCardType = ValidateCreditCard.getCardType(mCardHolder.getCardField().getText().toString());
			if (newCardType == CardType.AMERICAN_EXPRESS) {
				mCardHolder.getCardField().setMaxCardLength(AMEX_CARD_LENGTH);
				setCVVMaxLength(4);
			} else {
				mCardHolder.getCardField().setMaxCardLength(NON_AMEX_CARD_LENGTH);
				setCVVMaxLength(3);
			}
			mCardIcon.setCardType(ValidateCreditCard.getCardType(mCardHolder.getCardField().getText().toString()));
		}

		@Override
		public void onCardNumberInputReEntry() {
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

		@Override
		public void onCVVEntry() {
			mCardIcon.flipTo(CardFace.BACK);
			mCVVEditText.requestFocus();
		}

		@Override
		public void onCVVEntryComplete() {
			mCardIcon.flipTo(CardFace.FRONT);
			FieldHolder.this.requestFocus();
			// complete
		}

		@Override
		public void onBackFromCVV() {
			mExpirationEditText.requestFocus();
			mCardIcon.flipTo(CardFace.FRONT);
		}

	};
	
	public boolean isFieldsValid() {
		if (mExpirationEditText.getText().toString().length() != 5) {
			return false;
		} else if (mCVVEditText.getText().toString().length() != CVV_MAX_LENGTH) {
			return false;
		}
		return true;
	}

}
