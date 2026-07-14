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

    /**
     * Yeni bağlam nesnesi oluşturur.
     *
     * @param sweepY Sweep çizgisinin mevcut Y koordinatı.
     */
    public RenderContext(double sweepY) {
        this.sweepY = sweepY;
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
