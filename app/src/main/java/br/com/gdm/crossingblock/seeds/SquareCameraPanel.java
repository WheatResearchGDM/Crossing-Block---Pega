package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.view.*;

/** Camera remains square at every aspect ratio; reread stays alongside it. */
public final class SquareCameraPanel extends ViewGroup {
    private final View camera,reread,status;private int side,buttonWidth,buttonHeight,statusHeight,gap;
    public SquareCameraPanel(Context c,View camera,View reread,View status){super(c);this.camera=camera;this.reread=reread;this.status=status;addView(camera);addView(reread);addView(status);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);gap=Math.min(dp(8),Math.max(1,w/20));buttonWidth=Math.min(dp(56),w/4);status.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));statusHeight=Math.min(status.getMeasuredHeight(),h/4);side=Math.max(1,Math.min(w-buttonWidth-gap,h-statusHeight-gap));buttonHeight=Math.min(dp(70),side);exact(camera,side,side);exact(reread,buttonWidth,buttonHeight);exact(status,w,statusHeight);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int x=(getWidth()-side-gap-buttonWidth)/2,y=Math.max(0,(getHeight()-side-statusHeight-gap)/2);camera.layout(x,y,x+side,y+side);int bx=x+side+gap,by=y+(side-buttonHeight)/2;reread.layout(bx,by,bx+buttonWidth,by+buttonHeight);int sy=y+side+gap;status.layout(0,sy,getWidth(),sy+statusHeight);}
}
