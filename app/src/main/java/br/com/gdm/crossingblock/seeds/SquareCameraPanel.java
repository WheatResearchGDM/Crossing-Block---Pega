package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.view.*;

/** Full column width for the square preview; status and reread share the row below. */
public final class SquareCameraPanel extends ViewGroup {
    private final View camera,reread,status;private int side,buttonWidth,buttonHeight,statusHeight,gap;
    public SquareCameraPanel(Context c,View camera,View reread,View status){super(c);this.camera=camera;this.reread=reread;this.status=status;addView(camera);addView(reread);addView(status);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);gap=Math.min(dp(8),Math.max(1,w/20));buttonWidth=Math.min(dp(64),w/2);statusHeight=Math.max(1,Math.min(dp(44),h/4));side=Math.max(1,Math.min(w,h-statusHeight-gap));buttonHeight=statusHeight;exact(camera,side,side);exact(reread,buttonWidth,buttonHeight);exact(status,w-buttonWidth-gap,statusHeight);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int x=(getWidth()-side)/2;camera.layout(x,0,x+side,side);int sy=side+gap;status.layout(0,sy,getWidth()-buttonWidth-gap,sy+statusHeight);reread.layout(getWidth()-buttonWidth,sy,getWidth(),sy+buttonHeight);}
}
