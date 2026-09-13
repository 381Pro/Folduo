package jp.bunkaich.sukashimotion;

import android.content.Context;
import android.graphics.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RenderTest {
 private Bitmap render(boolean inner,float angle)throws Exception{
  return render(inner,angle,PreviewActivity.sample(640,720),null);
 }
 private Bitmap render(boolean inner,float angle,Bitmap source,Bitmap linked)throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  android.content.res.Configuration config=new android.content.res.Configuration(context.getResources().getConfiguration());config.densityDpi=160;
  Context renderContext=context.createConfigurationContext(config);
  FrameTexture frame=FrameTexture.prepare(source,1,()->false);
  FrameTexture rear=linked==null?null:FrameTexture.prepare(linked,1,()->false);
  android.media.ImageReader reader=android.media.ImageReader.newInstance(640,720,PixelFormat.RGBA_8888,2,android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE|android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT);
  HardwareRenderer renderer=new HardwareRenderer();renderer.setSurface(reader.getSurface());
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   SnapshotView view=new SnapshotView(renderContext,frame,inner,false);view.layout(0,0,640,720);if(rear!=null)view.setRearFrame(rear,false);view.setAngle(angle);
   RenderNode node=new RenderNode("fold-test");node.setPosition(0,0,640,720);Canvas c=node.beginRecording();view.draw(c);node.endRecording();
   renderer.setContentRoot(node);renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw();
  });
  android.media.Image image=null;
  for(int i=0;i<50&&image==null;i++){image=reader.acquireLatestImage();if(image==null)Thread.sleep(20);}
  assertNotNull("GPU frame available",image);
  android.hardware.HardwareBuffer buffer=image.getHardwareBuffer();
  Bitmap result=Bitmap.wrapHardwareBuffer(buffer,ColorSpace.get(ColorSpace.Named.SRGB)).copy(Bitmap.Config.ARGB_8888,false);
  buffer.close();image.close();renderer.destroy();reader.close();return result;
 }
 @Test public void rightPaneRemainsPixelIdentical()throws Exception{
  Bitmap open=render(true,180),folded=render(true,95);long difference=0;
  for(int y=5;y<715;y+=5)for(int x=324;x<635;x+=5)difference+=distance(open.getPixel(x,y),folded.getPixel(x,y));
  assertEquals("Right pane must not blur or slide",0,difference);
 }
 @Test public void leftPaneBlursWithoutReplacingContent()throws Exception{
  Bitmap open=render(true,180),folded=render(true,95);long difference=0;
  for(int y=40;y<680;y+=4)for(int x=20;x<270;x+=4)difference+=distance(open.getPixel(x,y),folded.getPixel(x,y));
  assertTrue("Left pane blur must be visible",difference>10000);
  assertTrue("The original green background must remain",Color.green(folded.getPixel(20,500))>Color.red(folded.getPixel(20,500)));
 }
 @Test public void innerSeamHasNoBlackCorner()throws Exception{
  Bitmap folded=render(true,90);for(int y:new int[]{0,1,10,710,719})assertTrue("No artificial corner on seam",Color.green(folded.getPixel(319,y))>20);
 }
 @Test public void coverShapeHasBlackOutsideAndVisibleCenter()throws Exception{
  Bitmap folded=render(false,70);assertTrue("Far outside remains black",Color.green(folded.getPixel(620,2))<5);assertTrue(Color.green(folded.getPixel(320,360))>20);
 }
 @Test public void shaderUsesCalibratedParallaxInsteadOfScreenPosition()throws Exception{
  Bitmap gradient=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);
  for(int x=0;x<640;x++)for(int y=0;y<720;y++)gradient.setPixel(x,y,Color.rgb(Math.round(x*255f/640),128,80));
  Bitmap result=render(true,120,gradient,null);int pixel=result.getPixel(100,360);
  float expected=GlassProjection.sample(100,360,640,720,120,true).x()*255/640;
  assertEquals("Gradient landmark follows the calibrated image plane",expected,Color.red(pixel),3);
  assertTrue("Parallax is present but restrained",Color.red(pixel)-100*255f/640>4&&Color.red(pixel)-100*255f/640<12);
 }
 @Test public void coverUsesInnerRightThenReturnsToOwnFrame()throws Exception{
  Bitmap cover=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);cover.eraseColor(Color.BLUE);
  Bitmap inside=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(inside);c.drawColor(Color.RED);Paint p=new Paint();p.setColor(Color.GREEN);c.drawRect(320,0,640,720,p);
  Bitmap folded=render(false,80,cover,inside),closed=render(false,0,cover,inside);
  int middle=folded.getPixel(400,360);
  assertTrue("Cover samples the paired INNER RIGHT image",Color.green(middle)>180&&Color.red(middle)<60&&Color.blue(middle)<10);
  assertEquals("Closed endpoint is exactly the actual cover image",Color.BLUE,closed.getPixel(400,360));
 }
 @Test public void frostedBoundaryHasWideFalloffAndNeverRevealsTheUnderlyingApp()throws Exception{
  Bitmap white=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);white.eraseColor(Color.WHITE);
  Bitmap folded=render(false,70,white,null);int intermediate=0,largestStep=0,previous=-1;
  for(int y=0;y<140;y++){
   int c=folded.getPixel(610,y),value=Color.red(c);
   if(value>10&&value<245)intermediate++;
   if(previous>=0)largestStep=Math.max(largestStep,Math.abs(value-previous));previous=value;
  }
  assertTrue("Frost extends into the black edge",intermediate>=30);
  assertTrue("No razor-sharp silhouette step",largestStep<=10);
  for(int y=0;y<720;y+=3)for(int x=0;x<640;x+=3)assertEquals("No clear holes exposing the real app",255,Color.alpha(folded.getPixel(x,y)));
 }
 @Test public void exchangingLayoutsDoesNotRetainSharpGhostLines()throws Exception{
  Bitmap cover=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888),inside=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);
  for(int y=0;y<720;y++)for(int x=0;x<640;x++){
   cover.setPixel(x,y,(x/8%2==0)?Color.WHITE:Color.BLACK);
   inside.setPixel(x,y,(y/8%2==0)?Color.BLACK:Color.WHITE);
  }
  for(float a:new float[]{26,35,50,65}){
   Bitmap folded=render(false,a,cover,inside);int low=255,high=0,rearLow=255,rearHigh=0;
   for(int x=100;x<540;x++){int value=Color.red(folded.getPixel(x,360));low=Math.min(low,value);high=Math.max(high,value);}
   for(int y=260;y<460;y++){int value=Color.red(folded.getPixel(320,y));rearLow=Math.min(rearLow,value);rearHigh=Math.max(rearHigh,value);}
   // Early in a gradual fold the ORIGINAL may still be readable. Distinct horizontal
   // and vertical details detect the regression we actually need to prevent: two
   // simultaneously readable layouts, not the presence of any remaining detail.
   assertTrue("No two readable layouts at "+a+" degrees: source="+(high-low)+" rear="+(rearHigh-rearLow),Math.min(high-low,rearHigh-rearLow)<=18);
  }
 }
 @Test public void temporaryDestinationContainsOnlyTheCurrentFrameAndPreparedFrost(){
  Bitmap source=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(source);c.drawColor(Color.RED);Paint p=new Paint();p.setColor(Color.BLUE);c.drawRect(320,0,640,720,p);
  FrameTexture original=FrameTexture.prepare(source,1,()->false);
  FrameTexture cover=original.transfer(true,320,720),inner=cover.transfer(false,640,720);
  assertTrue(cover.prepared&&inner.prepared);
  assertEquals(Color.BLUE,cover.sharp.getPixel(160,360));assertEquals(Color.BLUE,inner.sharp.getPixel(100,360));assertEquals(Color.BLUE,inner.sharp.getPixel(540,360));
  for(Bitmap level:inner.levels){int pixel=level.getPixel(level.getWidth()/4,level.getHeight()/2);assertTrue("Cropped right image remains blue after blur",Color.blue(pixel)>240&&Color.red(pixel)<15);}
 }
 @Test public void saveCalibratedRenderingSamples()throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  for(boolean inner:new boolean[]{false,true})for(int angle:new int[]{0,26,60,90,120,160,180}){
   if(inner&&angle<90||!inner&&angle>90)continue;
   Bitmap bitmap=render(inner,angle);
   try(var out=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"calibrated-"+(inner?"inner":"cover")+"-"+angle+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
  }
 }
 @Test public void cancellingTexturePreparationReturnsNoTexture(){Bitmap b=PreviewActivity.sample(320,360);assertNull(FrameTexture.prepare(b,1,()->true));}
 private Bitmap horizontalEdge(){
  Bitmap edge=Bitmap.createBitmap(640,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(edge);canvas.drawColor(Color.BLACK);
  Paint p=new Paint();p.setColor(Color.WHITE);canvas.drawRect(0,360,640,720,p);return edge;
 }
 @Test public void linkingRearImageCannotChangeBlurAtTheSameAngle()throws Exception{
  // Horizontal detail is identical in the full-width and cropped rear image.
  // This catches a blur floor driven by rearBlend, including late capture completion.
  Bitmap edge=horizontalEdge();
  for(float angle:new float[]{20,21,23,26,35,50,77,80}){
   Bitmap unlinked=render(false,angle,edge,null),linked=render(false,angle,edge,edge);int largest=0;
   for(int x=80;x<600;x+=13)for(int y=270;y<450;y++)largest=Math.max(largest,distance(unlinked.getPixel(x,y),linked.getPixel(x,y)));
   assertTrue("Pairing must not change optical strength at "+angle+" degrees: "+largest,largest<=3);
  }
 }
 private double edgeSigma(Bitmap image,int x){
  double total=0,first=0,second=0;
  for(int y=180;y<540;y++){
   double weight=Math.max(0,Color.red(image.getPixel(x,y+1))-Color.red(image.getPixel(x,y)));
   total+=weight;first+=weight*y;second+=weight*y*y;
  }
  assertTrue("Edge remains present",total>240);
  return Math.sqrt(Math.max(0,second/total-Math.pow(first/total,2)));
 }
 @Test public void slowOpeningHasNoBlurCliffAroundImageExchange()throws Exception{
  Bitmap edge=horizontalEdge();double previous=-1;StringBuilder samples=new StringBuilder("angle,sigma_pixels\n");
  for(int angle=18;angle<=32;angle++){
   double sigma=edgeSigma(render(false,angle,edge,edge),480);samples.append(angle).append(',').append(sigma).append('\n');
   if(previous>=0){assertTrue("No sudden blur increase at "+angle+": "+previous+" -> "+sigma,sigma-previous<.9);assertTrue("Opening must not sharpen at "+angle,sigma>=previous-.2);}
   previous=sigma;
  }
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(var out=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"blur-continuity.csv"))){out.write(samples.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 }
 @Test public void appHasNoHomeRoleIntent()throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME).setPackage(context.getPackageName());
  assertTrue(context.getPackageManager().queryIntentActivities(intent,0).isEmpty());
 }
 private int distance(int a,int b){return Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b));}
}
