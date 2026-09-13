package jp.bunkaich.sukashimotion;

import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Permission-free visual preview uses generated content, never another app's screen. */
public final class PreviewActivity extends Activity {
    private final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private FrameLayout canvas;private SnapshotView snapshot;private PreviewRig rig;private boolean physical=true;private TextView degrees;private boolean inner=true,closed;private int angle=180,generation;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().setDecorFitsSystemWindows(false);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,24,16,32);root.setBackgroundColor(Color.BLACK);
        degrees=new TextView(this);degrees.setTextColor(Color.WHITE);degrees.setTextSize(18);root.addView(degrees);
        canvas=new FrameLayout(this);root.addView(canvas,new LinearLayout.LayoutParams(-1,0,1));
        SeekBar seek=new SeekBar(this);seek.setMax(180);seek.setProgress(180);seek.setContentDescription("開閉角度");root.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int value,boolean fromUser){angle=value;update();}});
        Button mode=new Button(this);mode.setText("外側の表示に切り替え");mode.setOnClickListener(v->{inner=!inner;mode.setText(inner?"外側の表示に切り替え":"内側の表示に切り替え");build();});root.addView(mode);
        Button view=new Button(this);view.setText("液晶に描く像を見る");view.setOnClickListener(v->{physical=!physical;view.setText(physical?"液晶に描く像を見る":"端末を正面から見る");if(rig!=null)rig.setPhysical(physical);update();});root.addView(view);
        Button back=new Button(this);back.setText("設定に戻る");back.setOnClickListener(v->finish());root.addView(back);setContentView(root);getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());canvas.post(this::build);update();
    }
    private void update(){degrees.setText((inner?"内側":"外側")+"のデモ · "+angle+"°\n"+(physical?"端末を正面から見た様子":"液晶に描く像"));if(rig!=null)rig.setAngle(angle);}
    private void build(){
        int ticket=++generation;boolean mode=inner;
        float aspect=(mode?.9f:.43f)*.72f/.92f;int h=Math.max(200,Math.min(canvas.getHeight(),Math.round(canvas.getWidth()/aspect)));int w=Math.round(h*aspect);
        worker.execute(()->{
            int imageW=w-Math.round(w*.04f)*2,imageH=h-Math.round(h*.14f)*2;
            Bitmap sample=sample(mode?imageW:imageW*2,imageH);
            FrameTexture innerFrame=FrameTexture.prepare(sample,getResources().getDisplayMetrics().density,()->closed||ticket!=generation);
            if(innerFrame==null)return;
            FrameTexture frame=mode?innerFrame:FrameTexture.prepare(Bitmap.createBitmap(sample,imageW,0,imageW,imageH),getResources().getDisplayMetrics().density,()->closed||ticket!=generation);
            runOnUiThread(()->{
                if(closed||ticket!=generation||frame==null)return;canvas.removeAllViews();snapshot=new SnapshotView(this,frame,mode,false);
                if(!mode)snapshot.setRearFrame(innerFrame,false);
                rig=new PreviewRig(this,snapshot,innerFrame);rig.setPhysical(physical);canvas.addView(rig,new FrameLayout.LayoutParams(w,h,Gravity.CENTER));update();
            });
        });
    }

    static Bitmap sample(int w,int h){
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);Paint p=new Paint(3);
        p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff173b38,0xff396457,0xff8faaa0},null,Shader.TileMode.CLAMP));c.drawPaint(p);p.setShader(null);
        p.setColor(0xffedfff7);p.setTextSize(w*.07f);c.drawText("いつもの画面",w*.06f,h*.14f,p);
        p.setTextSize(w*.032f);c.drawText("ガラスが動いても、像は奥に残る",w*.06f,h*.20f,p);
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){
            float x=w*(.06f+col*.235f),y=h*(.3f+row*.19f);p.setColor(new int[]{0xffe6bc8a,0xffbbd6d1,0xffcad9a7,0xffcfbad8}[(row+col)%4]);c.drawRoundRect(x,y,x+w*.18f,y+h*.13f,22,22,p);p.setColor(0xff254138);p.setTextSize(w*.065f);c.drawText(""+(1+row*4+col),x+w*.04f,y+h*.09f,p);
        }return bitmap;
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);canvas.post(this::build);}
    @Override public void onDestroy(){closed=true;++generation;worker.shutdownNow();super.onDestroy();}
}
