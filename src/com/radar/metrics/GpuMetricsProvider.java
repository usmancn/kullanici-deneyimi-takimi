package com.radar.metrics;

import java.util.Random;
import java.util.logging.Logger;

/**
 * GPU kullanım metriğini simüle eden sağlayıcı (yer tutucu implementasyon).
 *
 * <p>İlk versiyon için gerçek GPU verisi yerine gerçekçi görünen bir
 * sinüs dalgası + gürültü fonksiyonu kullanılır. Bu yapı,
 * {@link IMetricsProvider} kontratını tam olarak karşıladığından
 * ilerleyen versiyonlarda gerçek bir GPU okuması yapan implementasyonla
 * yer değiştirilebilir (Open/Closed prensibi).</p>
 *
 * <p>Değerler zaman içinde dalgalanır ve panel üzerinde gerçekçi
 * bir grafik oluşturur.</p>
 */
public final class GpuMetricsProvider implements IMetricsProvider {

    private static final Logger LOGGER = Logger.getLogger(GpuMetricsProvider.class.getName());

    /** Dalga genliği (yüzde cinsinden). */
    private static final double WAVE_AMPLITUDE = 30.0;

    /** Dalga merkezi (yüzde cinsinden). */
    private static final double WAVE_CENTER = 45.0;

    /** Gürültü genliği (yüzde cinsinden). */
    private static final double NOISE_AMPLITUDE = 8.0;

    /** Sinüs dalgasının faz hızı (radyan/saniye). */
    private static final double PHASE_SPEED = 0.5;

    private final Random random;

    /** Uygulama başlangıcına göre geçen saniye sayısı (faz hesabı için). */
    private final long startTimeMs;

    public GpuMetricsProvider() {
        this.random      = new Random();
        this.startTimeMs = System.currentTimeMillis();
        LOGGER.info("GpuMetricsProvider: Simüle edilmis deger modunda baslatildi.");
    }

    /**
     * Gerçekçi görünen simüle edilmiş bir GPU kullanım değeri döndürür.
     * Sinüs dalgası + rassal gürültü kombinasyonundan oluşur.
     *
     * @return [0.0, 100.0] arasında simüle edilmiş GPU kullanım yüzdesi.
     */
    @Override
    public double getCurrentUsage() {
        double elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000.0;
        double wave           = Math.sin(elapsedSeconds * PHASE_SPEED) * WAVE_AMPLITUDE;
        double noise          = (random.nextDouble() - 0.5) * 2.0 * NOISE_AMPLITUDE;
        double value          = WAVE_CENTER + wave + noise;
        // [0.0, 100.0] sınırlarına sabitle
        return Math.max(0.0, Math.min(100.0, value));
    }

    @Override
    public String getLabel() {
        return "GPU Kullanimi (Simulasyon)";
    }
}
