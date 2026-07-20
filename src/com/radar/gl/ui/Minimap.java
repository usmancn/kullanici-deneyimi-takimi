package com.radar.gl.ui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import com.radar.gl.core.Camera;
import com.radar.gl.core.GainColor;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.GlBuffer;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.gl.layers.TargetLayer;


import java.util.Map;

/**
 * Sol ustte, ayri viewport'ta tum dunyayi gosteren minimap.
 * Hedefleri, tarama cizgisini ve ana gorunumun dortgenini cizer.
 * TAB ile acilip kapanir (durumu RadarCanvas tutar).
 */
public class Minimap {

    /** Minimap ekranin bu oraninda; sol ustte durur. */
    public static final float FRACTION = 0.25f;

    private static final float SCAN_THICKNESS = 6f;
    private static final float RECT_INSET     = 6f;

    private static final float BG_R = 0.01f, BG_G = 0.09f, BG_B = 0.05f;
    private static final float MAIN_BG_R = 0.02f, MAIN_BG_G = 0.15f, MAIN_BG_B = 0.08f;

    private final TargetGeometry geometry;
    private final GlBuffer rectPosition = new GlBuffer();
    private final GlBuffer rectColor    = new GlBuffer();
    private final float[] matrix = new float[16];

    public Minimap(TargetGeometry geometry) { this.geometry = geometry; }

    public void init(GL2 gl) {
        rectPosition.upload(gl, Geometry.RECT_VERTICES);
        rectColor.upload(gl, Geometry.RECT_COLORS);
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     Map<java.util.UUID, TargetLayer.Blip> blips, float scanY,
                     int surfaceWidth, int surfaceHeight) {

        int minimapWidth = surfaceWidth / 5;
        int minimapHeight = surfaceWidth / 5;
        int minimapX = 0;
        int minimapY = surfaceHeight - minimapHeight;

        gl.glViewport(minimapX, minimapY, minimapWidth, minimapHeight);

        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(minimapX, minimapY, minimapWidth, minimapHeight);
        gl.glClearColor(BG_R, BG_G, BG_B, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glClearColor(MAIN_BG_R, MAIN_BG_G, MAIN_BG_B, 1f);

        // hedefler
        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());

        for (TargetLayer.Blip blip : blips.values()) {
            // Gain filtresi: aralik disindaki gemileri minimap'te de gizle.
            if (!GainColor.passesFilter(blip.gain)) continue;

            float distance = scanY - blip.hitScanY;
            if (distance < 0) distance += Camera.WORLD_SIZE;

            float opacity = 1.0f - 0.8f * (distance / Camera.WORLD_SIZE);
            if (opacity < 0.2f) opacity = 0.2f;

            GainColor.applyGainColor(gl, shader, blip.gain, opacity);
            Camera.worldMatrix(matrix, blip.x, blip.y, blip.width, blip.height);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }
        GainColor.reset(gl, shader);

        // tarama cizgisi
        shader.bindColor(gl, geometry.greenColor.id());
        Camera.worldMatrix(matrix, Camera.WORLD_SIZE / 2f, scanY, Camera.WORLD_SIZE, SCAN_THICKNESS);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);

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

    public void dispose(GL2 gl) {
        rectPosition.dispose(gl);
        rectColor.dispose(gl);
    }
}
