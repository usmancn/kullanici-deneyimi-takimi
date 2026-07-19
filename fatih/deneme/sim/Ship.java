package deneme.sim;

import java.util.Random;

/**
 * Hareketli tek bir gemi. Konum dunya koordinati (0..worldSize) cinsinden tutulur.
 * Gemi ufak bir hizla belli bir yone gider; ara sira yon degistirir ve
 * kenarlara carpinca geri doner.
 */
public class Ship {

    private static final float MIN_SPEED = 4f;    // piksel / saniye
    private static final float MAX_SPEED = 16f;   // piksel / saniye
    private static final float MIN_HEADING_SEC = 2.5f;
    private static final float MAX_HEADING_SEC = 6.0f;

    private final ShipType type;
    private float x, y;
    private float vx, vy;
    private float headingTimer;

    public Ship(float x, float y, ShipType type, Random random) {
        this.x = x;
        this.y = y;
        this.type = type;
        pickHeading(random);
    }

    public float x()       { return x; }
    public float y()       { return y; }
    public ShipType type() { return type; }
    public float gain()    { return type.gain; }
    public float size()    { return type.size; }

    private void pickHeading(Random random) {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        float speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);
        vx = (float) Math.cos(angle) * speed;
        vy = (float) Math.sin(angle) * speed;
        headingTimer = MIN_HEADING_SEC + random.nextFloat() * (MAX_HEADING_SEC - MIN_HEADING_SEC);
    }

    /** dtSeconds kadar ilerlet; kenara carpinca yon cevir; suresi dolunca yeni rota sec. */
    public void update(float dtSeconds, float worldSize, Random random) {
        x += vx * dtSeconds;
        y += vy * dtSeconds;

        float half = type.size / 2f;
        if (x < half)             { x = half;             vx =  Math.abs(vx); }
        if (x > worldSize - half) { x = worldSize - half; vx = -Math.abs(vx); }
        if (y < half)             { y = half;             vy =  Math.abs(vy); }
        if (y > worldSize - half) { y = worldSize - half; vy = -Math.abs(vy); }

        headingTimer -= dtSeconds;
        if (headingTimer <= 0f) pickHeading(random);
    }
}
