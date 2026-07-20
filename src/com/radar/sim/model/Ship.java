package com.radar.sim.model;

import com.radar.config.SimulationConfig;
import com.radar.sim.core.ISimulationEntity;

import java.util.Random;
import java.util.UUID;

/**
 * Simülasyon sahnesindeki hareketli hedefi (gemi) temsil eder.
 * Temel pozisyon, hız ve kimlik verilerini barındırır.
 */
public final class Ship implements ISimulationEntity {

    private final UUID id;

    /** Gerçek anlık pozisyon; simülasyon thread'i yazar, render thread'i okur. */
    private volatile Vector2D position;

    /** Hız vektörü (piksel/saniye). */
    private volatile Vector2D velocity;

    private volatile boolean alive;
    private final SimulationConfig config;

    private volatile boolean marked = false;
    private volatile String customName;

    /** Geminin metre cinsinden gövde boyutları (spawn'da rastgele atanır, sabittir). */
    private final double width;
    private final double height;

    /**
     * Geminin radar kesit alanına (RCS) karşılık gelen gain factor'ü.
     * Gemi alanı (width * height) ile doğru orantılı olarak
     * [config.minGainFactor, config.maxGainFactor] aralığına eşlenir.
     */
    private final double gainFactor;

    private static final Random RANDOM = new Random();


    public Ship(SimulationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        this.config           = config;
        this.id               = UUID.randomUUID();
        this.alive            = true;
        this.width            = spawnRandomDimension();
        this.height           = spawnRandomDimension();
        this.gainFactor       = computeGainFactor(this.width, this.height);
        this.position         = spawnRandomPosition();
        this.velocity         = spawnRandomVelocity();
        this.customName       = "Gemi-" + id.toString().substring(0, 4);
    }

    /** Geminin metre cinsinden genişliği. */
    public double getWidth() {
        return width;
    }

    /** Geminin metre cinsinden yüksekliği. */
    public double getHeight() {
        return height;
    }

    /**
     * Geminin gain factor'ü (RCS benzeri): 0 ile 1 arasında, alanla doğru orantılı.
     * Görselleştirmede mavi ton yoğunluğu ve gain filtresi bu değeri kullanır.
     */
    public double getGainFactor() {
        return gainFactor;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
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

    @Override
    public void update(double deltaTime) {
        move(deltaTime);
    }

    // -------------------------------------------------------------------------
    // IMovable
    // -------------------------------------------------------------------------

    @Override
    public void move(double deltaTime) {
        Vector2D vel = velocity;
        double x = position.x + vel.x * deltaTime;
        double y = position.y + vel.y * deltaTime;

        // Geminin hiçbir kısmı matris dışına taşmasın: merkez, kenardan yarım boy içeride.
        double halfW = width  / 2.0;
        double halfH = height / 2.0;
        double minX = halfW, maxX = config.getRadarWidth()  - halfW;
        double minY = halfH, maxY = config.getRadarHeight() - halfH;

        // Duvarlardan sek: konumu sınıra sabitle, ilgili hız bileşenini ters çevir.
        if (x < minX)      { x = minX; vel = vel.reflectX(); }
        else if (x > maxX) { x = maxX; vel = vel.reflectX(); }
        if (y < minY)      { y = minY; vel = vel.reflectY(); }
        else if (y > maxY) { y = maxY; vel = vel.reflectY(); }

        position = new Vector2D(x, y);
        velocity = vel;
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

    /** Hız vektörünü günceller (çarpışma çözümünde motor tarafından kullanılır). */
    public void setVelocity(Vector2D velocity) {
        if (velocity == null) {
            throw new IllegalArgumentException("Hiz vektoru null olamaz.");
        }
        this.velocity = velocity;
    }


    // -------------------------------------------------------------------------
    // Yardımcı (Private)
    // -------------------------------------------------------------------------

    /** [minShipDimension, maxShipDimension] aralığında rastgele bir boyut (metre) üretir. */
    private double spawnRandomDimension() {
        double min = config.getMinShipDimension();
        double max = config.getMaxShipDimension();
        return min + RANDOM.nextDouble() * (max - min);
    }

    /**
     * Gemi alanını gain factor'e eşler.
     *
     * Alan, [minAlan, maxAlan] aralığından [minGain, maxGain] aralığına doğrusal
     * (alanla doğru orantılı, artan) olarak taşınır. Böylece maxDim×maxDim'lik gemi
     * maxGain'e (ör. 1.0), minDim×minDim'lik gemi minGain'e (ör. 0.20) karşılık gelir.
     */
    private double computeGainFactor(double w, double h) {
        double minDim  = config.getMinShipDimension();
        double maxDim  = config.getMaxShipDimension();
        double minArea = minDim * minDim;
        double maxArea = maxDim * maxDim;
        double area    = w * h;

        double minGain = config.getMinGainFactor();
        double maxGain = config.getMaxGainFactor();

        double normalized = (maxArea - minArea) <= 0.0
                ? 0.0
                : (area - minArea) / (maxArea - minArea);
        if (normalized < 0.0) normalized = 0.0;
        if (normalized > 1.0) normalized = 1.0;

        return minGain + normalized * (maxGain - minGain);
    }

    /**
     * Geminin tamamı matrisin içinde kalacak şekilde rastgele bir merkez konumu üretir.
     * Konum geminin merkezidir; gemi {@code [x±w/2, y±h/2]} kutusunu kapladığından
     * merkez, kenarlardan en az yarım gemi boyu içeride tutulur.
     *
     * Not: Çakışmasız yerleştirme {@code SimulationEngine} tarafından yapılır; burada
     * üretilen konum yalnızca sınır güvenli bir başlangıç adayıdır.
     */
    private Vector2D spawnRandomPosition() {
        double halfW = width / 2.0;
        double halfH = height / 2.0;
        double x = halfW + RANDOM.nextDouble() * (config.getRadarWidth()  - width);
        double y = halfH + RANDOM.nextDouble() * (config.getRadarHeight() - height);
        return new Vector2D(x, y);
    }

    private Vector2D spawnRandomVelocity() {
        double angle = RANDOM.nextDouble() * 2.0 * Math.PI;
        double speed = config.getMinShipSpeed()
                + RANDOM.nextDouble() * (config.getMaxShipSpeed() - config.getMinShipSpeed());
        return new Vector2D(Math.cos(angle) * speed, Math.sin(angle) * speed);
    }
}
