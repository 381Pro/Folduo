package jp.bunkaich.sukashimotion;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;
import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
    private final Handler handler=new Handler();private TextView state,diagnostic;private boolean probing;
    private final Shizuku.OnRequestPermissionResultListener permission=(code,result)->{if(result==0)BridgeConnection.connect(this);};
    private final IAngleSink diagnosticSink=new IAngleSink.Stub(){public void angle(float a,long t,int kind){}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);BridgeConnection.init(this);Shizuku.addRequestPermissionResultListener(permission);
        getWindow().setNavigationBarColor(Color.rgb(16,23,20));
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(28),dp(24),dp(40));scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
        label(page,getString(R.string.app_name),30,Color.WHITE);label(page,"いつものアプリを、開閉の向こうへ。",17,0xffb3eed4);
        label(page,"既存のランチャーと共存する開閉演出アプリです。開閉中の像を奥に残し、手前の液晶がすりガラスとして動くように描きます。開始後に一度閉じると準備が完了します。使用中は前面を主画面に固定し、アプリだけを内外へ移します。開閉中の像を保ち、表示が整ってから元のアプリへつなぎます。",15,0xffc5d3cd);
        state=label(page,"",15,0xffb3eed4);
        button(page,"見え方を試す",()->startActivity(new Intent(this,PreviewActivity.class)));
        label(page,"開いた画面の操作",21,Color.WHITE);
        label(page,"内側の下端に、戻る・ホーム・最近使ったアプリ・設定を表示します。履歴では画面のカードを選んで切り替えます。操作バーはアプリの大きさを変えずに重なり、開閉中は隠れます。前面は純正の操作バーを使います。上部の時計・通知アイコンも開閉中だけ隠します。内側の操作バーはこのアプリの補助機能で、純正の通知パネルやジェスチャーを復元するものではありません。",14,0xffc5d3cd);
        label(page,"初回の準備",21,Color.WHITE);
        label(page,"Shizukuを端末内のワイヤレスデバッグで起動し、このアプリに利用を許可してください。再起動後はShizukuの起動が必要です。",14,0xffc5d3cd);
        button(page,"Shizukuを開く",()->{Intent launch=getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");if(launch!=null)startActivity(launch);else startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/guide/setup/")));});
        button(page,"Shizukuを接続",()->{
            if(!Shizuku.pingBinder()){new AlertDialog.Builder(this).setMessage("Shizukuが起動していません。Shizukuを開いて起動するか、公式の案内を確認してください。").setPositiveButton("公式の案内",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/guide/setup/")))).setNegativeButton("閉じる",null).show();return;}
            if(BridgeConnection.permitted())BridgeConnection.connect(this);else Shizuku.requestPermission(7);
        });
        button(page,"重ねて表示を許可",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        label(page,"使用中の画面を扱います",21,Color.WHITE);
        label(page,"開始後は、開閉時に他のアプリの画面を一時的に読み取り、角度に応じた立体視差とぼかしを重ねます。画像はメモリー内だけで使い、開閉終了・停止・ロック時に参照を破棄します。保存・送信はしません。保護された画面は取得しません。",14,0xffc5d3cd);
        label(page,"有効な間は両画面が点灯し、通常より電池を使います。停止・ロック時に通常の表示へ戻ります。既存版の角度補助処理を停止してから開始してください。同時に画面を制御する処理があれば、開始せず理由を表示します。",14,0xffc5d3cd);
        button(page,MotionSettings.enabled(this)?"アニメーションを再開":"画面の一時利用に同意して常時有効にする",this::startMotion);
        button(page,"停止して画面制御を解除",()->{MotionSettings.setEnabled(this,false);stopService(new Intent(this,MotionService.class));if(!MotionService.running)BridgeConnection.disconnect();});
        label(page,"止まったときの戻し方",21,Color.WHITE);
        label(page,"通常は自動で接続を回復します。通知の「再開」でもやり直せます。ロックを解除してから一度完全に閉じてください。端末を再起動した場合は、先にShizukuを起動してください。アプリの強制停止やAndroidの実行中アプリ一覧で停止した場合は、この画面から再開します。",14,0xffc5d3cd);
        label(page,"電池の設定でこのアプリとShizukuを「制限なし」にすると、バックグラウンドで止められにくくなります。画面消灯中は画面の取得と開閉演出を休止します。",14,0xffc5d3cd);
        button(page,"このアプリの電池設定を確認",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        label(page,"センサーの確認",21,Color.WHITE);
        label(page,"通常側と反対側のジャイロを別々に購読し、許可の有無と実際に届いた件数を確認します。2系統が使えるとは限りません。この診断だけでは画面を読み取りません。",14,0xffc5d3cd);
        button(page,"5秒間センサーを調べる",()->probe(0));
        diagnostic=label(page,"まだ実測していません",13,0xffd0dbd5);
        label(page,"Fold7 SM-F966Z向け・実機で調整中",12,0xff90a298);
        setContentView(scroll);handler.post(refresh);
    }
    private void startMotion(){
        if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"先に「重ねて表示」を許可してください",Toast.LENGTH_LONG).show();return;}
        if(!BridgeConnection.permitted()){Toast.makeText(this,"先にShizukuを接続してください",Toast.LENGTH_LONG).show();return;}
        if(!"SM-F966Z".equals(Build.MODEL)){Toast.makeText(this,"この版の画面制御はFold7 SM-F966Z向けです。見え方のデモは利用できます",Toast.LENGTH_LONG).show();return;}
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8);
        BridgeConnection.connect(this);MotionSettings.setEnabled(this,true);startForegroundService(new Intent(this,MotionService.class).setAction(MotionService.running?"restart":"start"));
        Toast.makeText(this,"一度閉じると準備が完了します",Toast.LENGTH_LONG).show();finish();
    }
    private void probe(int attempt){
        if(probing)return;
        BridgeConnection.connect(this);IShellBridge bridge=BridgeConnection.bridge;
        if(bridge==null){diagnostic.setText(BridgeConnection.status);if(attempt<30&&BridgeConnection.permitted())handler.postDelayed(()->probe(attempt+1),300);return;}
        probing=true;diagnostic.setText("購読と受信件数を確認しています…端末を少し動かしてください");
        boolean alreadyRunning=MotionService.running;
        BridgeConnection.work.execute(()->{try{
            if(!alreadyRunning)bridge.startAngles(diagnosticSink);
            handler.postDelayed(()->BridgeConnection.work.execute(()->{try{
                Bundle report=bridge.inspect();
                if(!alreadyRunning&&!MotionService.running)bridge.stopAngles();
                String formatted=formatReport(report);handler.post(()->{probing=false;diagnostic.setText(formatted);});
            }catch(Exception e){handler.post(()->{probing=false;diagnostic.setText(ShellBridge.message(e));});}}),5000);
        }catch(Exception e){handler.post(()->{probing=false;diagnostic.setText(ShellBridge.message(e));});}});
    }
    static String formatReport(Bundle b){
        StringBuilder text=new StringBuilder("補助処理の権限: ").append(b.getInt("uid")).append("\nSamsung独自権限: ").append(b.getBoolean("samsungPermission")?"あり":"なし").append("\n");
        ArrayList<Bundle> rows=b.getParcelableArrayList("sensors",Bundle.class);boolean gyro=false,sub=false;
        if(rows!=null)for(Bundle r:rows){
            long count=r.getLong("events");int type=r.getInt("type");
            if(type==4&&count>0)gyro=true;if((type==65689||type==65690)&&count>0)sub=true;
            text.append('\n').append(r.getString("name")).append(" [").append(type).append("]\n購読: ").append(r.getBoolean("registered")?"成功":"不可").append(" / 受信: ").append(count).append("件");
            if(type==36||type==65686)text.append(" / 公称分解能: ").append(r.getFloat("resolution")).append("°");
            if(r.containsKey("error"))text.append("\n").append(r.getString("error"));text.append('\n');
        }
        text.append("\n2系統のジャイロ: ").append(gyro&&sub?"双方から受信（角度への変換は未校正）":"双方の受信は確認できません");
        text.append("\n\n画面制御: ").append(b.getString("display"));if(!b.getString("error","").isEmpty())text.append("\n").append(b.getString("error"));return text.toString();
    }
    private final Runnable refresh=new Runnable(){public void run(){String recovery=MotionSettings.recovery(MainActivity.this);state.setText((MotionService.running?MotionService.status:"停止中 · "+BridgeConnection.status)+"\n常時有効: "+(MotionSettings.enabled(MainActivity.this)?"オン":"オフ")+"\n重ねて表示: "+(Settings.canDrawOverlays(MainActivity.this)?"許可済み":"未許可")+(recovery.isEmpty()?"":"\n直近の停止・回復: "+recovery));handler.postDelayed(this,400);}};
    @Override protected void onResume(){super.onResume();if(MotionSettings.enabled(this)&&!MotionService.running&&Settings.canDrawOverlays(this))startForegroundService(new Intent(this,MotionService.class).setAction("restore"));}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private TextView label(LinearLayout parent,String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(10),0,dp(10));v.setLineSpacing(dp(3),1);parent.addView(v);return v;}
    private void button(LinearLayout parent,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(-1,-2));}
    @Override protected void onDestroy(){handler.removeCallbacks(refresh);Shizuku.removeRequestPermissionResultListener(permission);super.onDestroy();}
}
