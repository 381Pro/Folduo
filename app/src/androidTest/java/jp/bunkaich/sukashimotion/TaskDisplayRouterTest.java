package jp.bunkaich.sukashimotion;
import android.os.Bundle;
import android.app.ActivityOptions;
import android.content.ComponentName;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TaskDisplayRouterTest {
 public static class WindowConfig {int type;WindowConfig(int t){type=t;}public int getActivityType(){return type;}}
 public static class Config {public WindowConfig windowConfiguration;Config(int t){windowConfiguration=new WindowConfig(t);}}
 public static class Info {
  public int taskId,displayId;public Config configuration;public ComponentName topActivity=new ComponentName("test","test.Main");
  Info(int id,int display,int type){taskId=id;displayId=display;configuration=new Config(type);}
 }
 public static class Manager {
  List<Info> all=new ArrayList<>();int resumed=-1,focused=-1;List<String> moves=new ArrayList<>();
  public List<Info> getTasks(int count,boolean visible,boolean intent,int display){return getAllRootTaskInfosOnDisplay(display).stream().filter(i->i.topActivity!=null).toList();}
  public List<Info> getAllRootTaskInfosOnDisplay(int display){return all.stream().filter(i->i.displayId==display).toList();}
  public void moveRootTaskToDisplayOnTopOrBottom(int id,int display,boolean top){
   Info task=all.stream().filter(i->i.taskId==id).findFirst().orElseThrow();
   if(task.configuration.windowConfiguration.type==2&&all.stream().anyMatch(i->i.displayId==display&&i.configuration.windowConfiguration.type==2))throw new IllegalStateException("Duplicate HOME root");
   moves.add(id+":"+display+":"+top);task.displayId=display;
  }
  public void setFocusedRootTask(int id){focused=id;}
  public void setFocusedTask(int id){focused=id;}
  public int startActivityFromRecents(int id,Bundle options){resumed=id;all.stream().filter(i->i.taskId==id).forEach(i->i.displayId=options.getInt("android.activity.launchDisplayId", -1));return 0;}
 }
 @Test public void homeUsesExistingDestinationWithoutMovingAnotherRoot()throws Exception{
  Manager m=new Manager();m.all.add(new Info(7,0,2));m.all.add(new Info(8,1,2));TaskDisplayRouter r=new TaskDisplayRouter(m,Manager.class);
  assertTrue(r.move(0,1,false).getBoolean("home"));assertEquals(8,m.focused);assertTrue(m.moves.isEmpty());
  r.move(1,0,false);assertEquals(7,m.focused);assertTrue(m.moves.isEmpty());
 }
 @Test public void repeatedHomeFoldsKeepBothRootsOnTheirDisplay()throws Exception{
  Manager m=new Manager();m.all.add(new Info(7,0,2));m.all.add(new Info(8,1,2));TaskDisplayRouter r=new TaskDisplayRouter(m,Manager.class);
  for(int i=0;i<8;i++){r.move(0,1,false);r.move(1,0,false);}
  assertEquals(0,m.all.get(0).displayId);assertEquals(1,m.all.get(1).displayId);assertTrue(m.moves.isEmpty());
 }
 @Test public void restoreNeverMovesDuplicateHomeOrUnrelatedApps()throws Exception{
  Manager m=new Manager();m.all.add(new Info(7,0,2));m.all.add(new Info(8,1,2));m.all.add(new Info(15,1,1));TaskDisplayRouter r=new TaskDisplayRouter(m,Manager.class);r.restore();
  assertEquals(7,m.focused);assertTrue(m.moves.isEmpty());assertEquals(1,m.all.get(2).displayId);
 }
 @Test public void returnCurrentAppWithoutResurrectingPreviousApp()throws Exception{
  Manager m=new Manager();m.all.add(new Info(22,1,1));m.all.add(new Info(7,0,2));m.all.add(new Info(8,1,2));TaskDisplayRouter r=new TaskDisplayRouter(m,Manager.class);r.restore();assertEquals(22,m.resumed);assertEquals(0,m.all.get(0).displayId);assertTrue(m.moves.isEmpty());
 }
 @Test public void appsStillUseRecentsResume()throws Exception{
  Manager m=new Manager();m.all.add(new Info(12,0,1));new TaskDisplayRouter(m,Manager.class).move(0,1,false);assertEquals(12,m.resumed);assertEquals(1,m.all.get(0).displayId);assertTrue(m.moves.isEmpty());
 }
 @Test public void backFocusUsesTheAppOnTheRequestedDisplay()throws Exception{
  Manager m=new Manager();m.all.add(new Info(7,0,2));m.all.add(new Info(12,1,1));new TaskDisplayRouter(m,Manager.class).focusTop(1);assertEquals(12,m.focused);
 }
 @Test public void systemTasksRemainExcluded()throws Exception{
  Manager m=new Manager();m.all.add(new Info(12,0,3));assertThrows(UnsupportedOperationException.class,()->new TaskDisplayRouter(m,Manager.class).move(0,1,false));assertEquals(-1,m.resumed);
 }
}
