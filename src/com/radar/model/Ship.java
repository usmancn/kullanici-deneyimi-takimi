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
 * <p><b>Hareket:</b> Sabit bir hız vektörüyle düz çizgi hareket eder.
 * Alan sınırlarına çarptığında ilgili eksen yönünde yansıma (reflection) yapar.</p>
 *
 * <p><b>Görünürlük (Sweep-tabanlı Fading):</b> Gemi, radar sweep çizgisi
 * üzerinden geçtiği anda tam parlak görünür. Sweep çizgisi uzaklaştıkça
 * (y ekseni boyunca yukarı çıktıkça) gemi, {@link SimulationConfig#getSweepFadeDistance()}
 * mesafesinde sıfır opaklığa düşer. Bu, gerçek radar ekranlarındaki fosfor
 * sönüklesmesi (phosphor decay) etkisini taklit eder.</p>
 *
 * <p><b>Thread güvenliği:</b> {@code position} ve {@code velocity}
 * {@code volatile} olduğundan render thread'i güvenle okuyabilir.
 * Yazma yalnızca simülasyon thread'inden yapılır.</p>
 */
public final class Ship implements ISimulationEntity {

    private final UUID id;

    /** Mevcut pozisyon; simülasyon thread'i yazar, render thread'i okur. */
    private volatile Vector2D position;

    /** Hız vektörü (piksel/saniye). */
    private volatile Vector2D velocity;

    /** Geminin sahnede aktif olup olmadığı. */
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
        this.config   = config;
        this.id       = UUID.randomUUID();
        this.alive    = true;
        this.position = spawnRandomPosition();
        this.velocity = spawnRandomVelocity();
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

        // Yatay sınır yansıması
        if (newPos.x < minX || newPos.x > maxX) {
            velocity = velocity.reflectX();
            newPos   = new Vector2D(
                    Math.max(minX, Math.min(maxX, newPos.x)),
                    newPos.y
            );
        }

        // Dikey sınır yansıması
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
     * Gemiyi sweep-tabanlı opaklıkla JOGL bağlamına çizer.
     *
     * <p>Opaklık hesabı: sweep çizgisi geminin hemen üstünden geçtiğinde
     * {@code 1.0f} (tam parlak), sweep uzaklaştıkça {@code sweepFadeDistance}
     * mesafesinde {@code 0.0f}'a düşer.</p>
     *
     * <p>Yalnızca JOGL render thread'inden çağrılmalıdır.</p>
     */
    @Override
    public void render(GL2 gl, RenderContext ctx) {
        float opacity = computeSweepOpacity(position.y, ctx.getSweepY());
        if (opacity <= 0.01f) {
            return; // Görünmez — çizme
        }

        ShipRenderer.drawSquare(
                gl,
                position,
                opacity,
                config.getShipSize(),
                config.getShipColorR(),
                config.getShipColorG(),
                config.getShipColorB()
        );
    }

    // -------------------------------------------------------------------------
    // Sweep Opaklık Hesabı (Private)
    // -------------------------------------------------------------------------

    /**
     * Geminin sweep pozisyonuna göre opaklığını hesaplar.
     *
     * <p>Formül:
     * <ul>
     *   <li>{@code sweepY < shipY} → Sweep henüz bu gemiye ulaşmadı → {@code 0.0f}</li>
     *   <li>{@code 0 ≤ distance ≤ fadeDistance} → Lineer sönükleme: {@code 1 - d/fd}</li>
     *   <li>{@code distance > fadeDistance} → Tamamen sönük → {@code 0.0f}</li>
     * </ul>
     * </p>
     *
     * @param shipY  Geminin Y koordinatı.
     * @param sweepY Sweep çizgisinin mevcut Y koordinatı.
     * @return [0.0, 1.0] arasında opaklık değeri.
     */
    private float computeSweepOpacity(double shipY, double sweepY) {
        double distance     = sweepY - shipY;
        double fadeDistance = config.getSweepFadeDistance();

        if (distance < 0.0 || distance > fadeDistance) {
            return 0.0f;
        }
        return (float) (1.0 - (distance / fadeDistance));
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
