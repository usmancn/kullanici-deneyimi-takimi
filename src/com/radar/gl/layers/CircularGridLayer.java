package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.gl.core.Camera;
import com.radar.gl.core.ShaderProgram;

import java.nio.FloatBuffer;
import com.jogamp.common.nio.Buffers;

/**
 * Dairesel radar icin ic ice gecmis halkalar ve aci cizgileri cizer.
 */
public class CircularGridLayer {

    private final float[] matrix = new float[16];
    private final FloatBuffer vertexBuffer;
    private final int vertexCount;

    // Renk: hafif yesilimsi (kare izgara rengi)
    private static final float R = 0.05f;
    private static final float G = 0.35f;
    private static final float B = 0.15f;

    public CircularGridLayer() {
        // 5 adet ic ice halka, her halka 64 parca cizgi
        // 12 adet aci cizgisi (30 derecede bir)
        int numRings = 5;
        int segmentsPerRing = 64;
        int numAngleLines = 12;

        vertexCount = (numRings * segmentsPerRing * 2) + (numAngleLines * 2);
        vertexBuffer = Buffers.newDirectFloatBuffer(vertexCount * 2);

        float centerX = Camera.WORLD_SIZE / 2f;
        float centerY = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f;

        // Halkalari uret
        for (int i = 1; i <= numRings; i++) {
            float radius = maxRadius * ((float) i / numRings);
            for (int j = 0; j < segmentsPerRing; j++) {
                double angle1 = (j * 2 * Math.PI) / segmentsPerRing;
                double angle2 = ((j + 1) * 2 * Math.PI) / segmentsPerRing;

                vertexBuffer.put(centerX + (float)(Math.cos(angle1) * radius));
                vertexBuffer.put(centerY + (float)(Math.sin(angle1) * radius));

                vertexBuffer.put(centerX + (float)(Math.cos(angle2) * radius));
                vertexBuffer.put(centerY + (float)(Math.sin(angle2) * radius));
            }
        }

        // Aci cizgilerini uret
        for (int i = 0; i < numAngleLines; i++) {
            double angle = (i * 2 * Math.PI) / numAngleLines;
            vertexBuffer.put(centerX);
            vertexBuffer.put(centerY);
            vertexBuffer.put(centerX + (float)(Math.cos(angle) * maxRadius));
            vertexBuffer.put(centerY + (float)(Math.sin(angle) * maxRadius));
        }

        vertexBuffer.flip();
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        camera.modelMatrix(matrix, 0, 0, Camera.WORLD_SIZE, Camera.WORLD_SIZE);
        shader.setMatrix(gl, matrix);
        shader.setTint(gl, R, G, B, 0.4f);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL.GL_FLOAT, 0, vertexBuffer);

        gl.glLineWidth(1f);
        gl.glDrawArrays(GL.GL_LINES, 0, vertexCount);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }
}
