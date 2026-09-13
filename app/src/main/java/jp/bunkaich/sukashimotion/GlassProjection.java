package jp.bunkaich.sukashimotion;

/** Perceptual compensation calibrated against the accepted launcher, not an eye tracker.
 * The phone already supplies the physical rotation; the image needs only a gentle offset. */
final class GlassProjection {
    record Pose(float expansion,float taper) {}
    record Point(float x,float y) {}
    static Pose pose(float angle,boolean inner){
        float rotation=inner?180-clamp(angle):clamp(angle);
        float t=Math.min(1,rotation/90);t=t*t*(3-2*t);
        if(!inner)return new Pose(.39f*t,.144f*t);
        // Keep the left image close to its open position, with a little additional depth
        // beyond 90 degrees. The hinge and the whole right panel remain fixed.
        float beyond=Math.max(0,(rotation-90)/90);beyond=beyond*beyond*(3-2*beyond);
        return new Pose(.12f*t+.03f*beyond,.06f*t+.015f*beyond);
    }
    static float clamp(float angle){return Math.max(0,Math.min(180,angle));}
    static float rearWeight(float angle){float t=Math.max(0,Math.min(1,(angle-20)/60));return t*t*(3-2*t);}
    static Point sample(float x,float y,float width,float height,float angle,boolean inner){
        float page=inner?width*.5f:width,hinge=inner?page:0;
        if(inner&&x>=hinge)return new Point(x,y);
        Pose p=pose(angle,inner);float distance=(inner?(hinge-x):x)/page;
        float u=distance/(1+p.expansion),heightScale=1-p.taper*u;
        return new Point(hinge+(inner?-1:1)*page*u,height*.5f+(y-height*.5f)/heightScale);
    }
}
