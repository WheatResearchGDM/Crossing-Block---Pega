package br.com.gdm.crossingblock.seeds;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

/** Lays out the usable window directly instead of scaling a narrow phone-sized surface. */
public final class CaptureViewport extends ViewGroup {
    private final View header,summary,camera,details,entry;
    private boolean columns;
    private float scale=1;
    private int pad,gap,headerHeight,summaryHeight,topY,mainHeight,upperHeight,leftWidth,rightWidth;
    public CaptureViewport(Context context,View header,View summary,View camera,View details,View entry){
        super(context);this.header=header;this.summary=summary;this.camera=camera;this.details=details;this.entry=entry;
        addView(header);addView(summary);addView(camera);addView(details);addView(entry);
        setClipChildren(true);setOverScrollMode(OVER_SCROLL_NEVER);setWillNotDraw(false);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void exact(View v,int width,int height){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,height),MeasureSpec.EXACTLY));}
    private void fonts(View view){
        if(view instanceof TextView){TextView t=(TextView)view;Object tag=t.getTag();float sp=tag instanceof Number?((Number)tag).floatValue():t.getTextSize()/getResources().getDisplayMetrics().scaledDensity;
            if(!(tag instanceof Number))t.setTag(sp);
            int max=Math.max(1,Math.round(sp*getResources().getDisplayMetrics().scaledDensity*scale));
            if(t instanceof Button||t instanceof SeedCountView||t.getMaxLines()<=2){int min=Math.min(max,dp(t instanceof SeedCountView?16:t.getMaxLines()==1?8:10));t.setAutoSizeTextTypeUniformWithConfiguration(Math.max(1,min),max,1,TypedValue.COMPLEX_UNIT_PX);}
            else t.setTextSize(TypedValue.COMPLEX_UNIT_PX,max);
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)fonts(g.getChildAt(i));}
    }
    @Override protected void onMeasure(int ws,int hs){
        int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs),aw=Math.max(1,w-getPaddingLeft()-getPaddingRight()),ah=Math.max(1,h-getPaddingTop()-getPaddingBottom());
        float density=getResources().getDisplayMetrics().density;
        scale=Math.max(1,Math.min(1.6f,Math.min(aw/density/500f,ah/density/800f)));columns=aw>ah*1.1f;
        for(int i=0;i<getChildCount();i++)fonts(getChildAt(i));
        pad=dp(12*scale);gap=dp(12*scale);headerHeight=Math.min(dp(64*scale),ah/6);exact(header,aw,headerHeight);
        int inner=Math.max(1,aw-pad*2);
        summary.measure(MeasureSpec.makeMeasureSpec(inner,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));summaryHeight=summary.getMeasuredHeight();
        topY=getPaddingTop()+headerHeight+pad+summaryHeight+gap;mainHeight=Math.max(1,h-getPaddingBottom()-pad-topY);
        if(columns){int entryWidth=Math.max(1,Math.round((inner-gap*2)*.38f)),others=Math.max(2,inner-gap*2-entryWidth);leftWidth=others/2;rightWidth=others-leftWidth;exact(camera,leftWidth,mainHeight);exact(details,rightWidth,mainHeight);exact(entry,entryWidth,mainHeight);}
        else{leftWidth=(inner-gap)/2;rightWidth=inner-gap-leftWidth;upperHeight=Math.max(1,Math.round((mainHeight-gap)*.46f));exact(camera,leftWidth,upperHeight);exact(details,rightWidth,upperHeight);exact(entry,inner,Math.max(1,mainHeight-gap-upperHeight));}
        setMeasuredDimension(w,h);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int x=getPaddingLeft(),y=getPaddingTop(),aw=getWidth()-getPaddingLeft()-getPaddingRight();header.layout(x,y,x+aw,y+headerHeight);
        x+=pad;y+=headerHeight+pad;summary.layout(x,y,x+summary.getMeasuredWidth(),y+summaryHeight);
        camera.layout(x,topY,x+leftWidth,topY+camera.getMeasuredHeight());details.layout(x+leftWidth+gap,topY,x+leftWidth+gap+rightWidth,topY+details.getMeasuredHeight());
        if(columns){int ex=x+leftWidth+gap+rightWidth+gap;entry.layout(ex,topY,ex+entry.getMeasuredWidth(),topY+mainHeight);}
        else{int ey=topY+upperHeight+gap;entry.layout(x,ey,x+entry.getMeasuredWidth(),ey+entry.getMeasuredHeight());}
    }
    public boolean usesColumns(){return columns;}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);Paint p=new Paint();p.setColor(Color.rgb(9,36,59));canvas.drawRect(0,0,getWidth(),getPaddingTop(),p);canvas.drawRect(0,getHeight()-getPaddingBottom(),getWidth(),getHeight(),p);}
    public float contentScale(){return scale;}
    @Override public void scrollTo(int x,int y){super.scrollTo(0,0);}
    @Override public void scrollBy(int x,int y){}
}
