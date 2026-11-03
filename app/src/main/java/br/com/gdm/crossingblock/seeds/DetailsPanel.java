package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.view.*;

/** Fills the information column; compact fitting is reserved for small windows. */
public final class DetailsPanel extends ViewGroup {
    private final View information,label,comment;private float scale=1;private int infoHeight,labelHeight,commentHeight,gap;
    public DetailsPanel(Context c,View information,View label,View comment){super(c);this.information=information;this.label=label;this.comment=comment;addView(information);addView(label);addView(comment);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);gap=Math.max(1,Math.min(dp(8),h/30));commentHeight=Math.max(1,Math.min(dp(64),h/5));labelHeight=Math.max(1,Math.min(dp(24),h/8));infoHeight=Math.max(1,h-commentHeight-labelHeight-gap*2);information.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int natural=Math.max(1,information.getMeasuredHeight());scale=Math.min(1,(float)infoHeight/natural);if(scale==1)exact(information,w,infoHeight);information.setPivotX(0);information.setPivotY(0);information.setScaleX(scale);information.setScaleY(scale);exact(label,w,labelHeight);exact(comment,w,commentHeight);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int x=Math.round((getWidth()-information.getMeasuredWidth()*scale)/2);information.layout(x,0,x+information.getMeasuredWidth(),information.getMeasuredHeight());int y=infoHeight+gap;label.layout(0,y,getWidth(),y+labelHeight);y+=labelHeight+gap;comment.layout(0,y,getWidth(),y+commentHeight);}
}
