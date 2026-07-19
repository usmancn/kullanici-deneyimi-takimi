package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.gl.core.Camera;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.sim.model.Vector2D;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dairesel radarda tespit edilen hedefleri cizer.
 * Acisal uzakliga (angular distance) gore hedefleri sonuklestirir.
 */
public class CircularTargetLayer {

    private static final float TARGET_SIZE = 10f;

    public static class CircularBlip {
        public float x, y;
        public float hitAngle;
        public java.util.List<float[]> trail = new java.util.ArrayList<>();
    }

    private final TargetGeometry geometry;
    private final float[] matrix = new float[16];
    private final Map<UUID, CircularBlip> memory = new HashMap<>();

    public CircularTargetLayer(TargetGeometry geometry) { this.geometry = geometry; }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<ISimulationEntity> detected, float currentAngle) {
                     
        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;

        for (int i = 0; i < detected.size(); i++) {
            ISimulationEntity entity = detected.get(i);
            CircularBlip blip = memory.computeIfAbsent(entity.getId(), k -> new CircularBlip());
            Vector2D pos = entity.getPosition();
            
            if (blip.trail.isEmpty()) {
                blip.x = (float) pos.x;
                blip.y = (float) pos.y;
                blip.trail.add(new float[]{blip.x, blip.y});
            } else {
                if (Math.abs(blip.x - pos.x) > 0.5f || Math.abs(blip.y - pos.y) > 0.5f) {
                    blip.trail.add(new float[]{blip.x, blip.y});
                    if (blip.trail.size() > 15) {
                        blip.trail.remove(0);
                    }
                }
                blip.x = (float) pos.x;
                blip.y = (float) pos.y;
            }
            
            double angle = Math.atan2(pos.y - cy, pos.x - cx);
            if (angle < 0) angle += 2 * Math.PI;
            blip.hitAngle = (float) angle;
        }

        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());

        Iterator<Map.Entry<UUID, CircularBlip>> it = memory.entrySet().iterator();
        while (it.hasNext()) {
            CircularBlip blip = it.next().getValue();
            
            // Acisal mesafeyi hesapla (wrap-around)
            float angularDistance = currentAngle - blip.hitAngle;
            if (angularDistance < 0) {
                angularDistance += (float) (2 * Math.PI);
            }

            // En fazla %80 oraninda kararacak
            float opacity = 1.0f - 0.8f * (angularDistance / (float)(2 * Math.PI));
            if (opacity < 0.2f) opacity = 0.2f;

            for (int j = 0; j < blip.trail.size(); j++) {
                float[] oldPos = blip.trail.get(j);
                float ageFactor = (float)(j + 1) / (blip.trail.size() + 1);
                float pointOpacity = opacity * ageFactor * 0.6f;
                if (pointOpacity < 0.05f) pointOpacity = 0.05f;
                
                shader.setTint(gl, 1.0f, 1.0f, 1.0f, pointOpacity);
                camera.modelMatrix(matrix, oldPos[0], oldPos[1], TARGET_SIZE * 0.7f, TARGET_SIZE * 0.7f);
                shader.setMatrix(gl, matrix);
                gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
            }

            shader.setTint(gl, 1.0f, 1.0f, 1.0f, opacity);
            camera.modelMatrix(matrix, blip.x, blip.y, TARGET_SIZE, TARGET_SIZE);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }

        shader.resetTint(gl);
    }
}
