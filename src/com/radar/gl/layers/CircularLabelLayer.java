package com.radar.gl.layers;

import com.radar.gl.core.*;
import java.awt.Font;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Dairesel radar icin aci (0-360) ve menzil etiketleri cizer.
 */
public class CircularLabelLayer {

    private static final int FONT_SIZE = 12;
    private static final int PADDING = 6;
    private TextRenderer text;

    public void init(GL2 gl) {
        text = new TextRenderer(new Font("SansSerif", Font.BOLD, FONT_SIZE), true, true);
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera, int width, int height) {
        if (width <= 0 || height <= 0 || camera.rangeX() <= 0 || camera.rangeY() <= 0) return;

        shader.disableAttribs(gl);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0); // TextRenderer'in VBO ile cakisip cokmesini engeller
        gl.glUseProgram(0);

        text.beginRendering(width, height);
        // Kullanici yazilarin ve cizgilerin beyaz olmasini istedi
        text.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f * 0.92f;

        // --- Aci etiketleri ---
        int radialLines = 12;
        
        // Ekrana sigdirmak icin padding (dunya koordinatinda ne kadar yer kapliyor)
        float padWorldX = camera.rangeX() * (35f / width);
        float padWorldY = camera.rangeY() * (25f / height);
        
        float boundMinX = camera.minX() + padWorldX;
        float boundMaxX = camera.maxX() - padWorldX;
        float boundMinY = camera.minY() + padWorldY;
        float boundMaxY = camera.maxY() - padWorldY;

        for (int i = 0; i < radialLines; i++) {
            int degree = i * 30;
            // OpenGL acilari saatin tersi yonundedir, matematige gore hesapliyoruz
            double angle = 2.0 * Math.PI * i / radialLines;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            // Isin (ray) ile ekran sinirlarinin (bounding box) kesisimini bulalim
            float t = Float.MAX_VALUE;
            if (cos > 0.001f) t = Math.min(t, (boundMaxX - cx) / cos);
            else if (cos < -0.001f) t = Math.min(t, (boundMinX - cx) / cos);
            
            if (sin > 0.001f) t = Math.min(t, (boundMaxY - cy) / sin);
            else if (sin < -0.001f) t = Math.min(t, (boundMinY - cy) / sin);
            
            if (t < 0) t = 0f;
            
            // Istenen normal mesafe cemberin biraz disidir
            float desiredDistance = maxRadius * 1.02f;
            
            // Hangisi daha kucukse onu aliyoruz (boylece zoom yapinca etiketler ekranda kalir)
            float finalDistance = Math.min(desiredDistance, t);
            
            float worldX = cx + finalDistance * cos;
            float worldY = cy + finalDistance * sin;
            
            // Dunya koordinatini ekran koordinatina cevir
            int pixelX = Math.round((worldX - camera.minX()) / camera.rangeX() * width);
            int pixelY = Math.round((camera.maxY() - worldY) / camera.rangeY() * height); // Y ters
            
            String lbl = degree + "°";
            int textWidth = Math.round((float) text.getBounds(lbl).getWidth());
            int textHeight = Math.round((float) text.getBounds(lbl).getHeight());
            
            // Yaziyi tam ortalamak icin ufak duzeltme
            text.draw(lbl, pixelX - textWidth / 2, height - pixelY - textHeight / 2);
        }

        // --- Menzil etiketleri (4 halka icin: 250, 500, 750, 1000 gibi) ---
        // x ekseni uzerine dizecegiz (cx'ten saga dogru)
        for (float r = 0.25f; r <= 1.0f; r += 0.25f) {
            float worldX = cx + maxRadius * r;
            float worldY = cy;
            
            int pixelX = Math.round((worldX - camera.minX()) / camera.rangeX() * width);
            int pixelY = Math.round((camera.maxY() - worldY) / camera.rangeY() * height);
            
            String lbl = String.valueOf(Math.round(maxRadius * r));
            int textHeight = Math.round((float) text.getBounds(lbl).getHeight());
            
            // Yazi cizginin biraz ustunde dursun
            text.draw(lbl, pixelX + PADDING, height - pixelY + PADDING);
        }

        text.endRendering();
    }

    public void dispose(GL2 gl) {
        if (text != null) {
            text.dispose();
            text = null;
        }
    }
}
