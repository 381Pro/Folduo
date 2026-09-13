package jp.bunkaich.sukashimotion;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class PreviewTest {
 <T extends View> T find(View view,Class<T> type){if(type.isInstance(view))return type.cast(view);if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){T found=find(group.getChildAt(i),type);if(found!=null)return found;}return null;}
 @Test public void opensChangesAngleAndSwitchesPanels()throws Exception{
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();
  Activity activity=instrumentation.startActivitySync(new Intent(context,PreviewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  try{
   waitForFrame(activity,true);commit(activity,()->find(activity.getWindow().getDecorView(),SeekBar.class).setProgress(120));
   save("motion-inner-preview.png");
   instrumentation.runOnMainSync(()->find(activity.getWindow().getDecorView(),Button.class).performClick());
   waitForFrame(activity,false);commit(activity,()->find(activity.getWindow().getDecorView(),SeekBar.class).setProgress(60));
   save("motion-cover-preview.png");
  }finally{instrumentation.runOnMainSync(activity::finish);}
 }
 void commit(Activity activity,Runnable action)throws Exception{
  var done=new java.util.concurrent.CountDownLatch(1);
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   activity.getWindow().getDecorView().getViewTreeObserver().registerFrameCommitCallback(done::countDown);action.run();
  });
  assertTrue("Updated preview frame is committed",done.await(5,java.util.concurrent.TimeUnit.SECONDS));
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  Thread.sleep(600); // Allow the software emulator compositor to present the committed buffer.
 }
 void waitForFrame(Activity activity,boolean inner)throws Exception{
  boolean[] ready={false};long end=SystemClock.elapsedRealtime()+4000;
  while(!ready[0]&&SystemClock.elapsedRealtime()<end){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{SnapshotView v=find(activity.getWindow().getDecorView(),SnapshotView.class);ready[0]=v!=null&&v.inner==inner&&v.isLaidOut()&&v.getWidth()>0&&!activity.getWindow().getDecorView().isLayoutRequested();});if(!ready[0])Thread.sleep(25);}
  assertTrue("Preview is rendered with selected panel",ready[0]);
 }
 void save(String filename)throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(OutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),filename))){InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,out);}
 }
}
