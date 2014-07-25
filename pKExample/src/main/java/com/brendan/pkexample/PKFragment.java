package com.brendan.pkexample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.paymentkit.util.ToastUtils;
import com.paymentkit.util.ViewUtils;
import com.paymentkit.views.FieldHolder;

/**
 * 
 * @author Brendan Weinstein
 * http://www.brendanweinstein.me
 *
 */
public class PKFragment extends Fragment {

	private final static String TAG = PKFragment.class.getSimpleName();

	public static final float INPUT_WIDTH = 0.94f; // defined in terms of screen
																									// width
	private Button mSaveBtn;
	private ImageView mAcceptedCardsImg;
	private FieldHolder mFieldHolder;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View viewRoot = inflater.inflate(R.layout.add_credit_card, container, false);
		mSaveBtn = (Button) viewRoot.findViewById(R.id.save_btn);
		mAcceptedCardsImg = (ImageView) viewRoot.findViewById(R.id.accepted_cards);
		mFieldHolder = (FieldHolder) viewRoot.findViewById(R.id.field_holder);
		mSaveBtn.setOnClickListener(mSaveBtnListener);
		return viewRoot;
	}

	private OnClickListener mSaveBtnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			ViewUtils.hideSoftKeyboard(getActivity());
			if (mFieldHolder.isFieldsValid()) {
				ToastUtils.showToast(getActivity(), "Valid credit card entry!");
			} else {
                ToastUtils.showToast(getActivity(), getResources().getString(com.paymentkit.R.string.pk_error_invalid_card_no));
            }
		}
	};

	private void setupViews() {
		float marginLeft = 1.0f - INPUT_WIDTH;
		ViewUtils.setMarginLeft(mAcceptedCardsImg, (int) (marginLeft * ViewUtils.getScreenWidth(getActivity())));
		ViewUtils.setWidth(mFieldHolder, (int) (INPUT_WIDTH * ViewUtils.getScreenWidth(getActivity())));
	}

	/* After onCreateView is called */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupViews();
	}

}