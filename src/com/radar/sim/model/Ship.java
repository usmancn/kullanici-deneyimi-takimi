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

    private static final Random RANDOM = new Random();


    public Ship(SimulationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        this.config           = config;
        this.id               = UUID.randomUUID();
        this.alive            = true;
        this.position         = spawnRandomPosition();
        this.velocity         = spawnRandomVelocity();
        this.customName       = "Gemi-" + id.toString().substring(0, 4);
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
        // Kullanici istegi uzerine gemilerin hareket etmesi gecici olarak durduruldu.
        // Gemiler rastgele dogduklari yerde sabit kalacaklar.
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
