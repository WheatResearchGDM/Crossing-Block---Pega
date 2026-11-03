package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.view.*;
import android.widget.LinearLayout;

/** Fills the information column; compact fitting is reserved for small windows. */
public final class DetailsPanel extends ViewGroup {
    private final LinearLayout contents;private float scale=1;
    public DetailsPanel(Context c,LinearLayout contents){super(c);this.contents=contents;addView(contents);}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);contents.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int natural=Math.max(1,contents.getMeasuredHeight());scale=Math.min(1,(float)h/natural);if(scale==1)contents.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));contents.setPivotX(0);contents.setPivotY(0);contents.setScaleX(scale);contents.setScaleY(scale);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int x=Math.round((getWidth()-contents.getMeasuredWidth()*scale)/2);contents.layout(x,0,x+contents.getMeasuredWidth(),contents.getMeasuredHeight());}
}
