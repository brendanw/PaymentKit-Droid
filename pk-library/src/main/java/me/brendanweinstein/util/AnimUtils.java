package me.brendanweinstein.util;

import android.graphics.Color;
import android.view.animation.CycleInterpolator;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;

public final class AnimUtils {
    private static final int SHAKE_DURATION = 400;

    private AnimUtils() { }

    /**
     * @param shouldResetTextColor if true make sure you end the previous animation before starting this one.
     */
    public static ObjectAnimator getShakeAnimation(final TextView textView, final boolean shouldResetTextColor) {
        final int textColor = textView.getCurrentTextColor();
        textView.setTextColor(Color.RED);

        ObjectAnimator shakeAnim = ObjectAnimator.ofFloat(textView, "translationX", -16);
        shakeAnim.setDuration(SHAKE_DURATION);
        shakeAnim.setInterpolator(new CycleInterpolator(2.0f));
        shakeAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                if (shouldResetTextColor) {
                    textView.setTextColor(textColor);
                }
            }
        });
        return shakeAnim;
    }
}
