package com.radar.gl.core;

/**
 * Circular (PPI) radar icin polar esleme.
 *
 * <p>Gemi verisi Kartezyen (x, y) olarak uretilir; kare radar bunu oldugu gibi kullanir.
 * Circular radar ise ayni veriyi <b>polar</b> yorumlar:
 * <pre>
 *   x -> aci (bearing):   x in [0, WORLD_SIZE] -> [0, 2pi)
 *   y -> menzil (radius): y in [0, WORLD_SIZE] -> [0, maxRadius]
 * </pre>
 * Boylece 1000x1000 verinin tamami merkezdeki diskin icine sigar; Kartezyen projeksiyonda
 * disarda kalan kose gemileri artik kaybolmaz. Ham veri (Ship'in x/y'si) degismez, yalnizca
 * gosterim farklidir.
 *
 * <p>Etiketler menzili (y) 0..WORLD_SIZE olarak gosterir; gorsel yaricap ise diske sigmasi
 * icin {@link #maxRadius()} ile olceklenir (kenardaki aci etiketlerine pay birakmak amaciyla
 * yarim dunyanin %92'si).
 */
public final class CircularProjection {

    private CircularProjection() { /* yardimci sinif */ }

    /** Merkezden dis halkaya gorsel yaricap (dunya birimi). y = WORLD_SIZE bu yaricapa duser. */
    public static float maxRadius() {
        return Camera.WORLD_SIZE / 2f * 0.92f;
    }

    /**
     * Menzil ekseninin sikisma orani: maxRadius / WORLD_SIZE (= 0.46).
     *
     * <p>Konumlar bu oranla sikistirildigindan (y=1000 -> yaricap 460), gemi boyutlari da
     * ayni oranla olceklenmelidir; aksi halde gemiler 0..1000 etiket olceginde ~2.17x buyuk
     * gorunur. Yani circular ekrandaki gorsel gemi boyutu = gercek boyut * rangeScale().
     */
    public static float rangeScale() {
        return maxRadius() / Camera.WORLD_SIZE;
    }

    public static float centerX() { return Camera.WORLD_SIZE / 2f; }
    public static float centerY() { return Camera.WORLD_SIZE / 2f; }

    /** x -> aci (radyan). 0 = +X ekseni, saatin tersi yonu (matematiksel). */
    public static double angle(double x) {
        return x / Camera.WORLD_SIZE * 2.0 * Math.PI;
    }

    /** y -> merkezden gorsel yaricap (dunya birimi), [0, maxRadius] araligina kirpili. */
    public static double radius(double y) {
        double max = maxRadius();
        double r = y / Camera.WORLD_SIZE * max;
        if (r < 0.0) return 0.0;
        if (r > max) return max;
        return r;
    }

    /** Gemi (x, y) -> circular ekrandaki dunya X konumu. */
    public static double worldX(double x, double y) {
        return centerX() + radius(y) * Math.cos(angle(x));
    }

    /** Gemi (x, y) -> circular ekrandaki dunya Y konumu. */
    public static double worldY(double x, double y) {
        return centerY() + radius(y) * Math.sin(angle(x));
    }
}
