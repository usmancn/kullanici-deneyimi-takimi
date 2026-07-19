package com.radar.sim.model;

/**
 * İki boyutlu bir vektörü veya noktayı temsil eden değer nesnesi (value object).
 *
 * Nesne değiştirilemez (immutable) tasarlanmıştır: tüm işlem metotları
 * mevcut nesneyi değiştirmek yerine yeni bir Vector2D örneği döndürür.
 * Bu sayede çoklu thread'ler aynı vektörü güvenle paylaşabilir.
 */
public final class Vector2D {

    /** Yatay bileşen. */
    public final double x;

    /** Dikey bileşen. */
    public final double y;

    /** Başlangıç noktasını temsil eden sabit. */
    public static final Vector2D ZERO = new Vector2D(0.0, 0.0);

    /**
     * Belirtilen bileşenlerle yeni bir vektör oluşturur.
     *
     * @param x Yatay bileşen.
     * @param y Dikey bileşen.
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Bu vektörü başka bir vektörle toplar.
     *
     * @param other Toplanacak vektör; null olamaz.
     * @return Toplam vektör (yeni nesne).
     */
    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    /**
     * Bu vektörden başka bir vektörü çıkarır.
     *
     * @param other Çıkarılacak vektör; null olamaz.
     * @return Fark vektörü (yeni nesne).
     */
    public Vector2D subtract(Vector2D other) {
        return new Vector2D(this.x - other.x, this.y - other.y);
    }

    /**
     * Bu vektörü skaler bir değerle çarpar.
     *
     * @param scalar Çarpan.
     * @return Ölçeklenmiş vektör (yeni nesne).
     */
    public Vector2D scale(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }

    /**
     * Bu vektörün Öklid uzunluğunu (büyüklüğünü) döndürür.
     *
     * @return Vektörün uzunluğu; negatif olamaz.
     */
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    /**
     * Bu vektörün birim vektörünü (normalize edilmiş halini) döndürür.
     * Sıfır vektör için {@link Vector2D#ZERO} döndürür.
     *
     * @return Birim vektör (yeni nesne).
     */
    public Vector2D normalize() {
        double len = length();
        if (len == 0.0) {
            return ZERO;
        }
        return new Vector2D(this.x / len, this.y / len);
    }

    /**
     * X bileşeninin işaretini tersine çevirir (yatay yansıma).
     *
     * @return Yatay yansıması alınmış vektör (yeni nesne).
     */
    public Vector2D reflectX() {
        return new Vector2D(-this.x, this.y);
    }

    /**
     * Y bileşeninin işaretini tersine çevirir (dikey yansıma).
     *
     * @return Dikey yansıması alınmış vektör (yeni nesne).
     */
    public Vector2D reflectY() {
        return new Vector2D(this.x, -this.y);
    }

    @Override
    public String toString() {
        return String.format("Vector2D(%.2f, %.2f)", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Vector2D)) {
            return false;
        }
        Vector2D other = (Vector2D) obj;
        return Double.compare(this.x, other.x) == 0
                && Double.compare(this.y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }
}
