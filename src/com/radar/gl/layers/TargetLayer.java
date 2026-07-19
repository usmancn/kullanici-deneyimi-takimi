package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.gl.core.Camera;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.sim.model.Vector2D;

import java.util.List;

/**
 * Tarama cizgisinin gordugu hedefleri VBO ile yuksek hizda cizer.
 * Veriyi Osman'in EntityManager'indan gelen {@link ISimulationEntity} listesinden alir.
 */
public class TargetLayer {

    /** Hedef simgesinin dunya birimindeki boyutu. */
    private static final float TARGET_SIZE = 20f;

    private final TargetGeometry geometry;
    private final float[] matrix = new float[16];

    public TargetLayer(TargetGeometry geometry) { this.geometry = geometry; }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<ISimulationEntity> detected) {
        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());

        for (int i = 0; i < detected.size(); i++) {
            ISimulationEntity entity = detected.get(i);
            Vector2D pos = entity.getPosition();

            // Sabit parlaklik (1.0f)
            shader.setTint(gl, 1.0f, 1.0f, 1.0f, 1.0f);

            camera.modelMatrix(matrix,
                    (float) pos.x, (float) pos.y,
                    TARGET_SIZE, TARGET_SIZE);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }
    }
}
