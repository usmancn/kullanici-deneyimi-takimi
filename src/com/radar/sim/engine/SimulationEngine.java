package com.radar.sim.engine;

import com.radar.config.SimulationConfig;
import com.radar.sim.core.ISimulationEntity;
import com.radar.sim.model.Ship;
import com.radar.sim.model.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simülasyon döngüsünü bağımsız bir thread üzerinde çalıştıran motor.
 * Varlıkların hareketini (delta-time bazlı) ve yaşam döngüsünü yönetir.
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

    /** Çakışmasız yerleştirmede aday konumlar için rastgele üreteç. */
    private final Random random = new Random();

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
     * Delta-time hesaplar, eksik gemileri tamamlar, varlıkları günceller ve siler.
     */
    private void tick() {
        try {
            long nowNanos  = System.nanoTime();
            double deltaSeconds = (nowNanos - lastTickNanos) / 1_000_000_000.0;
            lastTickNanos  = nowNanos;

            spawnMissingShips();
            updateAllEntities(deltaSeconds);
            resolveCollisions();
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
     *
     * Gemiler 1000×1000 matris içine, birbirlerinin alanına girmeyecek (AABB çakışması
     * olmayacak) ve hiçbir kısmı matris dışına taşmayacak şekilde yerleştirilir.
     * Yeterli boş alan bulunamazsa kalan gemiler bu tick eklenmez; bir sonraki tick
     * yeniden denenir.
     */
    private void spawnMissingShips() {
        int current = entityManager.getEntityCount();
        int max     = config.getMaxShipCount();
        if (current >= max) {
            return;
        }

        List<Ship> placed = collectShips();
        for (int i = current; i < max; i++) {
            Ship ship = new Ship(config);
            if (placeWithoutOverlap(ship, placed)) {
                entityManager.addEntity(ship);
                placed.add(ship);
            } else {
                break; // Boş alan kalmadı; sonraki tick tekrar denenecek.
            }
        }
    }

    /** EntityManager'daki mevcut gemileri toplar (çakışma kontrolü için). */
    private List<Ship> collectShips() {
        List<Ship> ships = new ArrayList<>();
        for (ISimulationEntity entity : entityManager.getAll()) {
            if (entity instanceof Ship) {
                ships.add((Ship) entity);
            }
        }
        return ships;
    }

    /**
     * Gemiye, sınır içinde ve mevcut gemilerle çakışmayan bir konum bulmaya çalışır.
     * Önce yapıcıdan gelen aday konum denenir, olmazsa rastgele adaylar denenir.
     *
     * @return Uygun konum bulunup atandıysa {@code true}.
     */
    private boolean placeWithoutOverlap(Ship ship, List<Ship> others) {
        if (fitsAt(ship, ship.getPosition(), others)) {
            return true;
        }
        int attempts = config.getPlacementMaxAttempts();
        for (int a = 0; a < attempts; a++) {
            Vector2D candidate = randomBoundedPosition(ship);
            if (fitsAt(ship, candidate, others)) {
                ship.setPosition(candidate);
                return true;
            }
        }
        return false;
    }

    /** Gemiyi tamamen matris içinde tutan rastgele bir merkez konumu üretir. */
    private Vector2D randomBoundedPosition(Ship ship) {
        double w = ship.getWidth();
        double h = ship.getHeight();
        double x = w / 2.0 + random.nextDouble() * (config.getRadarWidth()  - w);
        double y = h / 2.0 + random.nextDouble() * (config.getRadarHeight() - h);
        return new Vector2D(x, y);
    }

    /**
     * Geminin verilen konumda hem matris sınırları içinde kaldığını hem de hiçbir
     * mevcut gemiyle çakışmadığını kontrol eder.
     */
    private boolean fitsAt(Ship ship, Vector2D pos, List<Ship> others) {
        double halfW = ship.getWidth()  / 2.0;
        double halfH = ship.getHeight() / 2.0;

        // Sınır kontrolü: geminin hiçbir kısmı matris dışına taşmasın.
        if (pos.x - halfW < 0.0 || pos.x + halfW > config.getRadarWidth())  return false;
        if (pos.y - halfH < 0.0 || pos.y + halfH > config.getRadarHeight()) return false;

        // Çakışma kontrolü: diğer gemilerin alanına girmesin.
        for (Ship other : others) {
            if (aabbOverlap(pos, ship.getWidth(), ship.getHeight(),
                            other.getPosition(), other.getWidth(), other.getHeight())) {
                return false;
            }
        }
        return true;
    }

    /**
     * İki eksen hizalı dikdörtgenin (AABB) çakışıp çakışmadığını döndürür.
     * Konumlar dikdörtgen merkezleridir. Kenarların tam değmesi çakışma sayılmaz.
     */
    private static boolean aabbOverlap(Vector2D aPos, double aw, double ah,
                                       Vector2D bPos, double bw, double bh) {
        double dx = Math.abs(aPos.x - bPos.x);
        double dy = Math.abs(aPos.y - bPos.y);
        return dx < (aw + bw) / 2.0 && dy < (ah + bh) / 2.0;
    }

    // -------------------------------------------------------------------------
    // Çarpışma Çözümü (gemi-gemi)
    // -------------------------------------------------------------------------

    /** Hareket sonrası çakışan gemileri ayırmak için kaç kez geçileceği. */
    private static final int COLLISION_ITERATIONS = 2;

    /**
     * Hareketten sonra çakışan gemi çiftlerini ayırır ve esnek (elastik) çarpışma
     * yanıtı uygular. Gemiler birbirinin içine geçmez; çarpışınca en az girişim
     * ekseninde ayrılıp o eksendeki hız bileşenlerini takas ederler (eşit kütle).
     */
    private void resolveCollisions() {
        List<Ship> ships = collectShips();
        int n = ships.size();
        if (n < 2) {
            return;
        }
        for (int iter = 0; iter < COLLISION_ITERATIONS; iter++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    resolvePair(ships.get(i), ships.get(j));
                }
            }
        }
    }

    /** Tek bir gemi çiftinin çakışmasını (varsa) ayırır ve hızlarını yansıtır. */
    private void resolvePair(Ship a, Ship b) {
        Vector2D pa = a.getPosition();
        Vector2D pb = b.getPosition();
        double dx = pb.x - pa.x;
        double dy = pb.y - pa.y;
        double overlapX = (a.getWidth()  + b.getWidth())  / 2.0 - Math.abs(dx);
        double overlapY = (a.getHeight() + b.getHeight()) / 2.0 - Math.abs(dy);

        if (overlapX <= 0.0 || overlapY <= 0.0) {
            return; // Çakışma yok.
        }

        Vector2D va = a.getVelocity();
        Vector2D vb = b.getVelocity();

        if (overlapX < overlapY) {
            // X ekseninde ayır ve vx bileşenlerini takas et.
            double sign = (dx < 0.0) ? -1.0 : 1.0; // b, a'nın sağındaysa +
            double push = overlapX / 2.0;
            a.setPosition(clampToBounds(new Vector2D(pa.x - sign * push, pa.y), a));
            b.setPosition(clampToBounds(new Vector2D(pb.x + sign * push, pb.y), b));
            a.setVelocity(new Vector2D(vb.x, va.y));
            b.setVelocity(new Vector2D(va.x, vb.y));
        } else {
            // Y ekseninde ayır ve vy bileşenlerini takas et.
            double sign = (dy < 0.0) ? -1.0 : 1.0; // b, a'nın üstündeyse +
            double push = overlapY / 2.0;
            a.setPosition(clampToBounds(new Vector2D(pa.x, pa.y - sign * push), a));
            b.setPosition(clampToBounds(new Vector2D(pb.x, pb.y + sign * push), b));
            a.setVelocity(new Vector2D(va.x, vb.y));
            b.setVelocity(new Vector2D(vb.x, va.y));
        }
    }

    /** Bir gemi konumunu, gemi tamamen matris içinde kalacak şekilde kırpar. */
    private Vector2D clampToBounds(Vector2D pos, Ship ship) {
        double halfW = ship.getWidth()  / 2.0;
        double halfH = ship.getHeight() / 2.0;
        double x = Math.max(halfW, Math.min(config.getRadarWidth()  - halfW, pos.x));
        double y = Math.max(halfH, Math.min(config.getRadarHeight() - halfH, pos.y));
        return new Vector2D(x, y);
    }

    /**
     * EntityManager'daki tüm varlıkları verilen delta-time ile günceller.
     */
    private void updateAllEntities(double deltaSeconds) {
        for (com.radar.sim.core.ISimulationEntity entity : entityManager.getAll()) {
            entity.update(deltaSeconds);
        }
    }
}
