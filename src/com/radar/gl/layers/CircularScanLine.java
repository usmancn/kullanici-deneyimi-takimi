package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.sim.engine.EntityManager;
import com.radar.gl.core.Camera;
import com.radar.gl.core.ShaderProgram;

import java.nio.FloatBuffer;
import com.jogamp.common.nio.Buffers;
import java.util.ArrayList;
import java.util.List;

/**
 * Dairesel radar icin donen tarama cizgisi (Sweep).
 * Aci tabanli calisir ve konik bir isik (ucgen dilimi) uretir.
 */
public class CircularScanLine {

    private static final float SCAN_PERIOD_SEC = 5f; // Tam tur suresi
    private final EntityManager entityManager;

    private float scanAngle  = 0f; // 0 to 2*PI
    private long  lastTimeNs = 0L;
    private final List<ISimulationEntity> detected = new ArrayList<>();
    private final float[] matrix = new float[16];

    // Konik isik cizimi icin buffer (Sweep Trail)
    private final FloatBuffer coneBuffer;
    private static final int CONE_SEGMENTS = 30; // Iz ucgenleri
    private static final float CONE_LENGTH_RAD = 1.0f; // Arkasinda birakan izin acisal uzunlugu

    public CircularScanLine(EntityManager entityManager) {
        this.entityManager = entityManager;
        coneBuffer = Buffers.newDirectFloatBuffer((CONE_SEGMENTS + 2) * 2);
    }

    public List<ISimulationEntity> detected() { return detected; }
    public float scanAngle()                  { return scanAngle; }

    public void reset() {
        scanAngle  = 0f;
        lastTimeNs = 0L;
        detected.clear();
    }

    public void advance() {
        long now = System.nanoTime();
        if (lastTimeNs == 0L) { lastTimeNs = now; return; }
        float deltaSec = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;

        float angleSpeed = (float) (2 * Math.PI) / SCAN_PERIOD_SEC;
        float prevAngle = scanAngle;
        scanAngle += angleSpeed * deltaSec;
        
        boolean wrapped = false;
        if (scanAngle >= 2 * Math.PI) {
            scanAngle -= 2 * Math.PI;
            wrapped = true;
        }

        detected.clear();
        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f;

        for (ISimulationEntity entity : entityManager.getAll()) {
            double ex = entity.getPosition().x;
            double ey = entity.getPosition().y;
            
            // Gemi radarin icinde mi?
            double distSq = (ex - cx)*(ex - cx) + (ey - cy)*(ey - cy);
            if (distSq > maxRadius * maxRadius) continue;

            // Acisini hesapla (0 ile 2*PI arasi)
            double angle = Math.atan2(ey - cy, ex - cx);
            if (angle < 0) angle += 2 * Math.PI;

            // Gemi eski aci ile yeni aci arasinda mi kaldi?
            boolean isHit = false;
            if (!wrapped) {
                if (angle >= prevAngle && angle <= scanAngle) isHit = true;
            } else {
                if (angle >= prevAngle || angle <= scanAngle) isHit = true;
            }

            if (isHit) {
                detected.add(entity);
            }
        }
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f;

        camera.modelMatrix(matrix, 0, 0, Camera.WORLD_SIZE, Camera.WORLD_SIZE);
        shader.setMatrix(gl, matrix);

        // 1. Ana Tarama Cizgisi
        gl.glLineWidth(3f);
        shader.setTint(gl, 0.4f, 1.0f, 0.4f, 0.9f);
        gl.glBegin(GL.GL_LINES);
        gl.glVertex2f(cx, cy);
        gl.glVertex2f(cx + (float) Math.cos(scanAngle) * maxRadius, cy + (float) Math.sin(scanAngle) * maxRadius);
        gl.glEnd();

        // 2. Tarama Iz Cizimi (Sweep Cone)
        coneBuffer.clear();
        coneBuffer.put(cx).put(cy); // Merkez
        for (int i = 0; i <= CONE_SEGMENTS; i++) {
            float a = scanAngle - (i * CONE_LENGTH_RAD / CONE_SEGMENTS);
            coneBuffer.put(cx + (float) Math.cos(a) * maxRadius);
            coneBuffer.put(cy + (float) Math.sin(a) * maxRadius);
        }
        coneBuffer.flip();

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL.GL_FLOAT, 0, coneBuffer);

        // Gradient efekti (merkezden disari) shader desteklemedigi icin opacity'i dusuk tutarak yarim saydam ucgen fani ciziyoruz
        shader.setTint(gl, 0.2f, 0.8f, 0.2f, 0.15f);
        gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, CONE_SEGMENTS + 2);
        
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }
}
