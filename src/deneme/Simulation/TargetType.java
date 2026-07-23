package deneme.Simulation;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hedef turu.
 *
 * <p>Calisma zamaninda (sag tik menusundeki "Yeni tur...") yeni tur
 * eklenebilmesi gerektigi icin enum degil, statik kayit defterli bir siniftir:
 * Java'da bir enum'a calisma zamaninda sabit eklenemez. Kullanim enum ile ayni
 * kalir - {@code TargetType.BIG}, {@link #values()}, {@link #name()} calisir.
 *
 * <p>Kayit defteri {@link CopyOnWriteArrayList} oldugundan tur ekleme (EDT) ile
 * okuma (GL / dedektor thread'i) birbirini bozmaz.
 */
public final class TargetType {

    private static final CopyOnWriteArrayList<TargetType> REGISTRY = new CopyOnWriteArrayList<>();

    // ---- baslangictaki sabit turler ----
    public static final TargetType BIG       = define("BIG", 20, 20);
    public static final TargetType SMALL     = define("SMALL", 10, 10);
    public static final TargetType RECTANGLE = define("RECTANGLE", 20, 10);

    private final String name;
    private final int width;
    private final int height;

    private TargetType(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    /**
     * Turu kayit defterine ekler. Ayni isimde tur varsa yenisi olusturulmaz,
     * mevcut olan doner (tur isimleri benzersizdir).
     */
    public static TargetType define(String name, int width, int height) {
        TargetType existing = valueOf(name);
        if (existing != null) return existing;

        TargetType created = new TargetType(name, width, height);
        REGISTRY.add(created);
        return created;
    }

    /** Kayitli tum turler (ekleme sirasiyla). */
    public static TargetType[] values() {
        return REGISTRY.toArray(new TargetType[0]);
    }

    /** Isimden tur; yoksa null (enum'un aksine exception atmaz). */
    public static TargetType valueOf(String name) {
        if (name == null) return null;
        for (TargetType type : REGISTRY) {
            if (type.name.equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    public String name() {
        return this.name;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public String getTypeName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
