package com.paymentkit.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.widget.RelativeLayout;

import com.paymentkit.R;
import com.paymentkit.ValidateCreditCard;
import com.paymentkit.util.ToastUtils;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.FieldHolder.CardEntryListener;

public class CardNumHolder extends RelativeLayout {

	private static final String TAG = CardNumHolder.class.getSimpleName();

	private static final int SHAKE_DURATION = 400;

	private CardNumEditText mCardNumberEditText;
	private InterceptEditText mLastFourDigits;
	private float mLeftOffset;
	private CardEntryListener mCardEntryListener;
	private View mTopItem;
	private int mSwitchIndex = 0;

	public CardNumHolder(Context context) {
		super(context);
		setup();
	}

	public CardNumHolder(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public String getCardNumberEditText() {
		return mCardNumberEditText.toString();
	}

	public void setCardNumberEditText(String mCardNumberEditText) {
		this.mCardNumberEditText.setText(mCardNumberEditText);
	}

	private void setup() {
		setClipChildren(false);
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.pk_card_holder, this, true);
		mCardNumberEditText = (CardNumEditText) findViewById(R.id.credit_card_no);
		mLastFourDigits = (InterceptEditText) findViewById(R.id.last_four_digits);
		mTopItem = mLastFourDigits;
	}

	public boolean isCardNumValid() {
		if (mCardNumberEditText.length() < mCardNumberEditText.getMaxCardLength()) {
			ToastUtils.showToast(getContext(), "Please enter a valid card number");
			return false;
		} else if (mCardNumberEditText.length() == mCardNumberEditText.getMaxCardLength()) {
			if (ValidateCreditCard.isValid(Long.parseLong(getCardField().getText().toString().replaceAll("\\s", "")))) {
				return true;
			} else {
				ToastUtils.showToast(getContext(), "Please enter a valid card number");
			}
		}
		return false;
	}

	private boolean mIsClickable = true;

	public void setIsClickable(boolean val) {
		mIsClickable = val;
	}

	public void setCardEntryListener(CardEntryListener listener) {
		mCardEntryListener = listener;
		mCardNumberEditText.setCardEntryListener(listener);
		mLastFourDigits.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsClickable) {
					mCardEntryListener.onCardNumberInputReEntry();
				}
			}
		});
	}

	@Override
	protected int getChildDrawingOrder(int childCount, int i) {
		View child = getChildAt(i);
		if (child instanceof CardNumEditText && mCardNumberEditText == mTopItem) {
			mSwitchIndex = i;
			return this.getChildCount() - 1;
		} else if (!(child instanceof CardNumEditText) && mTopItem == mCardNumberEditText) {
			return mSwitchIndex;
		}
		return i;
	}

	public void indicateInvalidCardNum() {
		getCardField().setTextColor(Color.RED);
		mTopItem = mCardNumberEditText;
		ObjectAnimator shakeAnim = ObjectAnimator.ofFloat(getCardField(), "translationX", -16);
		shakeAnim.setDuration(SHAKE_DURATION);
		shakeAnim.setInterpolator(new CycleInterpolator(2.0f));
		shakeAnim.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator anim) {
				mTopItem = null;
			}
		});
		shakeAnim.start();
	}

	public CardNumEditText getCardField() {
		return mCardNumberEditText;
	}

	public void createOverlay() {
		// create 4 digits
		String str = mCardNumberEditText.getText().toString();
		String last4Digits = str.substring(str.length() - 4, str.length());
		mLastFourDigits.setText(last4Digits);
		Paint textPaint = mCardNumberEditText.getPaint();
		float fullWidth = textPaint.measureText(mCardNumberEditText.getText().toString());
		float fourDigitsWidth = textPaint.measureText(last4Digits);
		mLeftOffset = fullWidth - fourDigitsWidth;
		ViewUtils.setMarginLeft(mLastFourDigits, (int) mLeftOffset);
		// align digits on right
		mLastFourDigits.setTextColor(Color.DKGRAY);
		mLastFourDigits.setVisibility(View.VISIBLE);
	}

	public void destroyOverlay() {
		mLastFourDigits.setVisibility(View.GONE);
	}

	public float getLeftOffset() {
		return mLeftOffset;
	}

}
