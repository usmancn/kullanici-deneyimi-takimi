package deneme.GLCore;

import com.jogamp.opengl.GL2;

/**
 * Veri alaninin ekrandaki yeri: her zaman kare ve en fazla 1000x1000 piksel.
 *
 * <p>Pencerenin iki kenari da 1000'i asiyorsa cizim 1000x1000'de kalir (dunyanin
 * bir birimi = bir piksel), artan yer bos pencere olur. Kenarlardan biri
 * 1000'den kucukse kisa kenara sigacak sekilde olceklenir. Boylece pencere ne
 * kadar buyutulurse buyutulsun gorunen dunya hep 1000x1000'dir ve kare oran
 * bozulmadigi icin ayrica aspect duzeltmesine gerek kalmaz.
 */
public final class Viewport {
    public static final int MAX_SIDE = (int) Camera.WORLD_SIZE;   

    private volatile int side = MAX_SIDE;
    private volatile int offsetX = 0;
    private volatile int offsetY = 0;

    /** reshape'te cagrilir: GL viewport'unu ortalanmis kare olarak ayarlar. */
    public void apply(GL2 gl, int width, int height) {
        side = side(width, height);
        offsetX = (width - side) / 2;
        offsetY = (height - side) / 2;
        gl.glViewport(offsetX, offsetY, side, side);
    }

    /** Kare alanin kenar uzunlugu (piksel). Cizim/etiket hesaplari bunu kullanir. */
    public int side() {
        return side;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    /** Verilen pencere boyutu icin kare alanin kenari. */
    public static int side(int width, int height) {
        return Math.max(1, Math.min(Math.min(width, height), MAX_SIDE));
    }

    /** Kare alanin bilesen uzayindaki sol kenari. */
    public static int offsetX(int width, int height) {
        return (width - side(width, height)) / 2;
    }

    /** Kare alanin bilesen uzayindaki ust kenari (fare olaylari sol-ust orijinli). */
    public static int offsetY(int width, int height) {
        return (height - side(width, height)) / 2;
    }

    // ---- fare olayi (bilesen uzayi, sol-ust orijin) -> kare alan icindeki konum ----

    public static int mouseX(int eventX, int componentWidth, int componentHeight) {
        int s = side(componentWidth, componentHeight);
        return clamp(eventX - (componentWidth - s) / 2, s);
    }

    public static int mouseY(int eventY, int componentWidth, int componentHeight) {
        int s = side(componentWidth, componentHeight);
        return clamp(eventY - (componentHeight - s) / 2, s);
    }

    private static int clamp(int value, int max) {
        if (value < 0) return 0;
        if (value > max) return max;
        return value;
    }
}
