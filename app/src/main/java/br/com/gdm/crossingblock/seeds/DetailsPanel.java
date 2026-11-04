package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.view.*;

/** Fills the information column; compact fitting is reserved for small windows. */
public final class DetailsPanel extends ViewGroup {
    private final View information;private float scale=1;
    public DetailsPanel(Context c,View information){super(c);this.information=information;addView(information);}
    private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);information.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int natural=Math.max(1,information.getMeasuredHeight());scale=Math.min(1,(float)h/natural);if(scale==1)exact(information,w,h);information.setPivotX(0);information.setPivotY(0);information.setScaleX(scale);information.setScaleY(scale);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int x=Math.round((getWidth()-information.getMeasuredWidth()*scale)/2);information.layout(x,0,x+information.getMeasuredWidth(),information.getMeasuredHeight());}
}
