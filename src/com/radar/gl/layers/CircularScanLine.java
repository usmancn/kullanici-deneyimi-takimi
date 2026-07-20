package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.sim.engine.EntityManager;
import com.radar.gl.core.Camera;
import com.radar.gl.core.CircularProjection;
import com.radar.gl.core.ShaderProgram;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sonar Ripple: Dairesel radarda merkezden dista dogru buyuyen dalga.
 * Dairesel radar tasarimina uygun calisir.
 */
public class CircularScanLine {

    private static final float SCAN_PERIOD_SEC = 5f; // Tam dalga suresi
    private final EntityManager entityManager;

    private float scanRadius = 0f; // 0'dan maxRadius'a kadar artar
    private long  lastTimeNs = 0L;
    private final List<ISimulationEntity> detected = new ArrayList<>();
    private final float[] matrix = new float[16];
    
    // Grid katmaninin paylastigi cember VBO referansi
    private final FloatBuffer circleBuffer;
    private static final int CIRCLE_STEP = 128;

    public CircularScanLine(EntityManager entityManager, FloatBuffer circleBuffer) {
        this.entityManager = entityManager;
        this.circleBuffer = circleBuffer;
    }

    public List<ISimulationEntity> detected() { return detected; }
    public float scanRadius()                 { return scanRadius; }

    public void reset() {
        scanRadius = 0f;
        lastTimeNs = 0L;
        detected.clear();
    }

    public void advance() {
        long now = System.nanoTime();
        if (lastTimeNs == 0L) { lastTimeNs = now; return; }
        float deltaSec = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;

        float maxRadius = CircularProjection.maxRadius();
        float speed = maxRadius / SCAN_PERIOD_SEC;

        float prevRadius = scanRadius;
        scanRadius += speed * deltaSec;

        boolean wrapped = false;
        if (scanRadius >= maxRadius) {
            scanRadius -= maxRadius;
            wrapped = true;
        }

        detected.clear();

        for (ISimulationEntity entity : entityManager.getAll()) {
            // Polar esleme: y -> menzil (radius). Tum y degerleri diske sigdigindan
            // kirpma yok; hicbir gemi kaybolmaz.
            double targetRadius = CircularProjection.radius(entity.getPosition().y);

            // Gemi eski dalga capi ile yeni dalga capi arasinda mi kaldi?
            boolean isHit = false;
            if (!wrapped) {
                if (targetRadius >= prevRadius && targetRadius <= scanRadius) isHit = true;
            } else {
                if (targetRadius >= prevRadius || targetRadius <= scanRadius) isHit = true;
            }

            if (isHit) {
                detected.add(entity);
            }
        }
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;

        shader.setTint(gl, 0.3f, 1.0f, 0.4f, 0.9f); // Belirlenen tarama dalgasi rengi
        gl.glLineWidth(3f);
        
        shader.bindPositionOnly(gl, circleBuffer, 2);

        float size = scanRadius * 2f;
        camera.modelMatrix(matrix, cx, cy, size, size);
        shader.setMatrix(gl, matrix);
        
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_STEP);
    }
}
