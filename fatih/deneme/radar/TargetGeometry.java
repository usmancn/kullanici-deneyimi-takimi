package deneme.radar;

import com.jogamp.opengl.GL2;

import deneme.radar.gl.Geometry;
import deneme.radar.gl.GlBuffer;

/**
 * Hedef karesi (sekizgen) geometrisi ve renkleri.
 * Hem hedef katmani, hem tarama cizgisi, hem de minimap ayni geometriyi
 * paylastigi icin VBO'lar tek yerde tutulur.
 */
public class TargetGeometry {

    final GlBuffer position    = new GlBuffer();
    final GlBuffer targetColor = new GlBuffer();
    final GlBuffer greenColor  = new GlBuffer();

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
