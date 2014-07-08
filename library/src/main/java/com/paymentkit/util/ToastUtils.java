package com.paymentkit.util;

import com.paymentkit.R;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ToastUtils {

	public static void showLongToast(Activity activity, int textResId) {
		String text = activity.getResources().getString(textResId);
		showLongToast(activity, text);
	}

	public static void showToast(Activity activity, int textResId) {
		String text = activity.getResources().getString(textResId);
		showToast(activity, text);
	}

	public static void showLongToast(Activity activity, String text) {
		showToast(activity, text, Toast.LENGTH_LONG);
	}

	public static void showToast(Activity activity, String text) {
		showToast(activity, text, Toast.LENGTH_SHORT);
	}

	public static void showToast(Context context, String text) {
		showToast((Activity) context, text, Toast.LENGTH_SHORT);
	}

	public static void showToast(Activity activity, String text, int duration) {
		Toast toast = makeToast(activity, text, duration);
		toast.show();
	}

	public static Toast makeToast(Activity activity, int textResId, int duration) {
		String text = activity.getResources().getString(textResId);
		return makeToast(activity, text, duration);
	}

	private static Toast makeToast(Activity activity, String text, int duration) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.pk_toast, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
		TextView textView = (TextView) layout.findViewById(R.id.text);
		textView.setText(text);
		Toast toast = new Toast(activity);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setDuration(duration);
		toast.setView(layout);
		return toast;
	}
}