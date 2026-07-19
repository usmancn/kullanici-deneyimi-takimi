package com.radar.app;

import com.radar.config.SimulationConfig;
import com.radar.sim.engine.EntityManager;
import com.radar.sim.engine.SimulationEngine;
import com.radar.factory.GraphFactory.GraphType;
import com.radar.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Kütüphanenin (Library) dış dünyaya açılan tek yüzü — Facade Pattern.
 *
 * <p>Builder zinciriyle kullanılır:</p>
 * <pre>{@code
 * RadarLibrary.create()
 *     .withShipCount(50)
 *     .withFrequency(60)
 *     .withGraph(GraphType.RADAR)
 *     .start();
 * }</pre>
 *
 * <p>Tüm bağımlılıklar ({@link SimulationEngine}, {@link EntityManager},
 * {@link MainFrame}) içeride otomatik kurulur; dışarıdan erişim gerekmez.</p>
 */
public final class RadarLibrary {

    private int       shipCount  = 50;
    private int       frequency  = 60;
    private double    radarSpeed = 80.0;   // sweep px/s (SimulationConfig.sweepSpeedPps)
    private GraphType graphType  = GraphType.RADAR;

    private RadarLibrary() { /* Builder pattern — private constructor */ }

    /** Yeni bir RadarLibrary builder'i başlatır. */
    public static RadarLibrary create() {
        return new RadarLibrary();
    }

    /**
     * Sahnedeki maksimum gemi sayısını ayarlar.
     * @param count Pozitif tam sayı.
     */
    public RadarLibrary withShipCount(int count) {
        this.shipCount = count;
        return this;
    }

    /**
     * Radar render FPS hedefini ayarlar.
     * @param fps Saniyedeki kare sayısı (örn. 60).
     */
    public RadarLibrary withFrequency(int fps) {
        this.frequency = fps;
        return this;
    }

    /**
     * Radar tarama çizgisinin hızını ayarlar (piksel/saniye).
     * @param speedPps Pozitif değer.
     */
    public RadarLibrary withRadarSpeed(double speedPps) {
        this.radarSpeed = speedPps;
        return this;
    }

    /**
     * Başlangıçta gösterilecek sekmeyi (grafik türünü) seçer.
     * @param type {@link GraphType} sabitlerinden biri.
     */
    public RadarLibrary withGraph(GraphType type) {
        this.graphType = type;
        return this;
    }

    /**
     * Tüm sistemi (motor + UI) kurar ve pencereyi açar.
     * EDT üzerinde çalışır; çağrı bloklanmaz.
     */
    public void start() {
        final int       _ships     = shipCount;
        final int       _freq      = frequency;
        final double    _speed     = radarSpeed;
        final GraphType _graphType = graphType;

        SwingUtilities.invokeLater(() -> {
            // 1. Konfigürasyon
            SimulationConfig config = SimulationConfig.getInstance();
            config.setMaxShipCount(_ships);
            config.setSweepSpeedPps((float) _speed);

            // 2. Motorlar
            EntityManager   entityManager = new EntityManager();
            SimulationEngine engine        = new SimulationEngine(config, entityManager);

            // 3. Kullanıcı Arayüzü
            MainFrame frame = new MainFrame(config, entityManager, engine, _freq, _graphType);
            frame.setVisible(true);

            // 4. Ateşle
            engine.start();
            frame.startAll();
        });
    }
}
