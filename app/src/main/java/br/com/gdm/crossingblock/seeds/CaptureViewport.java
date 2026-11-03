package br.com.gdm.crossingblock.seeds;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Fits the complete capture surface in the usable window, including after a resize.
 * The content retains its aspect ratio, grows on tablets and reflows into two columns
 * in landscape. Native view transforms also map touch coordinates to the keypad.
 */
public final class CaptureViewport extends ViewGroup {
    private final LinearLayout design,body,information,entry;
    private boolean columns;
    private float scale=1f;

    public CaptureViewport(Context context,LinearLayout design,LinearLayout body,
                           LinearLayout information,LinearLayout entry) {
        super(context);
        this.design=design;this.body=body;this.information=information;this.entry=entry;
        setClipChildren(true);
        setClipToPadding(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        addView(design);
        arrange(false);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void arrange(boolean useColumns){
        columns=useColumns;
        body.setOrientation(useColumns?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        body.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(useColumns?0:LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT,useColumns?1:0);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(useColumns?0:LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT,useColumns?1:0);
        if(useColumns)right.leftMargin=dp(16);else right.topMargin=dp(10);
        information.setLayoutParams(left);entry.setLayoutParams(right);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);
        int availableWidth=Math.max(1,width-getPaddingLeft()-getPaddingRight());
        int availableHeight=Math.max(1,height-getPaddingTop()-getPaddingBottom());
        boolean useColumns=availableWidth>availableHeight*1.1f;
        if(useColumns!=columns)arrange(useColumns);
        int referenceWidth=dp(useColumns?720:360);
        design.measure(MeasureSpec.makeMeasureSpec(referenceWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
        scale=Math.min((float)availableWidth/design.getMeasuredWidth(),(float)availableHeight/Math.max(1,design.getMeasuredHeight()));
        design.setPivotX(0);design.setPivotY(0);design.setScaleX(scale);design.setScaleY(scale);
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        int x=getPaddingLeft()+Math.round((getWidth()-getPaddingLeft()-getPaddingRight()-design.getMeasuredWidth()*scale)/2);
        int y=getPaddingTop()+Math.round((getHeight()-getPaddingTop()-getPaddingBottom()-design.getMeasuredHeight()*scale)/2);
        design.layout(x,y,x+design.getMeasuredWidth(),y+design.getMeasuredHeight());
    }
    public boolean usesColumns(){return columns;}
    public float contentScale(){return scale;}
    @Override public void scrollTo(int x,int y){super.scrollTo(0,0);}
    @Override public void scrollBy(int x,int y){ /* The capture screen has no scroll range. */ }
}
