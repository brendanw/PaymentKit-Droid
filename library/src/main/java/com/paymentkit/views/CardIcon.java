package com.paymentkit.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.paymentkit.CardType;
import com.paymentkit.R;
import com.paymentkit.util.ViewUtils;


public class CardIcon extends FrameLayout {

	private static final String TAG = CardIcon.class.getSimpleName();
	private static final Long HALF_FLIP_DURATION = 125L;

	private CardType mCardType = CardType.UNKNOWN_CARD;
	private CardFace mCardFace = CardFace.FRONT;

	private ImageView mFrontFace;
	private ImageView mBackFace;

	public CardIcon(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
		setCardType(CardType.UNKNOWN_CARD);
	}

	private void setup() {
		mFrontFace = new ImageView(getContext());
		mBackFace = new ImageView(getContext());

		mFrontFace.setScaleType(ScaleType.CENTER_INSIDE);
		mBackFace.setScaleType(ScaleType.CENTER_INSIDE);

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		addView(mBackFace, lp);
		addView(mFrontFace, lp);

		mBackFace.setImageDrawable(getContext().getResources().getDrawable(R.drawable.pk_card_cvc));
		alphaOut(mBackFace);
	}

	public void setCardType(CardType cardType) {
		mCardType = cardType;
		switch (cardType) {
		case VISA:
			mFrontFace.setImageDrawable(getContext().getResources().getDrawable(R.drawable.pk_card_visa));
			break;
		case AMERICAN_EXPRESS:
			mFrontFace.setImageDrawable(getContext().getResources().getDrawable(R.drawable.pk_card_amex));
			break;
		case DISCOVER:
			mFrontFace.setImageDrawable(getContext().getResources().getDrawable(R.drawable.pk_card_discover));
			break;
		case MASTERCARD:
			mFrontFace.setImageDrawable(getContext().getResources().getDrawable(R.drawable.pk_card_master));
			break;
		default:
			mFrontFace.setImageDrawable(getContext().getResources().getDrawable(R.drawable.pk_default_card));
		}
	}

    public boolean isCardType(CardType newCardType) {
        return newCardType == mCardType;
    }

    public enum CardFace {
		BACK, FRONT
	};

	public void flipTo(CardFace face) {
		if (mCardFace == face) {
			return;
		}

		if (face == CardFace.BACK) {
			mCardFace = face;
			flipToBack();
		} else {
			mCardFace = face;
			flipToFront();
		}
	}

	private void flipToBack() {
		ViewUtils.setHardwareLayer(mFrontFace);
		ObjectAnimator rotateFront = ObjectAnimator.ofFloat(mFrontFace, "scaleX", 1.0f, 0.0f);
		rotateFront.setDuration(HALF_FLIP_DURATION);

		ViewUtils.setHardwareLayer(mBackFace);
		final ObjectAnimator rotateBack = ObjectAnimator.ofFloat(mBackFace, "scaleX", 0.0f, 1.0f);
		rotateBack.setDuration(HALF_FLIP_DURATION);
		rotateBack.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator anim) {
				ViewUtils.setLayerTypeNone(mBackFace);
			}
		});

		rotateFront.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator anim) {
				alphaOut(mFrontFace);
				ViewUtils.setLayerTypeNone(mFrontFace);
				alphaIn(mBackFace);
				rotateBack.start();
			}
		});

		rotateFront.start();
	}

	private void flipToFront() {
		ViewUtils.setHardwareLayer(mBackFace);
		ObjectAnimator rotateBackFace = ObjectAnimator.ofFloat(mBackFace, "scaleX", 1.0f, 0.0f);
		rotateBackFace.setDuration(HALF_FLIP_DURATION);

		ViewUtils.setLayerTypeNone(mFrontFace);
		final ObjectAnimator rotateFrontFace = ObjectAnimator.ofFloat(mFrontFace, "scaleX", 0.0f, 1.0f);
		rotateFrontFace.setDuration(HALF_FLIP_DURATION);
		rotateFrontFace.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator anim) {
				ViewUtils.setLayerTypeNone(mBackFace);
			}
		});

		rotateBackFace.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator anim) {
				alphaOut(mBackFace);
			ViewUtils.setLayerTypeNone(mBackFace);
				alphaIn(mFrontFace);
				rotateFrontFace.start();
			}
		});

		rotateBackFace.start();
	}

	private void alphaOut(View v) {
		ObjectAnimator alphaOut = ObjectAnimator.ofFloat(v, "alpha", 0.0f).setDuration(0);
		alphaOut.start();
	}

	private void alphaIn(View v) {
		ObjectAnimator alphaIn = ObjectAnimator.ofFloat(v, "alpha", 1.0f).setDuration(0);
		alphaIn.start();
	}

	public CardType getCardType() {
		return mCardType;
	}

}
