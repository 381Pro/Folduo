package jp.bunkaich.sukashimotion;

import android.content.*;
import android.os.*;
import java.util.concurrent.*;
import rikka.shizuku.Shizuku;

final class BridgeConnection {
    static volatile IShellBridge bridge;
    static volatile String status="Shizukuへの接続を待っています";
    static final ExecutorService work=Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService pulse=Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean binding;private static boolean initialized;
    private static long bindingAt,nextAttempt;private static int failures;
    private static Shizuku.UserServiceArgs args;
    private static final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){synchronized(BridgeConnection.class){bridge=IShellBridge.Stub.asInterface(binder);status="補助処理に接続しました";binding=false;failures=0;nextAttempt=0;}}
        public void onServiceDisconnected(ComponentName name){synchronized(BridgeConnection.class){bridge=null;status="補助処理を再接続しています";binding=false;nextAttempt=0;}}
    };
    static synchronized void init(Context context){
        if(initialized)return;initialized=true;
        args=new Shizuku.UserServiceArgs(new ComponentName(context,ShellBridge.class)).daemon(false).processNameSuffix("motion_bridge").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE);
        Shizuku.addBinderDeadListener(()->{synchronized(BridgeConnection.class){bridge=null;binding=false;nextAttempt=0;status="Shizukuを起動すると自動で再接続します";}});
        pulse.scheduleWithFixedDelay(()->{IShellBridge b=bridge;if(b!=null)try{b.heartbeat();}catch(Exception e){synchronized(BridgeConnection.class){if(bridge==b){bridge=null;binding=false;status="補助処理を再接続しています";}}}},0,1,TimeUnit.SECONDS);
    }
    static boolean permitted(){try{return Shizuku.pingBinder()&&Shizuku.checkSelfPermission()==0;}catch(Exception e){return false;}}
    static synchronized void connect(Context context){
        init(context);if(bridge!=null)return;long now=SystemClock.elapsedRealtime();
        if(binding&&now-bindingAt<10000)return;
        if(binding){try{Shizuku.unbindUserService(args,connection,false);}catch(Exception ignored){}binding=false;}
        if(now<nextAttempt)return;
        nextAttempt=now+Math.min(15000,1000L<<Math.min(failures++,4));
        if(!permitted()){status="Shizukuを起動し、利用を許可してください";return;}
        try{binding=true;bindingAt=now;status="補助処理を接続しています";Shizuku.bindUserService(args,connection);}catch(Exception e){binding=false;status=ShellBridge.message(e);}
    }
    static void disconnect(){
        work.execute(()->{synchronized(BridgeConnection.class){try{if(args!=null)Shizuku.unbindUserService(args,connection,true);}catch(Exception ignored){}finally{bridge=null;binding=false;nextAttempt=0;failures=0;}}});
    }
}
