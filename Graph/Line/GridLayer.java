package deneme.Graph.Line;

import java.awt.Font;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Cizgi grafigi icin sabit grid.
 *
 * <p>Yatay cizgiler her 0.05 gain'de, dikey cizgiler her 50 x biriminde durur.
 * Dunya olcegi gain*1000 oldugu icin iki eksende de adim 50 dunya birimidir.
 * Cizgiler dunya uzayinda sabittir (veriyle birlikte zoom/pan olur, kayan bir
 * sey yok) ve soluk gri cizilir; siyah zeminde ve altindaki waterfall katmani
 * uzerinde okunur ama veriyi bastirmaz.
 *
 * <p>Etiketler kalabalik olmasin diye seyrektir: Y'de her 0.1 gain, X'te her 100
 * birim.
 */
public final class GridLayer {

    private static final int SCREEN_RESOLUTION = 1000;
    private static final float WORLD_SIZE = 1000f;

    /** Eksen basina default bolme sayisi -> 50 dunya birimi adim. */
    public static final int DEFAULT_LINE_COUNT = 20;

    private static final int FONT_SIZE = 12;
    private static final int PADDING = 4;

    // siyah zemin + altta waterfall: cizgi soluk gri, etiket okunur olsun diye acik
    private static final java.awt.Color DEFAULT_LINE_COLOR = new java.awt.Color(102, 102, 102);
    private static final java.awt.Color DEFAULT_LABEL_COLOR = new java.awt.Color(204, 204, 204);

    // ---- GridLineBuilder ile ayarlanan ozellikler (hepsinin default'u var) ----
    private float xStep = WORLD_SIZE / DEFAULT_LINE_COUNT;   // dikey cizgi adimi
    private float yStep = WORLD_SIZE / DEFAULT_LINE_COUNT;   // yatay cizgi adimi
    private java.awt.Color lineColor = DEFAULT_LINE_COLOR;
    private java.awt.Color labelColor = DEFAULT_LABEL_COLOR;

    private TextRenderer text;

    public void setXLineCount(int count) { if (count >= 1) this.xStep = WORLD_SIZE / count; }
    public void setYLineCount(int count) { if (count >= 1) this.yStep = WORLD_SIZE / count; }
    public void setLineColor(java.awt.Color color)  { if (color != null) this.lineColor = color; }
    public void setLabelColor(java.awt.Color color) { if (color != null) this.labelColor = color; }

    public void init(GL2 gl) {
        text = new TextRenderer(new Font("SansSerif", Font.PLAIN, FONT_SIZE), true, true);
    }

    /**
     * Veri cizgilerinden ONCE cagrilir ki grid altta kalsin.
     *
     * @param matrix veriyle ayni kamera matrisi
     * @param width/height drawable piksel boyutu
     */
    public void draw(GL2 gl, float[] matrix, int width, int height) {
        if (width <= 0 || height <= 0) return;

        // ---- cizgiler: dunya uzayi, veriyle ayni matris ----
        gl.glUseProgram(0);
        // shader'dan kalan aktif attribute dizileri sabit-fonksiyon cizimini bozar
        for (int i = 0; i < 8; i++) {
            gl.glDisableVertexAttribArray(i);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadMatrixf(matrix, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glColor3f(lineColor.getRed() / 255f, lineColor.getGreen() / 255f,
                     lineColor.getBlue() / 255f);
        gl.glLineWidth(1f);
        gl.glBegin(GL.GL_LINES);
        for (float v = 0f; v <= WORLD_SIZE + 1e-3f; v += xStep) {
            gl.glVertex2f(v, 0f);           gl.glVertex2f(v, WORLD_SIZE);   // dikey
        }
        for (float v = 0f; v <= WORLD_SIZE + 1e-3f; v += yStep) {
            gl.glVertex2f(0f, v);           gl.glVertex2f(WORLD_SIZE, v);   // yatay (gain)
        }
        gl.glEnd();

        // ---- etiketler: ekran uzayi, sabit boyut (iki cizgide bir) ----
        text.beginRendering(width, height);
        text.setColor(labelColor.getRed() / 255f, labelColor.getGreen() / 255f,
                      labelColor.getBlue() / 255f, 1f);

        // X ekseni (en altta)
        for (float x = 0f; x <= WORLD_SIZE + 1e-3f; x += 2 * xStep) {
            String label = String.valueOf(Math.round(x));
            int pixelX = Math.round(pixelX(matrix, x, 0f, width));
            int textWidth = Math.round((float) text.getBounds(label).getWidth());
            int labelX = pixelX + PADDING;
            if (labelX > width - textWidth - PADDING) labelX = width - textWidth - PADDING;
            if (labelX < PADDING) labelX = PADDING;
            text.draw(label, labelX, PADDING);
        }

        // Y ekseni: gain (solda)
        for (float y = 0f; y <= WORLD_SIZE + 1e-3f; y += 2 * yStep) {
            String label = String.format("%.1f", y / SCREEN_RESOLUTION);
            int labelY = Math.round(pixelY(matrix, 0f, y, height)) + PADDING;
            int maxBaseline = height - FONT_SIZE - 2;
            if (labelY > maxBaseline) labelY = maxBaseline;
            if (labelY < PADDING) labelY = PADDING;
            text.draw(label, PADDING, labelY);
        }

        text.endRendering();
    }

    // ---- dunya -> piksel ----

    private static float pixelX(float[] matrix, float worldX, float worldY, int width) {
        float clipX = matrix[0] * worldX + matrix[4] * worldY + matrix[12];
        return (clipX * 0.5f + 0.5f) * width;
    }

    private static float pixelY(float[] matrix, float worldX, float worldY, int height) {
        float clipY = matrix[1] * worldX + matrix[5] * worldY + matrix[13];
        return (clipY * 0.5f + 0.5f) * height;
    }

    public void dispose(GL2 gl) {
        if (text != null) {
            text.dispose();
            text = null;
        }
    }
}
