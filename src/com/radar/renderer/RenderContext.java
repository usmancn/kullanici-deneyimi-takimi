package com.radar.renderer;

/**
 * JOGL render döngüsü sırasında renderer'dan entity'lere aktarılan bağlam nesnesi.
 *
 * <p>Sweep tarama çizgisinin mevcut Y pozisyonunu taşır. Her {@code display()}
 * çağrısında güncel değerle oluşturulup {@link com.radar.core.IRenderable#render}
 * metoduna geçirilir. Entity'ler bu değeri kullanarak sweep-tabanlı opaklık
 * hesaplar.</p>
 */
public final class RenderContext {

    /** Sweep tarama çizgisinin mevcut dünya Y koordinatı [0, radarHeight]. */
    private final double sweepY;
    private final com.jogamp.opengl.util.gl2.GLUT glut;

    /**
     * Yeni bağlam nesnesi oluşturur.
     *
     * @param sweepY Sweep çizgisinin mevcut Y koordinatı.
     * @param glut   GLUT örneği (metin çizimi için)
     */
    public RenderContext(double sweepY, com.jogamp.opengl.util.gl2.GLUT glut) {
        this.sweepY = sweepY;
        this.glut = glut;
    }

    public com.jogamp.opengl.util.gl2.GLUT getGlut() {
        return glut;
    }

    /**
     * Sweep çizgisinin mevcut Y koordinatını döndürür.
     *
     * @return Y koordinatı (dünya birimleri, 0.0 = alt).
     */
    public double getSweepY() {
        return sweepY;
    }
}
