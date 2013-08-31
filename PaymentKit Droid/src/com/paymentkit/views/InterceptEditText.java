package com.paymentkit.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;


public class InterceptEditText extends EditText {
	
	private static final String TAG = InterceptEditText.class.getSimpleName();
	
	public InterceptEditText(Context context) {
		super(context);
		setup();
	}

	public InterceptEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}
	
	private void setup() {
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		switch(e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			return true;
		case MotionEvent.ACTION_UP:
			performClick();
		}
		return true;
	}

}
