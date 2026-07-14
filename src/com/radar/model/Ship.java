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

    private static class Blip {
        final Vector2D position;
        final double spawnTravel;

        Blip(Vector2D position, double spawnTravel) {
            this.position = position;
            this.spawnTravel = spawnTravel;
        }
    }

    private final java.util.LinkedList<Blip> blips = new java.util.LinkedList<>();
    private double totalSweepTravel = 0.0;
    private double prevActualShipY = -1.0;

    /**
     * Önceki render çağrısındaki sweep Y değeri.
     * Sweep geçişini algılamak için kullanılır.
     * Yalnızca render thread'inden erişilir; -1.0 = henüz başlatılmadı.
     */
    private double prevSweepY = -1.0;

    /**
     * Gemi henüz radar (sweep) tarafından hiç taranmadıysa görünmez olmalıdır.
     * İlk geçişte true olur.
     */
    private boolean hasBeenSwept = false;
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

    @Override
    public void render(GL2 gl, RenderContext ctx) {
        double currentSweepY = ctx.getSweepY();
        double actualShipY = position.y; // volatile okuma

        if (prevSweepY >= 0.0 && prevActualShipY >= 0.0) {
            double travel = currentSweepY - prevSweepY;
            if (travel < 0.0) {
                travel += config.getRadarHeight(); // wrap-around
            }
            totalSweepTravel += travel;

            boolean crossed = false;
            boolean sweepNormal = currentSweepY >= prevSweepY;
            boolean shipNormal = Math.abs(actualShipY - prevActualShipY) < 50.0; // sekme yok

            if (sweepNormal && shipNormal) {
                // Continuous collision detection
                double prevDiff = prevActualShipY - prevSweepY;
                double currDiff = actualShipY - currentSweepY;
                if (prevDiff * currDiff <= 0.0) {
                    crossed = true;
                }
            } else if (!sweepNormal) {
                // Sweep wrap-around
                if (actualShipY >= prevSweepY || actualShipY <= currentSweepY) {
                    crossed = true;
                }
            } else {
                // Gemi yansıması (bounce)
                if (actualShipY >= prevSweepY && actualShipY <= currentSweepY) {
                    crossed = true;
                }
            }

            if (crossed) {
                blips.add(new Blip(position, totalSweepTravel));
                hasBeenSwept = true;
            }
        }
        prevSweepY = currentSweepY;
        prevActualShipY = actualShipY;

        if (!hasBeenSwept || blips.isEmpty()) {
            return;
        }

        // Blipleri güncelle ve çiz
        java.util.Iterator<Blip> iter = blips.iterator();
        while (iter.hasNext()) {
            Blip b = iter.next();
            double age = totalSweepTravel - b.spawnTravel;
            
            // Eğer üzerinden 1 tam tur (radar boyu) geçtiyse bu eski izi sil
            if (age > config.getRadarHeight()) {
                if (blips.size() > 1) {
                    iter.remove();
                    continue;
                } else {
                    // Son blip ise silme, tamamen sönük (minOpacity) kalsın
                    age = config.getRadarHeight();
                }
            }
        }

        for (Blip b : blips) {
            double age = totalSweepTravel - b.spawnTravel;
            float opacity = computeOpacity(age);
            ShipRenderer.drawPyramidTop(
                    gl,
                    b.position,
                    opacity,
                    config.getShipSize(),
                    config.getShipColorR(),
                    config.getShipColorG(),
                    config.getShipColorB()
            );
        }

        if (this.marked && !blips.isEmpty()) {
            Blip newest = blips.getLast();
            ShipRenderer.drawMark(gl, newest.position, this.customName, ctx.getGlut());
        }
    }

    /**
     * Verilen (x,y) mantıksal koordinatının, geminin ekranda görünen herhangi bir 
     * izinin (blip) üzerine tıklanıp tıklanmadığını kontrol eder.
     * @param x Tıklanan X (mantıksal 0-1000)
     * @param y Tıklanan Y (mantıksal 0-1000)
     * @return Eğer tıklama bu gemiye isabet ettiyse true
     */
    public boolean hitTest(double x, double y) {
        if (!isAlive()) return false;
        double threshold = config.getShipSize() * 1.5; // Biraz tölerans payı
        for (Blip b : blips) {
            double dx = b.position.x - x;
            double dy = b.position.y - y;
            if (Math.sqrt(dx*dx + dy*dy) <= threshold) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Opaklık Hesabı (Private)
    // -------------------------------------------------------------------------

    private float computeOpacity(double age) {
        float minOpacity    = config.getMinShipOpacity();
        double fadeDistance = config.getSweepFadeDistance();
        
        if (age >= fadeDistance) {
            return minOpacity;
        }

        float t = (float) (age / fadeDistance);
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
