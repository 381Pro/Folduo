package jp.bunkaich.sukashimotion;
import android.os.*;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.graphics.Point;
import java.lang.reflect.*;
/** Device-local, explicitly invoked diagnostic. No app pixels or names are stored. */
public final class PanelProbe {
 public static void main(String[] args)throws Exception{
  Looper.prepareMainLooper();
  Object at=Class.forName("android.app.ActivityThread").getMethod("systemMain").invoke(null);
  Context system=(Context)at.getClass().getMethod("getSystemContext").invoke(at);
  DisplayManager dm=system.getSystemService(DisplayManager.class);
  Class<?> type=Class.forName("android.app.ActivityTaskManager");Object manager=type.getMethod("getService").invoke(null);
  Class<?> api=Class.forName("android.app.IActivityTaskManager");
  Object focused=api.getMethod("getFocusedRootTaskInfo").invoke(manager);
  int id=focused.getClass().getField("taskId").getInt(focused);
  int source=focused.getClass().getField("displayId").getInt(focused);
  android.content.ComponentName component=(android.content.ComponentName)focused.getClass().getField("topActivity").get(focused);
  if(component==null||!"com.sec.android.app.popupcalculator".equals(component.getPackageName())||source!=0)throw new IllegalStateException("Calculator on display 0 required");
  System.out.println("INITIAL task="+id+" display="+source);
  DualDisplayControl control=new DualDisplayControl();boolean moved=false;
  try{
   control.hold(false,0);System.out.println("REQUEST_OUTER "+control.describe());Thread.sleep(1200);
   for(Display d:dm.getDisplays()){Point p=new Point();d.getRealSize(p);System.out.println("PANEL "+d.getDisplayId()+" "+p.x+"x"+p.y+" state="+d.getState());}
   moved=true; // Test that launchDisplayId alone moves the existing task.
   System.out.println("MOVED_TO_INNER");
   api.getMethod("setFocusedTask",int.class).invoke(manager,id);Thread.sleep(300);
   android.app.ActivityOptions options=android.app.ActivityOptions.makeBasic().setLaunchDisplayId(1);
   Object launched=api.getMethod("startActivityFromRecents",int.class,Bundle.class).invoke(manager,id,options.toBundle());System.out.println("RESUME_RESULT "+launched);Thread.sleep(1500);
   for(Object root:(java.util.List<?>)api.getMethod("getAllRootTaskInfosOnDisplay",int.class).invoke(manager,1))System.out.println("INNER_TASK "+root.getClass().getField("taskId").getInt(root)+" display="+root.getClass().getField("displayId").getInt(root));
   ShellBridge check=new ShellBridge();Bundle ready=check.windowState(1);System.out.println("INNER_APP_READY "+ready.getBoolean("ready")+" "+ready.getString("geometry"));
   java.lang.Process windows=new ProcessBuilder("dumpsys","window","windows").start();String dump=new String(windows.getInputStream().readAllBytes());
   for(String window:dump.split("(?m)^  Window #"))if(window.contains("taskId="+id+" ")&&window.contains("ty=BASE_APPLICATION"))for(String line:window.split("\\n"))if(line.contains("Frames:")||line.contains("Surface:")||line.contains("isOnScreen=")||line.contains("mHasSurface=")||line.contains("mViewVisibility="))System.out.println(line.trim());
   Object after=api.getMethod("getFocusedRootTaskInfo").invoke(manager);
   System.out.println("FOCUSED task="+after.getClass().getField("taskId").getInt(after)+" display="+after.getClass().getField("displayId").getInt(after));
   Thread.sleep(1500);
  }catch(Exception e){System.out.println("ERROR "+ShellBridge.message(e));}
  finally{
   if(moved)try{api.getMethod("moveRootTaskToDisplay",int.class,int.class).invoke(manager,id,source);api.getMethod("setFocusedTask",int.class).invoke(manager,id);System.out.println("RESTORED_TASK");Thread.sleep(600);}catch(Exception e){System.out.println("RESTORE_ERROR "+ShellBridge.message(e));}
   control.close();System.out.println("RELEASED "+control.describe());System.exit(0);
  }
 }
}
