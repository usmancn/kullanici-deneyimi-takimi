package deneme.Graph.Circular;

import java.awt.Font;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Kutupsal (PPI) grafik icin grid: ic ice halkalar + aci cizgileri + etiketler,
 * hepsi tek sinifta.
 *
 * <p>Halkalar menzilin 1/4, 2/4, 3/4 ve tamamini; radyal cizgiler 30 derecelik
 * araliklari gosterir. Aci etiketleri dis cemberin biraz disinda durur, zoom
 * yapilinca ekran kenarina yapisip gorunur kalir.
 *
 * <p>Cizim {@code Mark} ile ayni sabit-fonksiyon (immediate mode) yolunu kullanir.
 */
public final class GridLayer {

    private static final int SCREEN_RESOLUTION = 1000;
    private static final float CENTER = SCREEN_RESOLUTION / 2f;        // 500
    private static final float MAX_RADIUS = SCREEN_RESOLUTION * 0.5f;  // 500 -> ic teget cember

    private static final int RING_SEGMENTS = 128;
    private static final int RADIAL_LINES = 12;                        // 30 derecede bir
    private static final float RING_STEP = 0.25f;                      // 4 halka

    private static final int FONT_SIZE = 12;
    private static final int PADDING = 6;
    private static final int EDGE_PADDING = 18;                        // etiketin ekran kenarina mesafesi

    // soluk gri-beyaz: gain gorseli uzerinde okunur ama bastirmaz
    private static final float LINE_GRAY = 0.65f;

    private TextRenderer text;

    public void init(GL2 gl) {
        text = new TextRenderer(new Font("SansSerif", Font.BOLD, FONT_SIZE), true, true);
    }

    /**
     * @param matrix veriyle ayni kamera matrisi (aspect duzeltmesi uygulanmis)
     * @param width/height drawable piksel boyutu
     */
    public void draw(GL2 gl, float[] matrix, int width, int height) {
        if (width <= 0 || height <= 0) return;

        // ---- halkalar + radyal cizgiler: dunya uzayi, veriyle ayni matris ----
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

        // ic ice halkalar (sonar gibi dalga dalga)
        for (float r = RING_STEP; r <= 1.0f + 1e-4f; r += RING_STEP) {
            float radius = MAX_RADIUS * r;
            gl.glBegin(GL.GL_LINE_LOOP);
            for (int k = 0; k < RING_SEGMENTS; k++) {
                double theta = 2.0 * Math.PI * k / RING_SEGMENTS;
                gl.glVertex2f(CENTER + radius * (float) Math.cos(theta),
                              CENTER + radius * (float) Math.sin(theta));
            }
            gl.glEnd();
        }

        // radyal (aci) cizgileri: merkezden dis cembere
        gl.glBegin(GL.GL_LINES);
        for (int i = 0; i < RADIAL_LINES; i++) {
            double bearing = bearing(i);
            gl.glVertex2f(CENTER, CENTER);
            gl.glVertex2f(CENTER + MAX_RADIUS * (float) Math.sin(bearing),
                          CENTER + MAX_RADIUS * (float) Math.cos(bearing));
        }
        gl.glEnd();

        // ---- etiketler: ekran uzayi, sabit boyut ----
        text.beginRendering(width, height);
        text.setColor(1f, 1f, 1f, 1f);

        float centerPixelX = pixelX(matrix, CENTER, CENTER, width);
        float centerPixelY = pixelY(matrix, CENTER, CENTER, height);

        // --- aci etiketleri (0..330, 30 derecede bir; 0 kuzeyde, saat yonunde artar) ---
        for (int i = 0; i < RADIAL_LINES; i++) {
            double bearing = bearing(i);
            // istenen konum: dis cemberin biraz disi
            float desired = MAX_RADIUS * 1.03f;
            float worldX = CENTER + desired * (float) Math.sin(bearing);
            float worldY = CENTER + desired * (float) Math.cos(bearing);

            float targetPixelX = pixelX(matrix, worldX, worldY, width);
            float targetPixelY = pixelY(matrix, worldX, worldY, height);

            float dirX = targetPixelX - centerPixelX;
            float dirY = targetPixelY - centerPixelY;
            float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            if (length < 1e-4f) continue;
            dirX /= length; dirY /= length;

            // zoom'da etiket ekran disina cikmasin: isini padding'li dikdortgene kirp
            float limit = rayLimit(centerPixelX, centerPixelY, dirX, dirY, width, height);
            float finalLength = Math.min(length, limit);
            if (finalLength < 0f) finalLength = 0f;

            int pixelX = Math.round(centerPixelX + dirX * finalLength);
            int pixelY = Math.round(centerPixelY + dirY * finalLength);

            String label = (i * (360 / RADIAL_LINES)) + "°";
            int textWidth  = Math.round((float) text.getBounds(label).getWidth());
            int textHeight = Math.round((float) text.getBounds(label).getHeight());
            text.draw(label, pixelX - textWidth / 2, pixelY - textHeight / 2);
        }

        // --- menzil etiketleri (4 halka: 250, 500, 750, 1000) ---
        // halkanin fiziksel yeri MAX_RADIUS*r, ustune yazilan menzil r * SCREEN_RESOLUTION;
        // boylece kare grafikle ayni 0..1000 olcegi gorunur
        for (float r = RING_STEP; r <= 1.0f + 1e-4f; r += RING_STEP) {
            float worldX = CENTER + MAX_RADIUS * r;
            float worldY = CENTER;

            int pixelX = Math.round(pixelX(matrix, worldX, worldY, width));
            int pixelY = Math.round(pixelY(matrix, worldX, worldY, height));

            String label = String.valueOf(Math.round(SCREEN_RESOLUTION * r));
            text.draw(label, pixelX + PADDING, pixelY + PADDING);
        }

        text.endRendering();
    }

    /**
     * i. radyal cizginin aci degeri (radyan): kuzeyden (yukari) saat yonunde olculur.
     * Shader'daki {@code a = atan(c.x, c.y) / 2pi} eslesmesinin tersidir; dunya yonu
     * {@code (sin(bearing), cos(bearing))} ile bulunur.
     */
    private static double bearing(int index) {
        return 2.0 * Math.PI * index / RADIAL_LINES;
    }

    /**
     * Merkezden (dirX,dirY) yonunde giden isinin, padding'li ekran dikdortgenini
     * terk ettigi mesafe (piksel).
     */
    private static float rayLimit(float originX, float originY, float dirX, float dirY,
                                  int width, int height) {
        float minX = EDGE_PADDING, maxX = width - EDGE_PADDING;
        float minY = EDGE_PADDING, maxY = height - EDGE_PADDING;

        float limit = Float.MAX_VALUE;
        if (dirX > 1e-4f)       limit = Math.min(limit, (maxX - originX) / dirX);
        else if (dirX < -1e-4f) limit = Math.min(limit, (minX - originX) / dirX);
        if (dirY > 1e-4f)       limit = Math.min(limit, (maxY - originY) / dirY);
        else if (dirY < -1e-4f) limit = Math.min(limit, (minY - originY) / dirY);
        return limit;
    }

    // ---- dunya -> piksel (matris aspect duzeltmesini de icerir) ----

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
