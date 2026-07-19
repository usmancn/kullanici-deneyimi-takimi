package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.gl.core.Camera;
import com.radar.gl.core.ShaderProgram;

import java.nio.FloatBuffer;
import com.jogamp.common.nio.Buffers;

/**
 * Dairesel radar icin ic ice gecmis halkalar ve aci cizgileri cizer.
 * Optimizasyon: Tek bir cember VBO'su uretip farkli yari caplar icin tekrar kullanir.
 */
public class CircularGridLayer {

    private final float[] matrix = new float[16];
    private final FloatBuffer circleBuffer;
    private final FloatBuffer lineBuffer;
    
    private static final int CIRCLE_STEP = 128;
    private static final int RADIAL_LINES = 12;

    // Renk: Fatih'in koyu yesil / parlak grid rengi
    private static final float R = 0.05f;
    private static final float G = 0.35f;
    private static final float B = 0.15f;

    public CircularGridLayer() {
        // 1. Cember Vertex'leri (Yaricap 1, Merkez 0,0)
        circleBuffer = Buffers.newDirectFloatBuffer(CIRCLE_STEP * 2);
        for (int i = 0; i < CIRCLE_STEP; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_STEP;
            circleBuffer.put((float) Math.cos(angle));
            circleBuffer.put((float) Math.sin(angle));
        }
        circleBuffer.flip();

        // 2. Aci Cizgileri (Merkezden distaki cembere kadar)
        lineBuffer = Buffers.newDirectFloatBuffer(RADIAL_LINES * 4);
        for (int i = 0; i < RADIAL_LINES; i++) {
            double angle = 2.0 * Math.PI * i / RADIAL_LINES;
            lineBuffer.put(0f).put(0f);
            lineBuffer.put((float) Math.cos(angle));
            lineBuffer.put((float) Math.sin(angle));
        }
        lineBuffer.flip();
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f;

        // Renk: Cizgilerin x y ekseni gibi beyaz ve belirgin olmasi istendi
        shader.setTint(gl, 1.0f, 1.0f, 1.0f, 0.4f);
        gl.glLineWidth(1f);
        // -- Ic ice halkalari ciz (Sonar gibi dalga dalga)
        shader.bindPositionOnly(gl, circleBuffer, 2);
        
        // Fatih'in kodundaki gibi 4 halka (1.0, 0.75, 0.50, 0.25 oranlarinda)
        for(float i = 1f; i > 0; i -= 0.25f) {
            float size = maxRadius * 2f * i;
            camera.modelMatrix(matrix, cx, cy, size, size);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_STEP);
        }

        // -- Radyal (Aci) cizgilerini ciz
        shader.bindPositionOnly(gl, lineBuffer, 2);
        camera.modelMatrix(matrix, cx, cy, Camera.WORLD_SIZE, Camera.WORLD_SIZE);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_LINES, 0, RADIAL_LINES * 2);
    }
    
    public FloatBuffer getCircleBuffer() {
        return circleBuffer;
    }
}
