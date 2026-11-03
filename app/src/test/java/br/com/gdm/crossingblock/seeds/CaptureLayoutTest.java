package br.com.gdm.crossingblock.seeds;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.Assert.*;

/** Measures the actual capture UI without starting camera, storage or file providers. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE,qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
public class CaptureLayoutTest {
    private MainActivity activity;
    private CaptureViewport viewport;

    @Before public void createCaptureSurface()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class);
        activity=controller.get();activity.setTheme(br.com.gdm.crossingblock.seeds.R.style.AppTheme);
        Method create=MainActivity.class.getDeclaredMethod("createInterface");create.setAccessible(true);create.invoke(activity);
        viewport=(CaptureViewport)((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
        Method operation=MainActivity.class.getDeclaredMethod("addOperation",String.class,String.class,String.class);operation.setAccessible(true);
        operation.invoke(activity,"Emasculação","30/10/2026","Responsável pela emasculação");
        operation.invoke(activity,"Polinização","31/10/2026","Responsável pela polinização");
    }
    private void layout(int width,int height){viewport.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));viewport.layout(0,0,width,height);}
    private List<View> descendants(View view){List<View> result=new ArrayList<>();result.add(view);if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)result.addAll(descendants(g.getChildAt(i)));}return result;}
    private SeedCountView count(){for(View v:descendants(viewport))if(v instanceof SeedCountView)return (SeedCountView)v;throw new AssertionError("Count display missing");}
    private void allControlsVisible(){
        Matrix globalToRoot=new Matrix();viewport.transformMatrixToGlobal(globalToRoot);globalToRoot.invert(globalToRoot);
        int numericKeys=0;
        for(View v:descendants(viewport)){
            assertFalse("No vertical scroll container",v instanceof ScrollView);
            assertFalse("No horizontal scroll container",v instanceof HorizontalScrollView);
            if(!(v instanceof Button)&&!(v instanceof ImageButton)&&!(v instanceof SeedCountView))continue;
            if(v instanceof Button&&((Button)v).getText().toString().matches("[0-9]"))numericKeys++;
            assertTrue("Control has width",v.getWidth()>0);assertTrue("Control has height",v.getHeight()>0);
            Matrix matrix=new Matrix();v.transformMatrixToGlobal(matrix);RectF rect=new RectF(0,0,v.getWidth(),v.getHeight());matrix.mapRect(rect);globalToRoot.mapRect(rect);
            assertTrue("Left edge of "+v,rect.left>=-1);
            assertTrue("Top edge of "+v,rect.top>=-1);
            assertTrue("Right edge of "+v,rect.right<=viewport.getWidth()+1);
            assertTrue("Bottom edge of "+v,rect.bottom<=viewport.getHeight()+1);
        }
        assertEquals("All ten numeric keys are present",10,numericKeys);
        assertFalse(viewport.canScrollVertically(1));assertFalse(viewport.canScrollHorizontally(1));
    }
    @Test public void allControlsFitOnSmallAndTallPhones(){for(int[] size:new int[][]{{320,480},{360,640},{360,740},{412,840}}){layout(size[0],size[1]);allControlsVisible();}}
    @Test public void tabletLayoutGrowsProportionally(){layout(360,740);float phone=viewport.contentScale();layout(800,1280);allControlsVisible();assertTrue("Tablet uses larger controls",viewport.contentScale()>phone);assertFalse(viewport.usesColumns());layout(1200,1920);allControlsVisible();}
    @Test public void landscapeReflowsAndResizeReturnsToPortrait(){for(int[] size:new int[][]{{640,360},{960,600},{1280,800},{1920,1200}}){layout(size[0],size[1]);allControlsVisible();assertTrue("Landscape uses two columns",viewport.usesColumns());}layout(360,740);allControlsVisible();assertFalse(viewport.usesColumns());}
    @Test public void countNeverPansOrChangesOnDrag(){
        layout(360,640);SeedCountView view=count();assertFalse(android.widget.EditText.class.isInstance(view));
        for(String value:new String[]{"0","99"}){
            view.setText(value);layout(360,640);view.scrollTo(45,20);view.scrollBy(15,40);
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(0,20,action,70,35,0);view.dispatchTouchEvent(event);event.recycle();}
            assertEquals(value,view.getText().toString());assertEquals(0,view.getScrollX());assertEquals(0,view.getScrollY());
            assertTrue(view.getLayout().getLineWidth(0)<=view.getWidth()-view.getPaddingLeft()-view.getPaddingRight());
        }
    }
    @Test public void systemInsetsLeaveAllControlsInsideUsableWindow(){viewport.setPadding(0,28,0,24);layout(360,640);allControlsVisible();}
    @Test public void collectedQrWarningIsRedInsideEarAndStaysWhileEditing()throws Exception{
        RecordStore store=new RecordStore(activity);
        try{
            store.clear();store.save("already-collected",7,null);ReflectionHelpers.setField(activity,"store",store);
            Method accept=MainActivity.class.getDeclaredMethod("acceptQr",String.class);accept.setAccessible(true);accept.invoke(activity,"already-collected");
            TextView warning=ReflectionHelpers.getField(activity,"duplicateNotice");LinearLayout ear=ReflectionHelpers.getField(activity,"ear");
            assertEquals("QR Code já coletado",warning.getText().toString());assertEquals(android.graphics.Color.rgb(160,45,45),warning.getCurrentTextColor());assertEquals(View.VISIBLE,warning.getVisibility());assertSame(ear,warning.getParent());
            for(View v:descendants(viewport))if(v instanceof Button&&((Button)v).getText().toString().equals("Limpar"))v.performClick();
            assertEquals(View.VISIBLE,warning.getVisibility());layout(360,640);allControlsVisible();
            accept.invoke(activity,"new-qr"); // A paused camera ignores this; reread resumes scanning.
            ReflectionHelpers.setField(activity,"scanning",true);accept.invoke(activity,"new-qr");assertEquals(View.GONE,warning.getVisibility());
        }finally{store.close();}
    }
}
