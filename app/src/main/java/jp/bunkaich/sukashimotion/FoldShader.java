package jp.bunkaich.sukashimotion;

/** Gentle parallax through an opaque frosted image; the optical edge is blurred too. */
final class FoldShader {
    static final String CODE = """
        uniform shader content;
        uniform shader rest1; uniform shader rest2;
        uniform shader rest4; uniform shader rest12; uniform shader rest28; uniform shader rest60;
        uniform shader rear; uniform shader rear1; uniform shader rear2; uniform shader rear4; uniform shader rear12; uniform shader rear28; uniform shader rear60;
        uniform float2 size; uniform float2 cacheScale; uniform float2 rearCacheScale;
        uniform float2 pose;
        uniform float amount; uniform float inner;
        uniform float radiusDp; uniform float rearBlend; uniform float pixelsPerDp;
        half blurWeight(float radius,float lower,float upper) {
            // Gaussian variance, not radius, is additive when mixing cached images.
            // This keeps the effective radius proportional to the hinge angle.
            return half(clamp((radius*radius-lower*lower)/(upper*upper-lower*lower),0.0,1.0));
        }
        half4 frost(float2 q,float radius) {
            float2 t=q*cacheScale;
            half4 sharp=content.eval(clamp(q,float2(.5),size-float2(.5)));
            if(radius<1.0)return mix(sharp,rest1.eval(t),blurWeight(radius,0.0,1.0));
            if(radius<2.0)return mix(rest1.eval(t),rest2.eval(t),blurWeight(radius,1.0,2.0));
            if(radius<4.0)return mix(rest2.eval(t),rest4.eval(t),blurWeight(radius,2.0,4.0));
            if(radius<12.0)return mix(rest4.eval(t),rest12.eval(t),blurWeight(radius,4.0,12.0));
            if(radius<28.0)return mix(rest12.eval(t),rest28.eval(t),blurWeight(radius,12.0,28.0));
            return mix(rest28.eval(t),rest60.eval(t),blurWeight(radius,28.0,60.0));
        }
        half4 rearFrost(float2 q,float radius) {
            float2 t=q*rearCacheScale;
            half4 sharp=rear.eval(clamp(q,float2(.5),size-float2(.5)));
            if(radius<1.0)return mix(sharp,rear1.eval(t),blurWeight(radius,0.0,1.0));
            if(radius<2.0)return mix(rear1.eval(t),rear2.eval(t),blurWeight(radius,1.0,2.0));
            if(radius<4.0)return mix(rear2.eval(t),rear4.eval(t),blurWeight(radius,2.0,4.0));
            if(radius<12.0)return mix(rear4.eval(t),rear12.eval(t),blurWeight(radius,4.0,12.0));
            if(radius<28.0)return mix(rear12.eval(t),rear28.eval(t),blurWeight(radius,12.0,28.0));
            return mix(rear28.eval(t),rear60.eval(t),blurWeight(radius,28.0,60.0));
        }
        half4 main(float2 p) {
            float page=size.x*mix(1.0,.5,inner);
            if(amount<=0.0 || (inner>.5 && p.x>=page))return half4(content.eval(p).rgb,1);
            float hinge=inner*page;
            float direction=mix(1.0,-1.0,inner);
            float distance=clamp((p.x-hinge)*direction/page,0.0,1.0);
            // Bounded expansion is calibrated to the original launcher. The real pane
            // already rotates, so projecting the full hinge angle again overstates depth.
            float u=distance/(1.0+pose.x);
            float denominator=1.0-pose.y*u;
            float2 q=float2(hinge+direction*page*u,
                size.y*.5+(p.y-size.y*.5)/denominator);
            float radius=radiusDp*amount*(.45+.55*sqrt(distance));
            // Frost and projection both join the stationary right pane at the exact hinge.
            if(inner>.5)radius=radiusDp*amount*2.0*(.35+.65*distance)*smoothstep(0.0,.16,distance);
            // Both images use the same angle-driven radius. Linking a prepared rear
            // image must not impose a sudden blur floor or change optical strength.
            half4 color=frost(q,radius);
            if(inner<.5 && rearBlend>0.0)color=mix(color,rearFrost(q,radius),half(rearBlend));
            // Blur the displaced top/bottom silhouette in IMAGE space, including its
            // black surround. Alpha stays opaque: this is optical softness, not a hole
            // through which the real, unblurred app can show. No hinge corner is added.
            float feather=max(.75,radius*pixelsPerDp*1.6);
            float edge=max(-q.y,q.y-size.y);
            float coverage=1.0-smoothstep(-feather,feather,edge);
            float taperVisible=smoothstep(0.0,.012,pose.y*u);
            coverage=mix(1.0,coverage,taperVisible);
            return half4(color.rgb*half(coverage),1);
        }
        """;
}
