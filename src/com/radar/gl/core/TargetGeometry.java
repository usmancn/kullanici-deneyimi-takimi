package com.radar.gl.core;



import com.jogamp.opengl.GL2;


/**
 * Hedef karesi (sekizgen) geometrisi ve renkleri.
 * Hem hedef katmani, hem tarama cizgisi, hem de minimap ayni geometriyi
 * paylastigi icin VBO'lar tek yerde tutulur.
 */
public class TargetGeometry {

    public final GlBuffer position    = new GlBuffer();
    public final GlBuffer targetColor = new GlBuffer();
    public final GlBuffer greenColor  = new GlBuffer();

    public void init(GL2 gl) {
        position.upload(gl, Geometry.TARGET_VERTICES);
        targetColor.upload(gl, Geometry.targetColors());
        greenColor.upload(gl, Geometry.greenColors());
    }

    public void dispose(GL2 gl) {
        position.dispose(gl);
        targetColor.dispose(gl);
        greenColor.dispose(gl);
    }
}
