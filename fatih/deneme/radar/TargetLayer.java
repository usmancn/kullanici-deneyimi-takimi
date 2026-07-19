package deneme.radar;

import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import deneme.radar.gl.Geometry;
import deneme.radar.gl.ShaderProgram;
import deneme.sim.Contact;
import deneme.sim.GainFilter;

/**
 * Tarama cizgisinin gordugu hedefleri cizer.
 * Her hedef, gemi cesidinin gain factor'une gore farkli parlaklikta gorunur
 * (tint uniform'u). Filtre esigini gecemeyen hedefler anlik olarak gizlenir.
 */
public class TargetLayer {

    private final TargetGeometry geometry;
    private final float[] matrix = new float[16];

    public TargetLayer(TargetGeometry geometry) { this.geometry = geometry; }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<Contact> detected, GainFilter filter) {
        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());

        for (int i = 0; i < detected.size(); i++) {
            Contact c = detected.get(i);
            if (!filter.accepts(c.gain)) continue;

            float brightness = 0.35f + 0.65f * c.gain;      // gain -> parlaklik
            shader.setTint(gl, brightness, brightness, brightness, 1f);

            camera.modelMatrix(matrix, c.x, c.y, c.size, c.size);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }

        shader.resetTint(gl);
    }
}
