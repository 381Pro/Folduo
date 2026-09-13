package jp.bunkaich.sukashimotion;
import android.os.*;
import android.graphics.Bitmap;
/** Invoked only by local emulator diagnostics. Not present in the release APK. */
public final class ShellSmoke {
 public static void main(String[] args)throws Exception{
  Looper.prepareMainLooper();ShellBridge bridge=new ShellBridge();
  bridge.startAngles(new IAngleSink.Stub(){public void angle(float a,long t,int source){}});
  Thread.sleep(1500);bridge.heartbeat();Bundle report=bridge.inspect();System.out.println("SENSORS "+report);
  Bundle capture=bridge.capture(0);Bitmap frame=capture.getParcelable("frame",Bitmap.class);
  if(frame==null)System.out.println("CAPTURE_ERROR "+capture.getString("error"));else{
   // Exercise the actual Bitmap parcel round trip used by the Shizuku binder.
   Parcel parcel=Parcel.obtain();capture.writeToParcel(parcel,0);parcel.setDataPosition(0);Bundle received=Bundle.CREATOR.createFromParcel(parcel);received.setClassLoader(ShellSmoke.class.getClassLoader());
   Bitmap copy=received.getParcelable("frame",Bitmap.class);Bitmap cpu=copy.copy(Bitmap.Config.ARGB_8888,false);
   System.out.println("CAPTURE_OK "+cpu.getWidth()+"x"+cpu.getHeight()+" parcelBytes="+parcel.dataSize());parcel.recycle();
  }
  bridge.stopAngles();bridge.release();bridge.destroy();
 }
}
