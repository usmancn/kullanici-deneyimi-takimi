package com.radar.model;

import com.jogamp.opengl.GL2;
import com.radar.config.SimulationConfig;
import com.radar.core.ISimulationEntity;
import com.radar.renderer.RenderContext;
import com.radar.renderer.ShipRenderer;

import java.util.Random;
import java.util.UUID;

/**
 * Simülasyon sahnesindeki bir gemiyi temsil eder.
 *
 * <p><b>Hareket:</b> Gemi sürekli hareket eder; simülasyon thread'i her tick'te
 * pozisyonu günceller. Alan sınırlarına çarptığında yansıma yapar.</p>
 *
 * <p><b>Klasik Radar Davranışı (Freeze-on-Sweep):</b><br>
 * Gerçek radarlarda gemiler sürekli hareket eder, ancak radar ekranı yalnızca
 * sweep çizgisinin o noktaya geçtiği andaki konumu gösterir. Bu sınıf bu davranışı
 * taklit eder:
 * <ol>
 *   <li>Sweep çizgisi geminin üzerinden geçtiği anda {@code lastSeenPosition} güncellenir
 *       (geminin o anki gerçek konumu kaydedilir).</li>
 *   <li>Sonraki render'larda gemi, {@code lastSeenPosition}'da çizilir.</li>
 *   <li>Sweep bir sonraki turda tekrar geçtiğinde konum yenilenir.</li>
 * </ol>
 * Bu yaklaşım, sweep döngüleri arasında geminin "yerinde donmuş" görünmesini sağlar;
 * sonraki sweep ile birden yeni konuma atlayan görüntü gerçek radar ekranlarına
 * özgü karakteristik görünümdür.
 * </p>
 *
 * <p><b>Minimum Opaklık:</b> Sweep uzakta olsa bile gemi,
 * {@link SimulationConfig#getMinShipOpacity()} değerinin altına düşmez.
 * Bu sayede tüm gemiler her zaman loş da olsa görünür kalır;
 * yalnızca sweep'e yakın olanlar daha parlak gösterilir.</p>
 *
 * <p><b>Thread güvenliği:</b><br>
 * {@code position} ve {@code velocity} → {@code volatile}, her iki thread'den okunabilir.<br>
 * {@code lastSeenPosition} ve {@code prevSweepY} → yalnızca JOGL render thread'inden
 * erişilir ({@code render()} içinde), senkronizasyon gerekmez.</p>
 */
public final class Ship implements ISimulationEntity {

    private final UUID id;

    /** Gerçek anlık pozisyon; simülasyon thread'i yazar, render thread'i okur. */
    private volatile Vector2D position;

    /** Hız vektörü (piksel/saniye). */
    private volatile Vector2D velocity;

    /**
     * Sweep çizgisinin en son geçtiği andaki gemi konumu.
     * Render thread'inde çizim bu pozisyona göre yapılır.
     * Yalnızca render thread'inden erişilir.
     */
    private Vector2D lastSeenPosition;

    /**
     * Önceki render çağrısındaki sweep Y değeri.
     * Sweep geçişini algılamak için kullanılır.
     * Yalnızca render thread'inden erişilir; -1.0 = henüz başlatılmadı.
     */
    private double prevSweepY = -1.0;

    private volatile boolean alive;
    private final SimulationConfig config;

    private static final Random RANDOM = new Random();

    /**
     * Verilen konfigürasyona göre rastgele pozisyon ve hızla yeni bir gemi oluşturur.
     *
     * @param config Simülasyon konfigürasyonu; null olamaz.
     */
    public Ship(SimulationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        this.config           = config;
        this.id               = UUID.randomUUID();
        this.alive            = true;
        this.position         = spawnRandomPosition();
        this.velocity         = spawnRandomVelocity();
        this.lastSeenPosition = this.position; // ilk render'da görünür başlasın
    }

    // -------------------------------------------------------------------------
    // ISimulationEntity
    // -------------------------------------------------------------------------

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    // -------------------------------------------------------------------------
    // IUpdateable
    // -------------------------------------------------------------------------

    /**
     * Gemiyi hareket ettirir. Yalnızca simülasyon thread'inden çağrılmalıdır.
     */
    @Override
    public void update(double deltaTime) {
        move(deltaTime);
    }

    // -------------------------------------------------------------------------
    // IMovable
    // -------------------------------------------------------------------------

    @Override
    public void move(double deltaTime) {
        Vector2D newPos = position.add(velocity.scale(deltaTime));

        double minX = 0.0;
        double maxX = config.getRadarWidth();
        double minY = 0.0;
        double maxY = config.getRadarHeight();

        if (newPos.x < minX || newPos.x > maxX) {
            velocity = velocity.reflectX();
            newPos   = new Vector2D(
                    Math.max(minX, Math.min(maxX, newPos.x)),
                    newPos.y
            );
        }

        if (newPos.y < minY || newPos.y > maxY) {
            velocity = velocity.reflectY();
            newPos   = new Vector2D(
                    newPos.x,
                    Math.max(minY, Math.min(maxY, newPos.y))
            );
        }

        position = newPos;
    }

    @Override
    public Vector2D getPosition() {
        return position;
    }

    @Override
    public void setPosition(Vector2D position) {
        if (position == null) {
            throw new IllegalArgumentException("Pozisyon null olamaz.");
        }
        this.position = position;
    }

    @Override
    public Vector2D getVelocity() {
        return velocity;
    }

    // -------------------------------------------------------------------------
    // IRenderable
    // -------------------------------------------------------------------------

    /**
     * Klasik radar davranışıyla gemiyi çizer.
     *
     * <p>Yalnızca JOGL render thread'inden çağrılmalıdır.</p>
     *
     * <p>İşlem sırası:
     * <ol>
     *   <li>Sweep geçişi algılanır: eğer bu frame'de sweep çizgisi geminin
     *       gerçek Y konumunun üzerinden geçtiyse {@code lastSeenPosition} güncellenir.</li>
     *   <li>Opaklık, {@code lastSeenPosition.y} ile mevcut sweep pozisyonu
     *       arasındaki farka göre hesaplanır.</li>
     *   <li>Piramit şekli {@code lastSeenPosition}'a çizilir.</li>
     * </ol>
     * </p>
     */
    @Override
    public void render(GL2 gl, RenderContext ctx) {
        double currentSweepY = ctx.getSweepY();

        // ── 1. Sweep geçişini algıla ──────────────────────────────────────────
        double actualShipY = position.y; // volatile okuma — thread-safe

        if (prevSweepY >= 0.0) {
            boolean normalCrossing = (prevSweepY < actualShipY && currentSweepY >= actualShipY);
            if (normalCrossing) {
                // Sweep bu frame'de geminin üzerinden geçti → konumu dondur
                lastSeenPosition = position; // volatile okuma
            }
        }
        prevSweepY = currentSweepY;

        // ── 2. Opaklık hesabı (lastSeenPosition.y bazlı) ─────────────────────
        float opacity = computeOpacity(lastSeenPosition.y, currentSweepY);

        // ── 3. Çizim ─────────────────────────────────────────────────────────
        ShipRenderer.drawPyramidTop(
                gl,
                lastSeenPosition,
                opacity,
                config.getShipSize(),
                config.getShipColorR(),
                config.getShipColorG(),
                config.getShipColorB()
        );
    }

    // -------------------------------------------------------------------------
    // Opaklık Hesabı (Private)
    // -------------------------------------------------------------------------

    /**
     * Sweep mesafesine göre opaklık hesaplar.
     *
     * <p>Formül (sweep geçtikten sonra):
     * <ul>
     *   <li>Hemen geçtikten sonra (distance ≈ 0) → {@code 1.0f} (tam parlak)</li>
     *   <li>Sweep {@code fadeDistance} kadar yukarıda → {@code minOpacity} (en loş)</li>
     *   <li>Sweep henüz geçmedi ya da çok uzakta → {@code minOpacity} (loş ama görünür)</li>
     * </ul>
     * Gemi asla tamamen kaybolmaz — {@code minOpacity} tabanı her zaman korunur.
     * </p>
     */
    private float computeOpacity(double lastSeenY, double sweepY) {
        float minOpacity    = config.getMinShipOpacity();
        double distance     = sweepY - lastSeenY;
        double fadeDistance = config.getSweepFadeDistance();

        // Sweep henüz geçmemiş ya da çok uzakta → minimum görünürlük
        if (distance < 0.0 || distance > fadeDistance) {
            return minOpacity;
        }

        // Yumuşak (kübik ease-out) sönüm: distance=0 → 1.0, distance=fadeDistance → minOpacity
        float t = (float) (distance / fadeDistance);
        // ease-out: 1 - t^2 (karesel, lineer'den daha yavaş solar)
        float eased = 1.0f - (t * t);
        return minOpacity + (1.0f - minOpacity) * eased;
    }

    // -------------------------------------------------------------------------
    // Yardımcı (Private)
    // -------------------------------------------------------------------------

    private Vector2D spawnRandomPosition() {
        double x = RANDOM.nextDouble() * config.getRadarWidth();
        double y = RANDOM.nextDouble() * config.getRadarHeight();
        return new Vector2D(x, y);
    }

    private Vector2D spawnRandomVelocity() {
        double angle = RANDOM.nextDouble() * 2.0 * Math.PI;
        double speed = config.getMinShipSpeed()
                + RANDOM.nextDouble() * (config.getMaxShipSpeed() - config.getMinShipSpeed());
        return new Vector2D(Math.cos(angle) * speed, Math.sin(angle) * speed);
    }
}
