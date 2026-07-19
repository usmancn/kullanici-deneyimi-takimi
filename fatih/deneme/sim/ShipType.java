package deneme.sim;

/**
 * Onceden ozellikleri belli gemi cesitleri.
 * Her cesit 0.0 (deniz) noktasindan ve birbirinden olabildigince uzak
 * bir gain factor tasir: 0.25 - 0.50 - 0.75 - 1.00 (esit araliklarla dagitildi).
 */
public enum ShipType {
    SAMANDIRA("Samandira", 0.25f,  6f),
    BALIKCI  ("Balikci",   0.50f,  9f),
    KARGO    ("Kargo",     0.75f, 13f),
    FIRKATEYN("Firkateyn", 1.00f, 11f);

    /** Ekranda gosterilecek ad. */
    public final String label;
    /** 0.0 - 1.0 arasi radar yansima siddeti. Deniz her yerde 0.0'dir. */
    public final float gain;
    /** Geminin piksel cinsinden karesel boyutu. */
    public final float size;

    ShipType(String label, float gain, float size) {
        this.label = label;
        this.gain  = gain;
        this.size  = size;
    }
}
