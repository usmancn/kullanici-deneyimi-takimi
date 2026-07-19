package com.radar.gl.ui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import com.radar.gl.core.Camera;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.GlBuffer;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.gl.core.Matrices;
import com.radar.gl.layers.CircularTargetLayer;

import java.util.Map;
import java.util.UUID;
import java.nio.FloatBuffer;

/**
 * Sonar Ripple Radar icin Minimap.
 * Hedefleri, buyuyen dalgayi ve aktif gorunum dortgenini cizer.
 */
public class CircularMinimap {

    private static final float SQUARE_SIZE    = 10f;
    private static final float RECT_INSET     = 6f;

    private static final float BG_R = 0.01f, BG_G = 0.09f, BG_B = 0.05f;
    private static final float MAIN_BG_R = 0.02f, MAIN_BG_G = 0.15f, MAIN_BG_B = 0.08f;

    private final TargetGeometry geometry;
    private final GlBuffer rectPosition = new GlBuffer();
    private final GlBuffer rectColor    = new GlBuffer();
    private final float[] matrix = new float[16];

    public CircularMinimap(TargetGeometry geometry) { this.geometry = geometry; }

    public void init(GL2 gl) {
        rectPosition.upload(gl, Geometry.RECT_VERTICES);
        rectColor.upload(gl, Geometry.RECT_COLORS);
    }

    public void dispose(GL2 gl) {
        rectPosition.dispose(gl);
        rectColor.dispose(gl);
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     Map<UUID, CircularTargetLayer.CircularBlip> blips, float scanRadius,
                     int surfaceWidth, int surfaceHeight, FloatBuffer circleBuffer) {

        int minimapWidth = surfaceWidth / 5;
        int minimapHeight = surfaceWidth / 5;
        int minimapX = 0;
        int minimapY = surfaceHeight - minimapHeight;

        gl.glViewport(minimapX, minimapY, minimapWidth, minimapHeight);

        // Kendi ufak arka planini cizmek icin scissor test ve glClear kullaniyoruz
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(minimapX, minimapY, minimapWidth, minimapHeight);
        gl.glClearColor(0.01f, 0.09f, 0.05f, 1f); // Koyu arka plan
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        
        // Ana radar alaninin arka plan rengini geri yukle
        gl.glClearColor(0.02f, 0.15f, 0.08f, 1f);

        shader.use(gl);
        Matrices.identity(matrix);
        shader.setMatrix(gl, matrix);

        // hedefler
        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());
        
        float maxRadius = Camera.WORLD_SIZE / 2f * 0.92f;

        for (CircularTargetLayer.CircularBlip blip : blips.values()) {
            float distance = scanRadius - blip.hitRadius;
            if (distance < 0) distance += maxRadius;
            
            float opacity = 1.0f - 0.8f * (distance / maxRadius);
            if (opacity < 0.2f) opacity = 0.2f;

            shader.setTint(gl, 1f, 1f, 1f, opacity);
            Camera.worldMatrix(matrix, blip.x, blip.y, SQUARE_SIZE, SQUARE_SIZE);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }
        shader.resetTint(gl);

        // tarama dalgasi
        shader.setTint(gl, 0.3f, 1.0f, 0.4f, 0.9f);
        shader.bindPositionOnly(gl, circleBuffer, 2);
        
        Camera.worldMatrix(matrix, Camera.WORLD_SIZE / 2f, Camera.WORLD_SIZE / 2f, scanRadius * 2f, scanRadius * 2f);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, 128); // 128 is CIRCLE_STEP
        
        shader.resetTint(gl);

        // gorunur alan dortgeni
        shader.bindPosition(gl, rectPosition.id());
        shader.bindColor(gl, rectColor.id());
        float viewCenterX = camera.centerX();
        float viewCenterY = camera.centerY();
        float viewWidth   = camera.rangeX() - 2f * RECT_INSET;
        float viewHeight  = camera.rangeY() - 2f * RECT_INSET;
        if (viewWidth  < 2f) viewWidth  = 2f;
        if (viewHeight < 2f) viewHeight = 2f;
        Camera.worldMatrix(matrix, viewCenterX, viewCenterY, viewWidth, viewHeight);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, Geometry.RECT_VERTEX_COUNT);

        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
    }
}
