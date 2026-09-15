package jp.bunkaich.sukashimotion;

import android.content.Context;
import android.graphics.*;
import android.widget.FrameLayout;

/** Applies the physical pane transform AFTER the shader, to inspect world-space continuity. */
final class PreviewRig extends FrameLayout {
    private final SnapshotView snapshot;private final FrameTexture innerFrame;
    private float angle;private boolean physical=true;
    private final Matrix matrix=new Matrix();private final Paint outline=new Paint(3);
    PreviewRig(Context context,SnapshotView snapshot,FrameTexture innerFrame){
        super(context);this.snapshot=snapshot;this.innerFrame=innerFrame;angle=snapshot.inner?180:0;
        addView(snapshot,new LayoutParams(-1,-1));outline.setStyle(Paint.Style.STROKE);outline.setStrokeWidth(2);outline.setColor(0xff9da8aa);
        setContentDescription(context.getString(R.string.rig_description));
    }
    void setAngle(float value){angle=value;snapshot.setAngle(value);invalidate();}
    void setPhysical(boolean value){physical=value;invalidate();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
        setPadding(Math.round(w*.04f),Math.round(h*.14f),Math.round(w*.04f),Math.round(h*.14f));
        super.onMeasure(widthSpec,heightSpec);
    }
    @Override protected void dispatchDraw(Canvas canvas){
        if(!physical){super.dispatchDraw(canvas);return;}
        int w=snapshot.getWidth(),h=snapshot.getHeight();canvas.save();canvas.translate(snapshot.getLeft(),snapshot.getTop());boolean inner=snapshot.inner;float edge=inner?w*.5f:w;
        if(inner){canvas.save();canvas.clipRect(edge,0,w,h);snapshot.draw(canvas);canvas.restore();}
        else if(innerFrame!=null){
            Bitmap b=innerFrame.sharp;
            canvas.drawBitmap(b,new Rect(b.getWidth()/2,0,b.getWidth(),b.getHeight()),new Rect(0,0,w,h),null);
        }
        // The opposite face is not visible from this reference eye after the pane passes 90°.
        if(inner?angle<=90:angle>=90){canvas.restore();return;}
        float[] points={0,0,edge,0,edge,h,0,h},projected=new float[8];
        for(int i=0;i<4;i++){
            // Use the same fixed reference distance as the inner image plane.
            float hinge=inner?edge:0,sign=inner?-1:1,distance=(points[i*2]-hinge)*sign/edge;
            double r=Math.toRadians(inner?180-angle:angle);float denominator=1-distance*(float)Math.sin(r)/(inner?GlassProjection.REFERENCE_DISTANCE:4);
            projected[i*2]=hinge+sign*edge*distance*(float)Math.cos(r)/denominator;
            projected[i*2+1]=h*.5f+(points[i*2+1]-h*.5f)/denominator;
        }
        if(!matrix.setPolyToPoly(points,0,projected,0,4)){canvas.restore();return;}
        canvas.save();canvas.concat(matrix);canvas.clipRect(0,0,edge,h);
        snapshot.draw(canvas);canvas.restore();
        Path boundary=new Path();boundary.moveTo(projected[0],projected[1]);for(int i=1;i<4;i++)boundary.lineTo(projected[i*2],projected[i*2+1]);boundary.close();canvas.drawPath(boundary,outline);canvas.restore();
    }
}
