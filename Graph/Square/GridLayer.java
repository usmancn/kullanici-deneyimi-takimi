package deneme.Graph.Square;

import java.awt.Font;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

import deneme.GLCore.Camera;

/**
 * Kare grafik icin grid: cizgiler + eksen etiketleri tek sinifta.
 *
 * <p>Cizgiler ekranin sabit oranlarinda (0, 1/4, 1/2, 3/4, 1) durur; dunya
 * konumlari kameranin o anki araligindan hesaplandigi icin zoom/pan yapilinca
 * cizgiler ekranda yerinde kalir, ustlerindeki etiketler degisir.
 *
 * <p>Cizim {@code Mark} ile ayni sabit-fonksiyon (immediate mode) yolunu
 * kullanir: shader birakilir, vertex-attribute dizileri kapatilir, kamera
 * matrisi PROJECTION olarak yuklenir.
 */
public final class GridLayer {

    /** Eksen basina default cizgi sayisi (kenarlar dahil) -> 0, 1/4, 1/2, 3/4, 1. */
    public static final int DEFAULT_LINE_COUNT = 5;

    private static final int FONT_SIZE = 14;
    private static final int PADDING = 6;
    private static final int X_BASELINE = 6;
    private static final int Y_LEFT = 6;
    private static final int Y_MIN_BASELINE = 26;

    // soluk gri: veriyi bastirmaz ama okunur
    private static final java.awt.Color DEFAULT_LINE_COLOR = new java.awt.Color(115, 115, 115);

    // ---- GridSquareBuilder ile ayarlanan ozellikler (hepsinin default'u var) ----
    private float[] xFractions = fractions(DEFAULT_LINE_COUNT);   // dikey cizgiler
    private float[] yFractions = fractions(DEFAULT_LINE_COUNT);   // yatay cizgiler
    private java.awt.Color lineColor = DEFAULT_LINE_COLOR;

    private TextRenderer text;

    public void setXLineCount(int count) { if (count >= 2) this.xFractions = fractions(count); }
    public void setYLineCount(int count) { if (count >= 2) this.yFractions = fractions(count); }
    public void setLineColor(java.awt.Color color) { if (color != null) this.lineColor = color; }

    /** count cizgiyi (kenarlar dahil) 0..1 araligina esit dagitir. */
    private static float[] fractions(int count) {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = i / (float) (count - 1);
        }
        return result;
    }

    public void init(GL2 gl) {
        text = new TextRenderer(new Font("SansSerif", Font.BOLD, FONT_SIZE), true, true);
    }

    /**
     * @param matrix veriyle ayni kamera matrisi (aspect duzeltmesi uygulanmis)
     * @param width/height drawable piksel boyutu
     */
    public void draw(GL2 gl, float[] matrix, Camera camera, int width, int height) {
        if (width <= 0 || height <= 0) return;

        float minX = camera.minX(), maxX = camera.maxX();
        float minY = camera.minY(), maxY = camera.maxY();

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
        for (float fraction : xFractions) {
            float worldX = minX + fraction * (maxX - minX);
            gl.glVertex2f(worldX, minY);   gl.glVertex2f(worldX, maxY);   // dikey
        }
        for (float fraction : yFractions) {
            float worldY = minY + fraction * (maxY - minY);
            gl.glVertex2f(minX, worldY);   gl.glVertex2f(maxX, worldY);   // yatay
        }
        gl.glEnd();

        // ---- etiketler: ekran uzayi, sabit boyut ----
        text.beginRendering(width, height);
        text.setColor(1f, 1f, 1f, 1f);

        // X ekseni: X koordinati etiketleri (en altta)
        for (int i = 0; i < xFractions.length; i++) {
            float fraction = xFractions[i];
            float worldX = minX + fraction * (maxX - minX);
            String xText = formatLabel(worldX);

            int pixelX = pixelX(matrix, worldX, minY, width);
            int textWidth = Math.round((float) text.getBounds(xText).getWidth());
            int labelX = (fraction >= 1f) ? pixelX - textWidth - PADDING : pixelX + PADDING;
            if (labelX < PADDING) labelX = PADDING;
            if (labelX > width - textWidth - PADDING) labelX = width - textWidth - PADDING;
            text.draw(xText, labelX, X_BASELINE);
        }

        // Y ekseni: menzil etiketleri
        for (int i = 0; i < yFractions.length; i++) {
            if (i == 0) continue;   // orijin (0) X ekseninde zaten cizildi

            float fraction = yFractions[i];
            float worldY = minY + fraction * (maxY - minY);
            String yText = formatLabel(worldY);

            int labelY = pixelY(matrix, minX, worldY, height) + PADDING;
            if (labelY < Y_MIN_BASELINE) labelY = Y_MIN_BASELINE;
            int maxBaseline = height - FONT_SIZE - 2;
            if (labelY > maxBaseline) labelY = maxBaseline;
            text.draw(yText, Y_LEFT, labelY);
        }

        text.endRendering();
    }

    // ---- dunya -> piksel (matris aspect duzeltmesini de icerir) ----

    private static int pixelX(float[] matrix, float worldX, float worldY, int width) {
        float clipX = matrix[0] * worldX + matrix[4] * worldY + matrix[12];
        return Math.round((clipX * 0.5f + 0.5f) * width);
    }

    private static int pixelY(float[] matrix, float worldX, float worldY, int height) {
        float clipY = matrix[1] * worldX + matrix[5] * worldY + matrix[13];
        return Math.round((clipY * 0.5f + 0.5f) * height);
    }

    private static String formatLabel(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }

    public void dispose(GL2 gl) {
        if (text != null) {
            text.dispose();
            text = null;
        }
    }
}
