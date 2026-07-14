package com.radar.metrics;

/**
 * Sistem kaynak kullanımı metrikleri için genel kontrat.
 *
 * <p>Farklı metrik kaynakları (CPU, GPU, bellek vb.) bu interface'i
 * implement ederek {@link com.radar.ui.MetricsPanel}'e geçirilebilir.
 * Panel, hangi kaynağı gösterdiğini bilmeden yalnızca bu kontrat
 * üzerinden çalışır (Dependency Inversion prensibi).</p>
 */
public interface IMetricsProvider {

    /**
     * Anlık kaynak kullanım oranını yüzde cinsinden döndürür.
     *
     * @return [0.0, 100.0] aralığında kullanım yüzdesi.
     *         Değer okunamazsa {@code -1.0} döndürülür (N/A durumu).
     */
    double getCurrentUsage();

    /**
     * Bu metrik kaynağının insan tarafından okunabilir etiketini döndürür.
     * Grafik paneli başlığında ve eksen etiketlerinde kullanılır.
     *
     * @return Etiket; null olamaz, örn. "CPU Kullanimi", "GPU Kullanimi".
     */
    String getLabel();
}
