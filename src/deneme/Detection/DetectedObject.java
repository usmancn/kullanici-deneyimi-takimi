package deneme.Detection;

/**
 * Scanline dedektorunun tespit ettigi bir obje: sinirlari, merkezi ve
 * (Simulation'dan sorulan) ID bilgisi. {@code id == null} ise ID yok (false).
 */
public class DetectedObject {

    public final int centerX;
    public final int centerY;
    public final int xMin;
    public final int xMax;
    public final int yMin;
    public final int yMax;
    public final String id;   // ID varsa; yoksa null (false)
    public final double gain; // obje uzerindeki en yuksek gain

    public DetectedObject(int centerX, int centerY,
                          int xMin, int xMax, int yMin, int yMax, String id, double gain) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.id = id;
        this.gain = gain;
    }

    /** Hedefin ID'si var mi (yani tanimli bir hedefe mi denk geldi). */
    public boolean isIdentified() {
        return id != null;
    }

    @Override
    public String toString() {
        return "DetectedObject[merkez=(" + centerX + "," + centerY + "), "
                + "x=[" + xMin + "," + xMax + "], y=[" + yMin + "," + yMax + "], "
                + "id=" + (id != null ? id : "false") + ", gain=" + gain + "]";
    }
}
