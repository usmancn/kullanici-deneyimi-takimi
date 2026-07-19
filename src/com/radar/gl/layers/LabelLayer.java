package com.radar.gl.layers;

import com.radar.gl.core.*;


import java.awt.Font;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;


/**
 * Eksen etiketleri (X: aci, Y: menzil).
 * TextRenderer sabit-fonksiyon pipeline kullandigi icin cizim sirasinda
 * shader ve attribute dizileri gecici olarak birakilir, sonra geri alinir.
 */
public class LabelLayer {

    private static final int FONT_SIZE = 14;
    private static final int PADDING = 6;
    private static final int X_BASELINE = 6;
    private static final int Y_LEFT = 6;
    private static final int Y_MIN_BASELINE = 26;

    private TextRenderer text;

    public void init(GL2 gl) {
        text = new TextRenderer(new Font("SansSerif", Font.BOLD, FONT_SIZE), true, true);
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera, int width, int height) {
        shader.disableAttribs(gl);
        gl.glUseProgram(0);

        float minX = camera.minX(), maxX = camera.maxX();
        float minY = camera.minY(), maxY = camera.maxY();

        text.beginRendering(width, height);
        text.setColor(1f, 1f, 1f, 1f);

        // X ekseni: X koordinati etiketleri (en altta)
        for (int i = 0; i < Geometry.GRID_FRACTIONS.length; i++) {
            float fraction = Geometry.GRID_FRACTIONS[i];
            float worldX = minX + fraction * (maxX - minX);
            String xText = formatLabel(worldX);

            int pixelX = Math.round(fraction * width);
            int textWidth = Math.round((float) text.getBounds(xText).getWidth());
            int labelX = (fraction >= 1f) ? pixelX - textWidth - PADDING : pixelX + PADDING;
            text.draw(xText, labelX, X_BASELINE);
        }

        // Y ekseni: menzil etiketleri
        for (int i = 0; i < Geometry.GRID_FRACTIONS.length; i++) {
            if (i == 0) continue; // Orijin (0) X ekseninde zaten cizildi
            
            float fraction = Geometry.GRID_FRACTIONS[i];
            float worldY = minY + fraction * (maxY - minY);
            String yText = formatLabel(worldY);

            int pixelY = Math.round(fraction * height);
            int labelY = pixelY + PADDING;
            if (labelY < Y_MIN_BASELINE) labelY = Y_MIN_BASELINE;
            int maxBaseline = height - FONT_SIZE - 2;
            if (labelY > maxBaseline) labelY = maxBaseline;
            text.draw(yText, Y_LEFT, labelY);
        }

        text.endRendering();

        shader.use(gl);
        shader.enableAttribs(gl);
    }

    private static String formatLabel(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }

    public void dispose(GL2 gl) {
        if (text != null) text.dispose();
    }
}
