package br.com.gdm.crossingblock.seeds;

import android.content.Context;
import android.view.*;

/** Aligned count and observation fields, with both labels outside their backgrounds. */
public final class AnnotationRow extends ViewGroup {
    private final View seedLabel,seeds,noteLabel,note;
    private int seedWidth,gap,labelHeight,fieldY;
    public AnnotationRow(Context c,View seedLabel,View seeds,View noteLabel,View note){super(c);this.seedLabel=seedLabel;this.seeds=seeds;this.noteLabel=noteLabel;this.note=note;addView(seedLabel);addView(seeds);addView(noteLabel);addView(note);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);gap=Math.max(1,Math.min(dp(12),w/24));seedWidth=Math.max(1,Math.min(w*42/100,Math.min(dp(180),Math.max(dp(86),w*28/100))));labelHeight=Math.max(1,Math.min(dp(24),h*28/100));fieldY=labelHeight+Math.max(1,Math.min(dp(6),h/15));int noteWidth=Math.max(1,w-seedWidth-gap),fieldHeight=Math.max(1,h-fieldY);exact(seedLabel,seedWidth,labelHeight);exact(seeds,seedWidth,fieldHeight);exact(noteLabel,noteWidth,labelHeight);exact(note,noteWidth,fieldHeight);setMeasuredDimension(w,h);}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){int nx=seedWidth+gap;seedLabel.layout(0,0,seedWidth,labelHeight);seeds.layout(0,fieldY,seedWidth,getHeight());noteLabel.layout(nx,0,getWidth(),labelHeight);note.layout(nx,fieldY,getWidth(),getHeight());}
}
