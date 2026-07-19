package com.radar.gl.layers;

import com.radar.gl.core.*;
import java.awt.Font;
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
        shader.disableAttribs(gl);
        gl.glUseProgram(0);

        text.beginRendering(width, height);
        // Rengi beyaz yerine acik yesil yapiyoruz (farkli ve guzel gorunmesi icin)
        text.setColor(0.3f, 0.8f, 0.4f, 0.8f);

        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f;

        // --- Aci etiketleri ---
        int radialLines = 12;
        for (int i = 0; i < radialLines; i++) {
            int degree = i * 30;
            // OpenGL acilari saatin tersi yonundedir, matematige gore hesapliyoruz
            double angle = 2.0 * Math.PI * i / radialLines;
            
            // Etiketi cemberin biraz disina koymak icin yaricapi biraz buyutuyoruz (maxRadius * 1.05)
            float worldX = cx + (maxRadius * 1.02f) * (float) Math.cos(angle);
            float worldY = cy + (maxRadius * 1.02f) * (float) Math.sin(angle);
            
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
