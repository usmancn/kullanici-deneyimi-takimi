package com.radar;

import com.radar.config.SimulationConfig;
import com.radar.engine.EntityManager;
import com.radar.engine.SimulationEngine;
import com.radar.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.util.logging.Logger;

/**
 * Gemi Radar Simülasyonu — Uygulama Giriş Noktası.
 *
 * <p>Başlatma sırası (bağımlılık yönü korunarak):
 * <ol>
 *   <li>{@link SimulationConfig} tekil örneği alınır.</li>
 *   <li>{@link EntityManager} oluşturulur.</li>
 *   <li>{@link SimulationEngine} oluşturulur ve başlatılır.</li>
 *   <li>Swing EDT üzerinde {@link MainFrame} oluşturulur ve gösterilir.</li>
 * </ol>
 * </p>
 *
 * <p><b>JOGL yerel kütüphane yolu:</b> JVM argümanı olarak
 * {@code -Djava.library.path=lib/} belirtilmelidir.</p>
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /** Bu sınıf örneklenemez. */
    private Main() {
        throw new UnsupportedOperationException("Main orneklenilemez.");
    }

    /**
     * Uygulamanın giriş noktası.
     *
     * @param args Komut satırı argümanları (şu an kullanılmıyor).
     */
    public static void main(String[] args) {
        LOGGER.info("Gemi Radar Simulasyonu baslatiliyor...");

        // 1. Konfigürasyon
        SimulationConfig config = SimulationConfig.getInstance();

        // 2. Varlık yöneticisi
        EntityManager entityManager = new EntityManager();

        // 3. Simülasyon motoru
        SimulationEngine engine = new SimulationEngine(config, entityManager);
        engine.start();
        LOGGER.info("SimulationEngine basladi.");

        // 4. Swing penceresi EDT'de oluşturulur
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(config, entityManager, engine);
            frame.startAll();
            frame.setVisible(true);
            LOGGER.info("MainFrame gosterildi.");
        });
    }
}
