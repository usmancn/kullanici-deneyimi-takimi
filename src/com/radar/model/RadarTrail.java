package com.radar.model;

/**
 * Bir geminin bıraktığı iz üzerindeki tek bir anlık görüntüyü (snapshot) temsil eder.
 *
 * <p>Her {@link Ship#update(double)} çağrısında geminin mevcut pozisyonu
 * bir {@code RadarTrail} nesnesi olarak iz listesinin başına eklenir.
 * İz listesi {@code SimulationConfig#getTrailLength()} değeriyle sınırlandırılır.</p>
 *
 * <p>Opaklık ({@code opacity}) render sırasında doğrudan OpenGL'e
 * ({@code glColor4f}) aktarılır; iz boyunca azalan opaklık "sönükleşme"
 * (fading) efektini oluşturur.</p>
 */
public final class RadarTrail {

    /** İzin bu adımda bulunduğu dünya koordinatı. */
    private final Vector2D position;

    /**
     * Bu iz adımının opaklık değeri [0.0, 1.0].
     * 1.0 tam opak (geminin başlangıç noktası), 0.0 tamamen şeffaf.
     */
    private final float opacity;

    /**
     * Yeni bir iz adımı oluşturur.
     *
     * @param position Bu adımın dünya koordinatı; null olamaz.
     * @param opacity  Opaklık değeri; [0.0, 1.0] aralığında olmalıdır.
     * @throws IllegalArgumentException opacity aralık dışındaysa.
     */
    public RadarTrail(Vector2D position, float opacity) {
        if (position == null) {
            throw new IllegalArgumentException("RadarTrail pozisyonu null olamaz.");
        }
        if (opacity < 0.0f || opacity > 1.0f) {
            throw new IllegalArgumentException(
                    "Opaklık [0.0, 1.0] araliginda olmalidir: " + opacity);
        }
        this.position = position;
        this.opacity  = opacity;
    }

    /**
     * Bu iz adımının dünya koordinatını döndürür.
     *
     * @return Pozisyon; null olamaz.
     */
    public Vector2D getPosition() {
        return position;
    }

    /**
     * Bu iz adımının opaklık değerini döndürür.
     *
     * @return [0.0, 1.0] arasında opaklık.
     */
    public float getOpacity() {
        return opacity;
    }

    @Override
    public String toString() {
        return String.format("RadarTrail[pos=%s, opacity=%.2f]", position, opacity);
    }
}
