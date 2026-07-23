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
 * sey yok) ve soluk gri cizilir; beyaz zeminde okunur ama veriyi bastirmaz.
 *
 * <p>Etiketler kalabalik olmasin diye seyrektir: Y'de her 0.1 gain, X'te her 100
 * birim.
 */
public final class GridLayer {

    private static final int SCREEN_RESOLUTION = 1000;
    private static final float WORLD_SIZE = 1000f;

    private static final float STEP = 50f;          // 0.05 gain = 50 dunya birimi = 50 x
    private static final float LABEL_STEP = 100f;   // etiketler iki cizgide bir

    private static final int FONT_SIZE = 12;
    private static final int PADDING = 4;

    // beyaz zeminde soluk gri cizgi / biraz daha koyu etiket
    private static final float LINE_GRAY = 0.85f;
    private static final float TEXT_GRAY = 0.45f;

    private TextRenderer text;

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

        gl.glColor3f(LINE_GRAY, LINE_GRAY, LINE_GRAY);
        gl.glLineWidth(1f);
        gl.glBegin(GL.GL_LINES);
        for (float v = 0f; v <= WORLD_SIZE + 1e-3f; v += STEP) {
            gl.glVertex2f(v, 0f);           gl.glVertex2f(v, WORLD_SIZE);   // dikey: her 50 x
            gl.glVertex2f(0f, v);           gl.glVertex2f(WORLD_SIZE, v);   // yatay: her 0.05 gain
        }
        gl.glEnd();

        // ---- etiketler: ekran uzayi, sabit boyut ----
        text.beginRendering(width, height);
        text.setColor(TEXT_GRAY, TEXT_GRAY, TEXT_GRAY, 1f);

        // X ekseni: 0, 100, ... 1000 (en altta)
        for (float x = 0f; x <= WORLD_SIZE + 1e-3f; x += LABEL_STEP) {
            String label = String.valueOf(Math.round(x));
            int pixelX = Math.round(pixelX(matrix, x, 0f, width));
            int textWidth = Math.round((float) text.getBounds(label).getWidth());
            int labelX = pixelX + PADDING;
            if (labelX > width - textWidth - PADDING) labelX = width - textWidth - PADDING;
            if (labelX < PADDING) labelX = PADDING;
            text.draw(label, labelX, PADDING);
        }

        // Y ekseni: gain 0.0, 0.1, ... 1.0 (solda)
        for (float y = 0f; y <= WORLD_SIZE + 1e-3f; y += LABEL_STEP) {
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
