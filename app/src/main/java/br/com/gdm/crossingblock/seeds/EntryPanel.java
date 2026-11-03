package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.view.*;

/** Expands the keypad into the remaining height instead of leaving an empty lower area. */
public final class EntryPanel extends ViewGroup {
    private final View value,keys,save,feedback;private int vh,kh,sh,fh,gap;
    public EntryPanel(Context c,View value,View keys,View save,View feedback){super(c);this.value=value;this.keys=keys;this.save=save;this.feedback=feedback;addView(value);addView(keys);addView(save);addView(feedback);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);gap=Math.max(2,Math.min(dp(10),h/50));vh=Math.max(1,Math.min(dp(80),h*22/100));sh=Math.max(1,Math.min(dp(64),h*14/100));feedback.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));fh=Math.min(feedback.getMeasuredHeight(),h/4);kh=Math.max(1,h-vh-sh-fh-gap*3);exact(value,w,vh);exact(keys,w,kh);exact(save,w,sh);exact(feedback,w,fh);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int y=0;value.layout(0,y,getWidth(),y+vh);y+=vh+gap;keys.layout(0,y,getWidth(),y+kh);y+=kh+gap;save.layout(0,y,getWidth(),y+sh);y+=sh+gap;feedback.layout(0,y,getWidth(),y+fh);}
}
