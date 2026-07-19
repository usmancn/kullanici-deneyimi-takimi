package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.sim.engine.EntityManager;
import com.radar.gl.core.Camera;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Asagidan yukari suzulen tarama cizgisi.
 * Varliklari {@link EntityManager}'dan alir.
 */
public class ScanLine {

    private static final float SCAN_PERIOD_SEC = 10f;
    private static final float LINE_THICKNESS  = 3f;

    private final TargetGeometry  geometry;
    private final EntityManager   entityManager;

    private float scanY      = 0f;
    private long  lastTimeNs = 0L;
    private final List<ISimulationEntity> detected = new ArrayList<>();
    private final float[] matrix = new float[16];

    public ScanLine(TargetGeometry geometry, EntityManager entityManager) {
        this.geometry      = geometry;
        this.entityManager = entityManager;
    }

    public List<ISimulationEntity> detected() { return detected; }
    public float scanY()                       { return scanY; }

    public void reset() {
        scanY      = 0f;
        lastTimeNs = 0L;
        detected.clear();
    }

    /**
     * Zaman ilerlet: bandi kaydir, tespit edilen varliklar listesini guncelle.
     * Fizigi Osman'in SimulationEngine'i zaten kendi thread'inde ilerletiyor;
     * biz sadece tarama pozisyonunu yonetiyoruz.
     */
    public void advance() {
        final float resolution = Camera.WORLD_SIZE;

        long now = System.nanoTime();
        if (lastTimeNs == 0L) { lastTimeNs = now; return; }
        float deltaSec = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;

        float bandStart = scanY;
        float bandEnd   = scanY + (resolution * deltaSec) / SCAN_PERIOD_SEC;

        detected.clear();
        boolean wrapped = false;
        if (bandEnd >= resolution) {
            wrapped = true;
            bandEnd   = bandEnd % resolution;
        }

        List<ISimulationEntity> snapshot = entityManager.getAll();
        for (int i = 0; i < snapshot.size(); i++) {
            ISimulationEntity entity = snapshot.get(i);
            double y = entity.getPosition().y;
            
            if (wrapped) {
                // Wrap-around olduysa, bandStart'tan sona kadar VEYA 0'dan bandEnd'e kadar
                if ((y >= bandStart && y < resolution) || (y >= 0 && y < bandEnd)) {
                    detected.add(entity);
                }
            } else {
                if (y >= bandStart && y < bandEnd) {
                    detected.add(entity);
                }
            }
        }

        scanY = bandEnd;
    }

    /** Ana ekranda tarama bandini cizer. */
    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        float thickness = LINE_THICKNESS * camera.rangeY() / Camera.WORLD_SIZE;

        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.greenColor.id());
        camera.modelMatrix(matrix, camera.centerX(), scanY, camera.rangeX(), thickness);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
    }
}
