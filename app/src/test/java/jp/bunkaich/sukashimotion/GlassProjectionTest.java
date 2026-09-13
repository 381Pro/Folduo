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
 @Test public void geometryStillChangesWhenBlurIsSaturated(){
  assertEquals(FoldPolicy.blur(90,true),FoldPolicy.blur(60,true),0);
  assertNotEquals(GlassProjection.sample(100,250,1000,1000,90,true).x(),GlassProjection.sample(100,250,1000,1000,60,true).x(),1);
 }
 @Test public void projectionStaysGentleAcrossTheVisibleRange(){
  for(float angle=0;angle<=90;angle+=.5f){
   var front=GlassProjection.sample(1000,0,1000,2200,angle,false);
   assertTrue("Cover widens by no more than the accepted launcher",front.x()>=719&&front.x()<=1000);
   assertTrue("Vertical distortion stays below 6 percent of height",front.y()>=-130&&front.y()<=0);
   var left=GlassProjection.sample(0,0,1000,1100,180-angle,true);
   assertTrue("Left UI stays near its open position",left.x()>=0&&left.x()<=54);
   assertTrue("Left taper stays gentle",left.y()>=-32&&left.y()<=0);
  }
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
