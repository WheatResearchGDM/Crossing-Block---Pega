package br.com.gdm.crossingblock.seeds;

import android.content.Context;
import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;

/** Read-only value display. Only the app keypad changes the value; gestures cannot pan it. */
// This app uses the native Material theme and API 26+ platform text autosizing.
@SuppressLint("AppCompatCustomView")
public final class SeedCountView extends TextView {
    public SeedCountView(Context context) {
        super(context);
        setGravity(Gravity.CENTER);
        setMaxLines(1);
        setHorizontallyScrolling(false);
        setMovementMethod(null);
        setTextIsSelectable(false);
        setFocusable(false);
        setClickable(false);
        setLongClickable(false);
        setIncludeFontPadding(false);
        setPadding(0,0,0,0);
        setAutoSizeTextTypeUniformWithConfiguration(16,28,1,TypedValue.COMPLEX_UNIT_SP);
        setHint("—");
        setContentDescription("Número de sementes");
        setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
    }
    @Override public void scrollTo(int x,int y) { super.scrollTo(0,0); }
    @Override public void scrollBy(int x,int y) { /* The displayed count is anchored to the center. */ }
    @Override public boolean onTouchEvent(MotionEvent event) { return false; }
}
