package jp.bunkaich.sukashimotion;

import android.os.*;

/** Binder-scoped: system UI releases this request even if the helper dies. */
final class StatusBarControl {
    private final IBinder token=new Binder();
    private final Class<?> api;
    private final Object service;
    private boolean hidden;
    StatusBarControl()throws Exception{
        api=Class.forName("com.android.internal.statusbar.IStatusBarService");
        Object binder=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"statusbar");
        service=Class.forName(api.getName()+"$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
    }
    synchronized void hide(boolean value)throws Exception{
        if(hidden==value)return;
        Class<?> constants=Class.forName("android.app.StatusBarManager");int flags=0;
        if(value)for(String name:new String[]{"DISABLE_CLOCK","DISABLE_NOTIFICATION_ICONS","DISABLE_SYSTEM_INFO"})flags|=constants.getField(name).getInt(null);
        // Do not disable notification shade, navigation, calls, or privacy indicators.
        api.getMethod("disable",int.class,IBinder.class,String.class).invoke(service,flags,token,"com.android.shell");hidden=value;
    }
    synchronized boolean hidden(){return hidden;}
}
