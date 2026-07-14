package com.radar.metrics;

import java.util.logging.Logger;

/**
 * GPU/Render thread kullanım metriğini gerçek zamanlı hesaplayan sağlayıcı.
 *
 * <p>JOGL Render döngüsünün (display metodu) ne kadar sürdüğünü, 
 * geçen toplam süreye oranlayarak Render iş hattının doluluk yüzdesini hesaplar.</p>
 */
public final class GpuMetricsProvider implements IMetricsProvider {

    private static final Logger LOGGER = Logger.getLogger(GpuMetricsProvider.class.getName());

    private static volatile double smoothedUsage = 0.0;

    public GpuMetricsProvider() {
        LOGGER.info("GpuMetricsProvider: Gerçek render süresi modunda başlatıldı.");
    }

    /**
     * Render döngüsünden her karede çağrılarak kullanımın güncellenmesini sağlar.
     * @param renderTimeNanos Bir karenin çiziminin sürdüğü nanosaniye
     * @param totalFrameNanos İki kare arasında geçen toplam nanosaniye
     */
    public static void reportRenderTime(long renderTimeNanos, long totalFrameNanos) {
        if (totalFrameNanos > 0) {
            double usage = ((double) renderTimeNanos / totalFrameNanos) * 100.0;
            // Ani sıçramaları yumuşat (Low-pass filter)
            smoothedUsage = (smoothedUsage * 0.9) + (usage * 0.1);
        }
    }

    @Override
    public double getCurrentUsage() {
        return Math.max(0.0, Math.min(100.0, smoothedUsage));
    }

    @Override
    public String getLabel() {
        return "GPU / Render Kullanımı (%)";
    }
}
