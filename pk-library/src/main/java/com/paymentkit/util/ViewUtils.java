package com.paymentkit.util;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ViewUtils {

	private static final String TAG = ViewUtils.class.getSimpleName();
	
	private static final int LAYER_TYPE_HARDWARE = 2;
	private static final int LAYER_TYPE_NONE = 0;
	private static final int LAYER_TYPE_SOFTWARE = 1;
	
	public static void hideSoftKeyboard(final Activity activity) {
		if (activity.getCurrentFocus() != null) {
			final InputMethodManager imm = (InputMethodManager) activity.getSystemService(activity.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
		}
	}

    public static void replaceAllText(Editable editable, CharSequence newString) {
        InputFilter[] filters = editable.getFilters();
        editable.setFilters(new InputFilter[] { });
        // We need to remove filters so we can add text with spaces.
        editable.replace(0, editable.length(), newString);
        editable.setFilters(filters);
    }

	public static int getScreenHeight(final Context context) {
		return context.getResources().getDisplayMetrics().heightPixels;
	}

	public static int getScreenWidth(final Context context) {
		return context.getResources().getDisplayMetrics().widthPixels;
	}

	public static void moveVertical(final View v, final float distance) {
		final MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
		final int newTopMargin = (int) (lp.topMargin + distance);
		setMarginTop(v, newTopMargin);
	}

	public static void setMarginTop(final View v, final int margin) {
		final MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
		lp.setMargins(lp.leftMargin, margin, lp.rightMargin, lp.bottomMargin);
		v.setLayoutParams(lp);
	}

	public static void setMarginBottom(final View v, final int margin) {
		final MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
		lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, margin);
		v.setLayoutParams(lp);
	}

	public static void setMarginLeft(final View v, final int margin) {
		final MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
		lp.setMargins(margin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
		v.setLayoutParams(lp);
	}

	public static void setMarginRight(final View v, final int margin) {
		final MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
		lp.setMargins(lp.leftMargin, lp.topMargin, margin, lp.bottomMargin);
		v.setLayoutParams(lp);
	}

	public static void setHeight(final View v, final int height) {
		final ViewGroup.LayoutParams lp = v.getLayoutParams();
		if (lp.height != height) {
			lp.height = height;
			v.setLayoutParams(lp);
		}
	}

	public static void setWidth(final View v, final int width) {
		final ViewGroup.LayoutParams lp = v.getLayoutParams();
		if (lp.width != width) {
			lp.width = width;
			v.setLayoutParams(lp);
		}
	}
	
	public static void setHardwareLayer(View view) {
		setLayerType(view, LAYER_TYPE_HARDWARE);
	}

	public static void setLayerTypeNone(View view) {
		setLayerType(view, LAYER_TYPE_NONE);
	}

	public static void setLayerTypeSoftware(View view) {
		setLayerType(view, LAYER_TYPE_SOFTWARE);
	}

	public static void setLayerType(View view, int type) {
		try {
			Class[] paramTypes = new Class[1];
			paramTypes[0] = Integer.TYPE;
			Method setLayerTypeMethod = view.getClass().getMethod("setLayerType", paramTypes);
			setLayerTypeMethod.invoke(view, type);
			Log.d(TAG, "setLayerType successfully called");
		} catch (NoSuchMethodException e) {
			// method does not exist in this api level
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}