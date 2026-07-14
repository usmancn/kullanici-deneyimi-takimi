package com.radar.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JVM işlem CPU kullanımını ölçen metrik sağlayıcısı.
 *
 * <p>{@code com.sun.management.OperatingSystemMXBean} arayüzünün
 * {@code getProcessCpuLoad()} metodu kullanılır. Bu metot,
 * son ölçümden bu yana JVM işleminin tükettiği CPU zamanının
 * toplam kullanılabilir işlemci kapasitesine oranını döndürür.</p>
 *
 * <p>Bazı JVM uygulamalarında ilk çağrıda değer henüz hesaplanmamış
 * olabileceğinden {@code -1} döndürülebilir; bu durum {@code getCurrentUsage()}
 * tarafından N/A ({@code -1.0}) olarak raporlanır.</p>
 *
 * <p><b>Platform uyumu:</b> {@code com.sun.management} paketi
 * Oracle/OpenJDK ile Mac ve Windows'ta desteklenmektedir.</p>
 */
public final class CpuMetricsProvider implements IMetricsProvider {

    private static final Logger LOGGER = Logger.getLogger(CpuMetricsProvider.class.getName());

    /** Yüzde çarpanı: [0.0, 1.0] aralığını [0.0, 100.0]'a dönüştürür. */
    private static final double PERCENT_MULTIPLIER = 100.0;

    private final com.sun.management.OperatingSystemMXBean osMxBean;

    /**
     * CPU metrik sağlayıcısını oluşturur.
     * {@code com.sun.management.OperatingSystemMXBean} erişilemezse
     * bir uyarı loglanır ve tüm değerler N/A döndürür.
     */
    public CpuMetricsProvider() {
        com.sun.management.OperatingSystemMXBean bean = null;
        try {
            OperatingSystemMXBean rawBean = ManagementFactory.getOperatingSystemMXBean();
            if (rawBean instanceof com.sun.management.OperatingSystemMXBean) {
                bean = (com.sun.management.OperatingSystemMXBean) rawBean;
            } else {
                LOGGER.warning("com.sun.management.OperatingSystemMXBean desteklenmiyor; "
                        + "CPU metrigi N/A olarak raporlanacak.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "CPU MXBean alinamadi.", e);
        }
        this.osMxBean = bean;
    }

    /**
     * Anlık JVM işlem CPU kullanımını yüzde olarak döndürür.
     *
     * @return [0.0, 100.0] arasında kullanım yüzdesi,
     *         ya da okuma başarısızsa {@code -1.0}.
     */
    @Override
    public double getCurrentUsage() {
        if (osMxBean == null) {
            return -1.0;
        }
        double cpuLoad = osMxBean.getProcessCpuLoad();
        if (cpuLoad < 0.0) {
            return -1.0;
        }
        return cpuLoad * PERCENT_MULTIPLIER;
    }

    @Override
    public String getLabel() {
        return "CPU Kullanimi";
    }
}
