package com.radar.model;

import com.jogamp.opengl.GL2;
import com.radar.config.SimulationConfig;
import com.radar.core.ISimulationEntity;
import com.radar.renderer.ShipRenderer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.UUID;

/**
 * Simülasyon sahnesindeki bir gemiyi temsil eder.
 *
 * <p><b>Hareket:</b> Sabit bir hız vektörüyle düz çizgi hareket eder.
 * Alan sınırlarına çarptığında ilgili eksen yönünde yansıma (reflection) yapar;
 * bu sayede hiçbir zaman ekran dışına çıkmaz.</p>
 *
 * <p><b>İz (Trail):</b> Her {@code update()} çağrısında anlık pozisyon
 * bir {@link RadarTrail} nesnesi olarak iz kuyruğunun başına eklenir.
 * Kuyruk uzunluğu {@link SimulationConfig#getTrailLength()} ile sınırlandırılır.
 * Her yeni eleman tam opak ({@code 1.0f}), sonrakiler
 * {@code fadeFactor^i} formülüyle giderek şeffaflaşır.</p>
 *
 * <p><b>Thread güvenliği:</b> {@code trail} kuyruğuna yalnızca simülasyon
 * thread'i yazar; render thread'i okur. Kopyalama ile okuma yapıldığından
 * senkronizasyon gerekmez (bkz. {@link #getTrailSnapshot()}).</p>
 */
public final class Ship implements ISimulationEntity {

    private final UUID id;

    /** Mevcut pozisyon; her update'de değişir. */
    private volatile Vector2D position;

    /** Hız vektörü (piksel/saniye). */
    private volatile Vector2D velocity;

    /**
     * İz kuyruğu: indeks 0 en yeni, son indeks en eski anlık görüntüdür.
     * Yalnızca simülasyon thread'inden erişilir.
     */
    private final Deque<RadarTrail> trail;

    /** Geminin simülasyondan kaldırılıp kaldırılmayacağını belirtir. */
    private volatile boolean alive;

    private final SimulationConfig config;

    private static final Random RANDOM = new Random();

    /**
     * Verilen konfigürasyona göre rastgele bir pozisyon ve hızla yeni bir gemi oluşturur.
     *
     * @param config Simülasyon konfigürasyonu; null olamaz.
     */
    public Ship(SimulationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        this.config   = config;
        this.id       = UUID.randomUUID();
        this.trail    = new ArrayDeque<>();
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
     * Gemiyi hareket ettirir ve iz kuyruğunu günceller.
     * Yalnızca simülasyon thread'inden çağrılmalıdır.
     */
    @Override
    public void update(double deltaTime) {
        move(deltaTime);
        addTrailPoint();
        trimTrail();
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
            newPos   = new Vector2D(Math.max(minX, Math.min(maxX, newPos.x)), newPos.y);
        }

        // Dikey sınır yansıması
        if (newPos.y < minY || newPos.y > maxY) {
            velocity = velocity.reflectY();
            newPos   = new Vector2D(newPos.x, Math.max(minY, Math.min(maxY, newPos.y)));
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
     * Gemiyi ve izini JOGL bağlamına çizer.
     * Yalnızca JOGL render thread'inden çağrılmalıdır.
     */
    @Override
    public void render(GL2 gl) {
        // İz anlık görüntüsü üzerinden güvenle iterasyon yapılır
        RadarTrail[] snapshot = getTrailSnapshot();

        float colorR = config.getShipColorR();
        float colorG = config.getShipColorG();
        float colorB = config.getShipColorB();
        int   size   = config.getShipSize();

        for (RadarTrail trailPoint : snapshot) {
            ShipRenderer.drawPyramidTop(
                    gl,
                    trailPoint.getPosition(),
                    trailPoint.getOpacity(),
                    size,
                    colorR, colorG, colorB
            );
        }

        // En üstteki nokta (geminin kendisi) tam opak olarak çizilir
        ShipRenderer.drawPyramidTop(
                gl,
                position,
                1.0f,
                size,
                colorR, colorG, colorB
        );
    }

    // -------------------------------------------------------------------------
    // Yardımcı (Private)
    // -------------------------------------------------------------------------

    /**
     * Konfigürasyon sınırları içinde rastgele bir spawn noktası üretir.
     */
    private Vector2D spawnRandomPosition() {
        double x = RANDOM.nextDouble() * config.getRadarWidth();
        double y = RANDOM.nextDouble() * config.getRadarHeight();
        return new Vector2D(x, y);
    }

    /**
     * Min/max hız aralığında rastgele bir hız vektörü üretir.
     * Yön açısı 0–360 derece arasında rastgele seçilir.
     */
    private Vector2D spawnRandomVelocity() {
        double angle = RANDOM.nextDouble() * 2.0 * Math.PI;
        double speed = config.getMinShipSpeed()
                + RANDOM.nextDouble() * (config.getMaxShipSpeed() - config.getMinShipSpeed());
        return new Vector2D(Math.cos(angle) * speed, Math.sin(angle) * speed);
    }

    /**
     * Mevcut pozisyonu fadeFactor ile hesaplanmış opaklıkla iz kuyruğuna ekler.
     * Her yeni eleman kuyruğun önüne (baş) eklenir; en eski arka tarafta kalır.
     */
    private void addTrailPoint() {
        // Mevcut izin opaklıklarını kaydır: 0.indeksteki (en yeni) fadeFactor ile çarpılır
        // Yeni nokta her zaman tam opak (1.0f) başlar;
        // eski noktalar zaten kendi opaklıklarına sahip.
        // Hesap: i. iz noktasının opaklığı = fadeFactor^i
        float firstOpacity = config.getFadeFactor();

        // Mevcut kuyruk başına yeni anlık görüntü eklenir
        // (önceki "baş" şimdi 1. indekse kayar, dolayısıyla opaklığı fadeFactor^1)
        trail.addFirst(new RadarTrail(position, firstOpacity));
    }

    /**
     * İz kuyruğunu maksimum uzunlukla sınırlandırır;
     * fazla eski noktaları kuyruk sonundan (en eski) kaldırır.
     */
    private void trimTrail() {
        int maxTrail = config.getTrailLength();
        while (trail.size() > maxTrail) {
            trail.removeLast();
        }
    }

    /**
     * İz kuyruğunun thread-safe anlık görüntüsünü döndürür.
     * Her eleman için gerçek opaklık {@code fadeFactor^i} olarak yeniden hesaplanır;
     * bu sayede konfigürasyon çalışma zamanında değişse bile render doğru görünür.
     *
     * @return İz noktaları dizisi; indeks 0 en yeni, son indeks en eski.
     */
    public RadarTrail[] getTrailSnapshot() {
        Object[] raw        = trail.toArray();
        RadarTrail[] result = new RadarTrail[raw.length];
        float fadeFactor    = config.getFadeFactor();

        for (int i = 0; i < raw.length; i++) {
            RadarTrail original = (RadarTrail) raw[i];
            // i=0 → fadeFactor^1, i=1 → fadeFactor^2, ...
            float recomputedOpacity = (float) Math.pow(fadeFactor, i + 1);
            result[i] = new RadarTrail(original.getPosition(), recomputedOpacity);
        }
        return result;
    }
}
