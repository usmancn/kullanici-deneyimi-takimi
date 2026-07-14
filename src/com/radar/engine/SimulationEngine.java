package com.radar.engine;

import com.radar.config.SimulationConfig;
import com.radar.model.Ship;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simülasyonun mantık döngüsünü belirli bir frekans (Hz) ile çalıştıran motor.
 *
 * <p><b>Sorumluluklar:</b>
 * <ul>
 *   <li>Her tick'te {@link EntityManager} üzerindeki tüm varlıkları günceller.</li>
 *   <li>Sahne varlık sayısı {@link SimulationConfig#getMaxShipCount()} değerinin
 *       altına düştüğünde otomatik olarak yeni {@link Ship} üretir.</li>
 *   <li>Ölü varlıkları listeden temizler.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread modeli:</b> Motor kendi {@link ScheduledExecutorService}'ini (tek thread)
 * yönetir. JOGL render döngüsü ile tamamen bağımsız çalışır; ikisi arasındaki
 * paylaşım {@link EntityManager} üzerinden thread-safe biçimde gerçekleşir.</p>
 *
 * <p><b>Delta-time hesabı:</b> Planlanan periyot ile gerçek geçen süre arasındaki
 * fark {@code System.nanoTime()} kullanılarak ölçülür ve {@code update(deltaTime)}
 * çağrılarına aktarılır. Bu sayede hareket hesapları kare hızından bağımsız kalır.</p>
 */
public final class SimulationEngine {

    private static final Logger LOGGER = Logger.getLogger(SimulationEngine.class.getName());

    private final SimulationConfig config;
    private final EntityManager    entityManager;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>       tickFuture;

    /** Motor çalışıyor mu? */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Son tick'in nanosaniye cinsinden zaman damgası. */
    private long lastTickNanos;

    /**
     * Yeni bir simülasyon motoru oluşturur.
     *
     * @param config        Konfigürasyon nesnesi; null olamaz.
     * @param entityManager Varlık yöneticisi; null olamaz.
     */
    public SimulationEngine(SimulationConfig config, EntityManager entityManager) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        if (entityManager == null) {
            throw new IllegalArgumentException("EntityManager null olamaz.");
        }
        this.config        = config;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------------------------
    // Motor Yaşam Döngüsü
    // -------------------------------------------------------------------------

    /**
     * Simülasyon motorunu başlatır.
     * Motor zaten çalışıyorsa bu çağrı yoksayılır.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler      = Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "SimulationEngine-Thread");
                        t.setDaemon(true);
                        return t;
                    }
            );
            lastTickNanos  = System.nanoTime();
            scheduleNextTick();
            LOGGER.info("SimulationEngine baslatildi (" + config.getSimulationHz() + " Hz).");
        }
    }

    /**
     * Simülasyon motorunu durdurur ve tüm kaynakları serbest bırakır.
     * Motor zaten durmuşsa bu çağrı yoksayılır.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (tickFuture != null) {
                tickFuture.cancel(false);
            }
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2L, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
            LOGGER.info("SimulationEngine durduruldu.");
        }
    }

    /**
     * Motorun çalışıp çalışmadığını döndürür.
     *
     * @return {@code true} motor aktif ise.
     */
    public boolean isRunning() {
        return running.get();
    }

    // -------------------------------------------------------------------------
    // Tick Planlaması
    // -------------------------------------------------------------------------

    /**
     * Bir sonraki tick'i mevcut Hz değerine göre planlar.
     * Hz değeri çalışma zamanında değiştirilirse bir sonraki tick'ten
     * itibaren yeni periyot kullanılır.
     */
    private void scheduleNextTick() {
        if (!running.get()) {
            return;
        }
        long periodMicros = 1_000_000L / config.getSimulationHz();
        tickFuture = scheduler.schedule(this::tick, periodMicros, TimeUnit.MICROSECONDS);
    }

    /**
     * Tek bir simülasyon adımını gerçekleştirir:
     * <ol>
     *   <li>Delta-time hesaplar.</li>
     *   <li>Eksik gemileri tamamlar (spawn).</li>
     *   <li>Tüm varlıkları günceller.</li>
     *   <li>Ölü varlıkları temizler.</li>
     *   <li>Bir sonraki tick'i planlar.</li>
     * </ol>
     */
    private void tick() {
        try {
            long nowNanos  = System.nanoTime();
            double deltaSeconds = (nowNanos - lastTickNanos) / 1_000_000_000.0;
            lastTickNanos  = nowNanos;

            spawnMissingShips();
            updateAllEntities(deltaSeconds);
            entityManager.removeDeadEntities();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Tick sirasinda beklenmeyen hata: ", e);
        } finally {
            scheduleNextTick();
        }
    }

    // -------------------------------------------------------------------------
    // Yardımcı (Private)
    // -------------------------------------------------------------------------

    /**
     * Sahne varlık sayısı maksimumun altındaysa yeni gemiler oluşturur.
     */
    private void spawnMissingShips() {
        int current = entityManager.getEntityCount();
        int max     = config.getMaxShipCount();

        for (int i = current; i < max; i++) {
            entityManager.addEntity(new Ship(config));
        }
    }

    /**
     * EntityManager'daki tüm varlıkları verilen delta-time ile günceller.
     */
    private void updateAllEntities(double deltaSeconds) {
        for (com.radar.core.ISimulationEntity entity : entityManager.getAll()) {
            entity.update(deltaSeconds);
        }
    }
}
