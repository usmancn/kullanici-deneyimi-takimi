package com.radar.renderer;

import com.jogamp.opengl.GL2;
import com.radar.model.Vector2D;

/**
 * JOGL bağlamında şekil çizim yardımcı sınıfı.
 *
 * <p>Bu sınıf tamamen <b>statik</b> metotlardan oluşur; örneklenemez.
 * Amacı: çizim mantığını ({@link com.radar.model.Ship}) varlık sınıfından
 * ayırmak ve gerekirse başka entity türleri tarafından da kullanılabilmesini
 * sağlamaktır (Single Responsibility prensibi).</p>
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
     * Belirtilen pozisyona, tepeden bakılan bir piramit görünümünde şekil çizer.
     *
     * <p>Şekil, merkez tepe noktasından 4 köşeye uzanan 4 üçgenden oluşur.
     * Sonuç görsel olarak bir "radar izi noktası" görünümüne sahiptir:
     * tepe merkezi parlak, kenarlar giderek şeffaflaşır.</p>
     *
     * <p>Anti-aliasing'in etkin olması için çağrıdan önce
     * {@code GL_BLEND} ve {@code GL_POLYGON_SMOOTH} etkinleştirilmiş olmalıdır
     * (bkz. {@link RadarRenderer#init}).</p>
     *
     * @param gl      Aktif GL2 bağlamı.
     * @param center  Şeklin merkez koordinatı (tepe nokta).
     * @param opacity Bu şeklin opaklık değeri [0.0, 1.0].
     * @param size    Şeklin yarı-boyutu (piksel). Tam boyut {@code size} dir.
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
        float cx = (float) center.x;
        float cy = (float) center.y;
        float half = size / 2.0f;

        // Dört köşe noktası
        float leftX  = cx - half;
        float rightX = cx + half;
        float bottomY = cy - half;
        float topY    = cy + half;

        // Üçgen 1: Merkez → Sol-Alt → Sağ-Alt  (ALT KENAR)
        // Üçgen 2: Merkez → Sağ-Alt → Sağ-Üst  (SAĞ KENAR)
        // Üçgen 3: Merkez → Sağ-Üst → Sol-Üst  (ÜST KENAR)
        // Üçgen 4: Merkez → Sol-Üst → Sol-Alt   (SOL KENAR)

        gl.glBegin(GL2.GL_TRIANGLES);

        // Üçgen 1
        setVertexColor(gl, r, g, b, opacity);          // merkez → parlak
        gl.glVertex2f(cx, cy);
        setVertexColor(gl, r, g, b, opacity * 0.0f);   // köşe  → şeffaf
        gl.glVertex2f(leftX, bottomY);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(rightX, bottomY);

        // Üçgen 2
        setVertexColor(gl, r, g, b, opacity);
        gl.glVertex2f(cx, cy);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(rightX, bottomY);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(rightX, topY);

        // Üçgen 3
        setVertexColor(gl, r, g, b, opacity);
        gl.glVertex2f(cx, cy);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(rightX, topY);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(leftX, topY);

        // Üçgen 4
        setVertexColor(gl, r, g, b, opacity);
        gl.glVertex2f(cx, cy);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(leftX, topY);
        setVertexColor(gl, r, g, b, opacity * 0.0f);
        gl.glVertex2f(leftX, bottomY);

        gl.glEnd();
    }

    /**
     * Bir vertex için RGBA renk değerini ayarlar.
     * Alfa kanalı {@code opacity} parametresinden gelir.
     *
     * @param gl      GL2 bağlamı.
     * @param r       Kırmızı [0.0, 1.0].
     * @param g       Yeşil [0.0, 1.0].
     * @param b       Mavi [0.0, 1.0].
     * @param alpha   Alfa / opaklık [0.0, 1.0].
     */
    private static void setVertexColor(GL2 gl, float r, float g, float b, float alpha) {
        gl.glColor4f(r, g, b, Math.max(0.0f, Math.min(1.0f, alpha)));
    }
}
