package jp.bunkaich.sukashimotion;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlassProjectionTest {
 @Test public void endpointsAreIdentity(){
  for(boolean inner:new boolean[]{true,false})for(int x=0;x<=1000;x+=25)for(int y=0;y<=1000;y+=25){
   var q=GlassProjection.sample(x,y,1000,1000,inner?180:0,inner);assertEquals(x,q.x(),.001f);assertEquals(y,q.y(),.001f);
  }
 }
 @Test public void hingeNeverMoves(){
  for(float a=0;a<=180;a+=.5f)for(int y=0;y<=1000;y+=50){var q=GlassProjection.sample(500,y,1000,1000,a,true);assertEquals(500,q.x(),0);assertEquals(y,q.y(),0);}
 }
 @Test public void rightSideNeverMoves(){
  for(float a=0;a<=180;a+=5)for(int x=500;x<=1000;x+=25){var q=GlassProjection.sample(x,230,1000,1000,a,true);assertEquals(x,q.x(),0);assertEquals(230,q.y(),0);}
 }
 @Test public void ninetyDegreesNeverDividesByZero(){
  for(boolean inner:new boolean[]{true,false})for(float a=89.5f;a<90.5f;a+=.01f)for(int x=0;x<=1000;x+=5){var q=GlassProjection.sample(x,0,1000,1000,a,inner);assertTrue(Float.isFinite(q.x()));assertTrue(Float.isFinite(q.y()));assertTrue(Math.abs(q.y())<=200);}
 }
 @Test public void hiddenFaceNeverMirrorsOrReappears(){
  for(float angle=0;angle<=90;angle+=.5f)for(int x=0;x<500;x+=25){
   var edge=GlassProjection.sample(x,250,1000,1000,90,true);
   var hidden=GlassProjection.sample(x,250,1000,1000,angle,true);
   assertEquals(edge.x(),hidden.x(),.001f);assertEquals(edge.y(),hidden.y(),.001f);
   assertTrue(hidden.x()<=500);
  }
 }
 @Test public void coverKeepsItsAcceptedProjection(){
  for(float angle=0;angle<=90;angle+=.5f){
   var front=GlassProjection.sample(1000,0,1000,2200,angle,false);
   assertTrue("Cover widens by no more than the accepted launcher",front.x()>=719&&front.x()<=1000);
   assertTrue("Vertical distortion stays below 6 percent of height",front.y()>=-130&&front.y()<=0);
  }
 }
 @Test public void innerContentNeverMovesOrStretchesHorizontally(){
  // Width is invariant on the display, even around the former 90-degree singularity.
  for(float angle=0;angle<=180;angle+=.25f)for(int x=0;x<=500;x+=10){
   var q=GlassProjection.sample(x,250,1000,1000,angle,true);
   var next=GlassProjection.sample(x+10,250,1000,1000,angle,true);
   assertEquals(x,q.x(),0);assertEquals(10,next.x()-q.x(),0);
  }
 }
 @Test public void verticalCompensationKeepsTheAcceptedPlaneHeight(){
  // Apply physical vertical projection independently. The horizontal image position
  // intentionally stays on the panel, rather than stretching to counter its rotation.
  double half=500,eyeDistance=half*GlassProjection.REFERENCE_DISTANCE;
  for(double angle:new double[]{179,170,150,135,120,100,91}){
   double beta=Math.toRadians(180-angle),sin=Math.sin(beta);
   for(double s=10;s<half;s+=10){
    double z=s*sin,scale=(eyeDistance-z)/eyeDistance;
    for(double y:new double[]{200,500,800}){
     var q=GlassProjection.sample((float)(half-s),(float)(500+(y-500)*scale),1000,1000,(float)angle,true);
     assertEquals(y,q.y(),.001);
    }
   }
  }
 }
 @Test public void innerFarEdgeKeepsItsHeightInsteadOfLookingPinched(){
  for(float angle=90;angle<=180;angle+=.5f){
   float visibleHeight=1-GlassProjection.innerPlane(angle).depth();
   assertTrue("Far-edge height must remain above 93%",visibleHeight>=.93f);
  }
  float depth=GlassProjection.innerPlane(120).depth();
  assertEquals("120-degree total height reduction",.05413f,depth,.0001f);
  // The top of the image is inset by half the lost height, symmetrically.
  assertEquals(0,GlassProjection.sample(0,1100*depth/2,1000,1100,120,true).y(),.001f);
 }
 @Test public void farEdgeMovesOutwardOnCover(){
  // Find where the 60% landmark will be drawn; it must move right, never left.
  for(float a=10;a<=90;a+=5){
   float low=600,high=1000;
   for(int i=0;i<20;i++){float x=(low+high)/2;if(GlassProjection.sample(x,500,1000,2200,a,false).x()<600)low=x;else high=x;}
   assertTrue(low>600);assertTrue(high<=835);
  }
 }
 @Test public void frontClearsIntoItsOwnContentAtClosedEndpoint(){assertEquals(0,GlassProjection.rearWeight(0),0);assertEquals(1,GlassProjection.rearWeight(80),0);}
}
