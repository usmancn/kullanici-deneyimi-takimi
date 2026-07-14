package com.radar.renderer;

import com.jogamp.opengl.GL2;
import com.radar.model.Vector2D;

/**
 * JOGL bağlamında gemi simgesi çizim yardımcı sınıfı.
 *
 * <p>Gemi görsel temsili olarak <b>tepeden bakış açısıyla piramit</b> kullanılır.
 * Piramit merkezden (tepe nokta) 4 köşeye uzanan 4 üçgenden oluşur:
 * <ul>
 *   <li>Merkez vertex → tam parlak (opacity = parametre değeri)</li>
 *   <li>Köşe vertexler → tamamen şeffaf (opacity = 0.0)</li>
 * </ul>
 * Bu yapı klasik radar ekranlarındaki fosfor parlamasını taklit eder;
 * sweep geçtikten sonra tüm piramit birlikte sönüklenir.</p>
 *
 * <p>Anti-aliasing için {@link com.jogamp.opengl.GL2#GL_POLYGON_SMOOTH} ve
 * {@code GL_BLEND} etkin olmalıdır (bkz. {@link RadarRenderer#init}).</p>
 *
 * <p><b>Thread güvenliği:</b> Tüm metotlar yalnızca JOGL render thread'inden
 * (GLEventListener#display içinden) çağrılmalıdır.</p>
 */
public final class ShipRenderer {

    /** Bu sınıf örneklenemez. */
    private ShipRenderer() {
        throw new UnsupportedOperationException("ShipRenderer orneklenilemez.");
    }

    /**
     * Belirtilen merkez noktasına, tepeden bakılan piramit şeklini çizer.
     *
     * <p>4 üçgenden oluşur (alt, sağ, üst, sol):
     * <pre>
     *        Sol-Üst ------- Sağ-Üst
     *           |  \       /  |
     *           |    Merkez   |
     *           |  /       \  |
     *        Sol-Alt ------- Sağ-Alt
     * </pre>
     * Her üçgenin merkez köşesi {@code opacity} değerinde, kenar köşeleri
     * sıfır opaklıkta çizilir.</p>
     *
     * @param gl      Aktif GL2 bağlamı; null olamaz.
     * @param center  Piramidin merkez (tepe) koordinatı.
     * @param opacity Merkez noktanın opaklığı [0.0, 1.0]; sweep tarafından hesaplanır.
     * @param size    Piramidin kenar uzunluğu (piksel). Her iki yönde bu değer kullanılır.
     * @param r       Kırmızı kanal [0.0, 1.0].
     * @param g       Yeşil kanal [0.0, 1.0].
     * @param b       Mavi kanal [0.0, 1.0].
     */
    public static void drawPyramidTop(GL2 gl,
                                      Vector2D center,
                                      float opacity,
                                      int size,
                                      float r,
                                      float g,
                                      float b) {
        if (opacity <= 0.01f) {
            return; // Görünmez — OpenGL çağrısı yapma
        }

        float cx   = (float) center.x;
        float cy   = (float) center.y;
        float half = size / 2.0f;

        // Dört köşe koordinatları
        float leftX   = cx - half;
        float rightX  = cx + half;
        float bottomY = cy - half;
        float topY    = cy + half;

        float clampedOpacity = clampAlpha(opacity);

        gl.glBegin(GL2.GL_TRIANGLES);

        // ── Üçgen 1: ALT ──────────────────────────────────
        // Merkez (parlak)
        gl.glColor4f(r, g, b, clampedOpacity);
        gl.glVertex2f(cx, cy);
        // Sol-alt köşe (şeffaf)
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(leftX, bottomY);
        // Sağ-alt köşe (şeffaf)
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(rightX, bottomY);

        // ── Üçgen 2: SAĞ ──────────────────────────────────
        gl.glColor4f(r, g, b, clampedOpacity);
        gl.glVertex2f(cx, cy);
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(rightX, bottomY);
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(rightX, topY);

        // ── Üçgen 3: ÜST ──────────────────────────────────
        gl.glColor4f(r, g, b, clampedOpacity);
        gl.glVertex2f(cx, cy);
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(rightX, topY);
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(leftX, topY);

        // ── Üçgen 4: SOL ──────────────────────────────────
        gl.glColor4f(r, g, b, clampedOpacity);
        gl.glVertex2f(cx, cy);
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(leftX, topY);
        gl.glColor4f(r, g, b, 0.0f);
        gl.glVertex2f(leftX, bottomY);

        gl.glEnd();
    }

    /**
     * İşaretli (marked) gemilerin etrafına dikkat çekici bir halka/kutu çizer ve ismini yazar.
     */
    public static void drawMark(GL2 gl, Vector2D center, String name, com.jogamp.opengl.util.gl2.GLUT glut) {
        float cx = (float) center.x;
        float cy = (float) center.y;
        float size = 20.0f; // İşaret boyutu
        
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glColor4f(0.2f, 0.8f, 1.0f, 0.9f); // Açık mavi (Cyan) renk
        gl.glVertex2f(cx - size, cy - size);
        gl.glVertex2f(cx + size, cy - size);
        gl.glVertex2f(cx + size, cy + size);
        gl.glVertex2f(cx - size, cy + size);
        gl.glEnd();
        gl.glLineWidth(1.0f);

        if (name != null && glut != null) {
            gl.glRasterPos2f(cx + size + 5.0f, cy + size + 5.0f);
            glut.glutBitmapString(com.jogamp.opengl.util.gl2.GLUT.BITMAP_HELVETICA_12, name);
        }
    }

    /**
     * Alfa değerini [0.0, 1.0] aralığıyla sınırlandırır.
     */
    private static float clampAlpha(float alpha) {
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }
}
