package jp.bunkaich.sukashimotion;

import android.os.SystemClock;
import android.view.*;

/** Sends a paired key event directly; no command-line process on a button press. */
final class NavigationInput {
    static void back(int display)throws Exception{
        Class<?> wm=Class.forName("android.view.IWindowManager");
        Object binder=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");
        Object window=Class.forName(wm.getName()+"$Stub").getMethod("asInterface",android.os.IBinder.class).invoke(null,binder);
        wm.getMethod("moveDisplayToTop",int.class,String.class).invoke(window,display,"Sukashi user navigation");
        Class<?> api=Class.forName("android.hardware.input.InputManagerGlobal");
        Object manager=api.getMethod("getInstance").invoke(null);
        long now=SystemClock.uptimeMillis();boolean accepted=true;
        for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP}){
            KeyEvent event=new KeyEvent(now,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_BACK,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,KeyEvent.FLAG_FROM_SYSTEM|KeyEvent.FLAG_VIRTUAL_HARD_KEY,InputDevice.SOURCE_KEYBOARD);
            KeyEvent.class.getMethod("setDisplayId",int.class).invoke(event,display);
            accepted&=(boolean)api.getMethod("injectInputEvent",InputEvent.class,int.class).invoke(manager,event,2);
        }
        if(!accepted)throw new IllegalStateException("戻る操作が受け付けられませんでした");
    }
}
