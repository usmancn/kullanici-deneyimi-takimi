package deneme.sim;

/** Radara verilen anlik, degismez gemi ozeti (snapshot elemani). */
public final class Contact {
    public final float x, y, gain, size;
    public final ShipType type;

    public Contact(float x, float y, float gain, float size, ShipType type) {
        this.x = x;
        this.y = y;
        this.gain = gain;
        this.size = size;
        this.type = type;
    }
}
