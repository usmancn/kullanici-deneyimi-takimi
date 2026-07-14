package com.radar.renderer;

import com.jogamp.opengl.GL2;
import com.radar.model.Vector2D;

/**
 * JOGL bağlamında gemi simgesi çizim yardımcı sınıfı.
 *
 * <p>Bu sınıf tamamen <b>statik</b> metotlardan oluşur; örneklenemez.
 * Gemi görsel temsili olarak küçük dolu bir kare ve etrafında hafif
 * bir ışıma (glow) efekti kullanılır — bu, gerçek radar ekranlarındaki
 * fosfor parlaması etkisini taklit eder.</p>
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
     * Belirtilen pozisyona sweep-tabanlı opaklıkla dolu kare çizer.
     *
     * <p>İki katmanlı çizim yapılır:
     * <ol>
     *   <li><b>Glow katmanı</b>: Karenin 2.5 katı büyüklükte yarı saydam hale,
     *       fosfor parlaması etkisi verir.</li>
     *   <li><b>Çekirdek kare</b>: Tam dolu, keskin kenarlı kare.</li>
     * </ol>
     * </p>
     *
     * @param gl      Aktif GL2 bağlamı.
     * @param center  Karenin merkez koordinatı.
     * @param opacity Bu karenin opaklık değeri [0.0, 1.0].
     * @param size    Karenin piksel cinsinden kenar uzunluğu.
     * @param r       Kırmızı kanal [0.0, 1.0].
     * @param g       Yeşil kanal [0.0, 1.0].
     * @param b       Mavi kanal [0.0, 1.0].
     */
    public static void drawSquare(GL2 gl,
                                  Vector2D center,
                                  float opacity,
                                  int size,
                                  float r,
                                  float g,
                                  float b) {
        if (opacity <= 0.01f) {
            return; // Görünmez nokta çizme
        }

        float cx   = (float) center.x;
        float cy   = (float) center.y;
        float half = size / 2.0f;

        // --- Glow katmanı (2.5× büyüklükte, düşük alfa) ---
        float glowHalf = half * 2.5f;
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(r, g, b, opacity * 0.20f);
        gl.glVertex2f(cx - glowHalf, cy - glowHalf);
        gl.glVertex2f(cx + glowHalf, cy - glowHalf);
        gl.glVertex2f(cx + glowHalf, cy + glowHalf);
        gl.glVertex2f(cx - glowHalf, cy + glowHalf);
        gl.glEnd();

        // --- Çekirdek kare (tam opak, küçük) ---
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(r, g, b, clampAlpha(opacity));
        gl.glVertex2f(cx - half, cy - half);
        gl.glVertex2f(cx + half, cy - half);
        gl.glVertex2f(cx + half, cy + half);
        gl.glVertex2f(cx - half, cy + half);
        gl.glEnd();
    }

    /**
     * Alfa değerini [0.0, 1.0] aralığıyla sınırlandırır.
     */
    private static float clampAlpha(float alpha) {
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }
}
