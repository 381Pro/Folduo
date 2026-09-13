package jp.bunkaich.sukashimotion;

import android.app.ActivityOptions;
import android.os.Bundle;
import java.lang.reflect.*;
import java.util.*;

/** Move app and home tasks without swapping physical/logical display IDs. */
final class TaskDisplayRouter {
    private final Object manager;private final Class<?> api;
    private int lastDestination;
    private final Set<Integer> movedTasks=new LinkedHashSet<>();
    TaskDisplayRouter()throws Exception{
        manager=Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
        api=Class.forName("android.app.IActivityTaskManager");
    }
    TaskDisplayRouter(Object manager,Class<?> api){this.manager=manager;this.api=api;}
    private int number(Object info,String field)throws Exception{return info.getClass().getField(field).getInt(info);}
    private int activityType(Object info)throws Exception{
        Object configuration=info.getClass().getField("configuration").get(info);
        Object window=configuration.getClass().getField("windowConfiguration").get(configuration);
        return (int)window.getClass().getMethod("getActivityType").invoke(window);
    }
    private boolean standard(Object info)throws Exception{return activityType(info)==1;}
    private List<?> roots(int display)throws Exception{return (List<?>)api.getMethod("getAllRootTaskInfosOnDisplay",int.class).invoke(manager,display);}
    private Object home(int display)throws Exception{
        for(Object root:roots(display))if(activityType(root)==2&&root.getClass().getField("topActivity").get(root)!=null)return root;
        return null;
    }
    private void moveHome(Object root,int destination,boolean focus)throws Exception{
        // Samsung permits one HOME root per display. Use an already-created HOME,
        // never reparent a second HOME root into it.
        Object existing=home(destination);
        if(existing!=null)root=existing;
        int id=number(root,"taskId");
        if(number(root,"displayId")!=destination)api.getMethod("moveRootTaskToDisplayOnTopOrBottom",int.class,int.class,boolean.class).invoke(manager,id,destination,focus);
        if(focus)api.getMethod("setFocusedRootTask",int.class).invoke(manager,id);
        lastDestination=destination;
    }
    synchronized void showHome(int display)throws Exception{
        Object root=home(display);if(root==null)root=home(display==0?1:0);
        if(root==null)throw new IllegalStateException("@folduo/err_home_missing");
        moveHome(root,display,true);
    }
    private List<?> tasks(int display)throws Exception{return (List<?>)api.getMethod("getTasks",int.class,boolean.class,boolean.class,int.class).invoke(manager,1,false,false,display);}
    synchronized Bundle move(int source,int destination,boolean idle)throws Exception{
        Bundle result=new Bundle();List<?> tasks=tasks(source);
        if(!tasks.isEmpty()&&activityType(tasks.get(0))==2){
            Object root=home(source);if(root==null)throw new IllegalStateException("@folduo/err_source_home_missing");
            moveHome(root,destination,true);result.putBoolean("ok",true);result.putBoolean("moved",true);result.putBoolean("home",true);return result;
        }
        if(tasks.isEmpty()||!standard(tasks.get(0))){
            if(idle){result.putBoolean("ok",true);return result;}
            throw new UnsupportedOperationException("@folduo/err_system_screen");
        }
        Object task=tasks.get(0);int id=number(task,"taskId");
        if(number(task,"displayId")!=source)throw new IllegalStateException("@folduo/err_app_moved");
        // Recents restarts the existing task on its destination. A bare reparent left it undrawn
        // on this Fold7. The framework still checks launch/display and task restrictions.
        Bundle options=ActivityOptions.makeBasic().setLaunchDisplayId(destination).toBundle();
        api.getMethod("startActivityFromRecents",int.class,Bundle.class).invoke(manager,id,options);
        lastDestination=destination;movedTasks.add(id);result.putBoolean("ok",true);result.putBoolean("moved",true);result.putInt("taskId",id);return result;
    }
    synchronized ArrayList<Bundle> recentApps(android.content.Context context)throws Exception{
        Object slice=api.getMethod("getRecentTasks",int.class,int.class,int.class).invoke(manager,24,2,android.os.Process.myUid()/100000);
        List<?> recent=(List<?>)slice.getClass().getMethod("getList").invoke(slice);ArrayList<Bundle> result=new ArrayList<>();
        for(Object task:recent){
            if(!standard(task))continue;
            android.content.ComponentName component=(android.content.ComponentName)task.getClass().getField("realActivity").get(task);
            if(component==null||component.getPackageName().equals(BuildConfig.APPLICATION_ID))continue;
            Bundle row=new Bundle();row.putInt("taskId",number(task,"taskId"));
            try{
                android.content.pm.ApplicationInfo info=context.getPackageManager().getApplicationInfo(component.getPackageName(),0);
                row.putString("label",String.valueOf(info.loadLabel(context.getPackageManager())));
                android.graphics.drawable.Drawable icon=info.loadIcon(context.getPackageManager());
                android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(96,96,android.graphics.Bitmap.Config.ARGB_8888);
                icon.setBounds(0,0,96,96);icon.draw(new android.graphics.Canvas(bitmap));row.putParcelable("icon",bitmap);
            }catch(android.content.pm.PackageManager.NameNotFoundException unavailable){continue;}
            result.add(row);if(result.size()>=12)break;
        }
        return result;
    }
    synchronized void selectRecent(int taskId,int display)throws Exception{
        Object slice=api.getMethod("getRecentTasks",int.class,int.class,int.class).invoke(manager,64,2,android.os.Process.myUid()/100000);
        for(Object task:(List<?>)slice.getClass().getMethod("getList").invoke(slice))if(number(task,"taskId")==taskId&&standard(task)){
            api.getMethod("startActivityFromRecents",int.class,Bundle.class).invoke(manager,taskId,ActivityOptions.makeBasic().setLaunchDisplayId(display).toBundle());lastDestination=display;movedTasks.add(taskId);focusTop(display);return;
        }
        throw new IllegalStateException("@folduo/err_app_finished");
    }
    synchronized void focusTop(int display)throws Exception{
        List<?> top=tasks(display);if(top.isEmpty())return;
        int task=number(top.get(0),"taskId");
        api.getMethod("setFocusedTask",int.class).invoke(manager,task);
    }
    private int settingsTask()throws Exception{
        Object slice=api.getMethod("getRecentTasks",int.class,int.class,int.class).invoke(manager,64,2,android.os.Process.myUid()/100000);
        for(Object task:(List<?>)slice.getClass().getMethod("getList").invoke(slice)){
            android.content.ComponentName component=(android.content.ComponentName)task.getClass().getField("realActivity").get(task);
            if(standard(task)&&component!=null&&"com.android.settings".equals(component.getPackageName()))return number(task,"taskId");
        }
        return -1;
    }
    synchronized void openSettings(android.content.Context context,int display)throws Exception{
        int task=settingsTask();
        if(task<0){
            // Samsung rejects a new caller on this private auxiliary panel. Launch
            // the app normally first, then use the supported existing-task route.
            android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent,ActivityOptions.makeBasic().setLaunchDisplayId(0).toBundle());
            for(int i=0;i<20&&task<0;i++){android.os.SystemClock.sleep(50);task=settingsTask();}
        }
        if(task<0)throw new IllegalStateException("@folduo/err_settings_missing");
        selectRecent(task,display);lastDestination=display;
    }
    synchronized android.graphics.Bitmap preview(int taskId)throws Exception{
        // Only use the platform's recents snapshot, which excludes protected windows.
        // A missing/unsupported snapshot is represented by the normal app icon.
        Object slice=api.getMethod("getRecentTasks",int.class,int.class,int.class).invoke(manager,64,2,android.os.Process.myUid()/100000);
        boolean allowed=false;
        for(Object task:(List<?>)slice.getClass().getMethod("getList").invoke(slice))if(number(task,"taskId")==taskId&&standard(task)){allowed=true;break;}
        if(!allowed)return null;
        Object snapshot=api.getMethod("getTaskSnapshot",int.class,boolean.class).invoke(manager,taskId,true);
        if(snapshot==null)return null;
        android.hardware.HardwareBuffer buffer=(android.hardware.HardwareBuffer)snapshot.getClass().getMethod("getHardwareBuffer").invoke(snapshot);
        if(buffer==null)return null;
        try{
            if((buffer.getUsage()&android.hardware.HardwareBuffer.USAGE_PROTECTED_CONTENT)!=0)return null;
            android.graphics.ColorSpace space=(android.graphics.ColorSpace)snapshot.getClass().getMethod("getColorSpace").invoke(snapshot);
            android.graphics.Bitmap hardware=android.graphics.Bitmap.wrapHardwareBuffer(buffer,space);if(hardware==null)return null;
            android.graphics.Bitmap cpu=hardware.copy(android.graphics.Bitmap.Config.ARGB_8888,false);hardware.recycle();if(cpu==null)return null;
            float scale=Math.min(1,320f/Math.max(cpu.getWidth(),cpu.getHeight()));
            android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(cpu,Math.max(1,Math.round(cpu.getWidth()*scale)),Math.max(1,Math.round(cpu.getHeight()*scale)),true);
            if(small!=cpu)cpu.recycle();return small;
        }finally{buffer.close();}
    }
    synchronized void restore()throws Exception{
        List<?> visible=tasks(1);Object top=visible.isEmpty()?null:visible.get(0);
        int active=top!=null&&standard(top)?number(top,"taskId"):-1;
        // Return the current app first. Do not sweep unrelated HOME roots or change
        // which unrelated application was selected after the fold.
        if(active>=0)api.getMethod("startActivityFromRecents",int.class,Bundle.class).invoke(manager,active,ActivityOptions.makeBasic().setLaunchDisplayId(0).toBundle());
        else if(top!=null&&activityType(top)==2){Object destination=home(0);if(destination!=null)api.getMethod("setFocusedRootTask",int.class).invoke(manager,number(destination,"taskId"));}
        for(Object root:roots(1))if(standard(root)&&movedTasks.contains(number(root,"taskId"))&&number(root,"taskId")!=active)
            api.getMethod("moveRootTaskToDisplayOnTopOrBottom",int.class,int.class,boolean.class).invoke(manager,number(root,"taskId"),0,false);
        movedTasks.clear();lastDestination=0;
    }
}
