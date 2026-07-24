package deneme.GLCore;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

/**
 * Tanimli (ID'li) hedef isaretleri icin tek sinifta toplanmis kayit + cizim.
 *
 * <p>Dedektor bir objeyi tanimlayinca (ID'si varsa) {@link #register} ile buraya
 * yazar; ayni hedef her taramada ayni ID ile guncellenir (tekrar birikmez).
 * Square ve Circular canvas'lar her karede {@link #draw} ile isaretli marklarin
 * etrafina {@link MarkStyle} ile secilen sekli (kare / daire / ucgen) cizer.
 * Konum donusumu (kare / polar) disaridan {@link Mapper} ile verilir.
 * ID etiketleri ayri bir ozelliktir, {@link IDLabel} cizer.
 *
 * <p>Cizim sabit-fonksiyon (immediate mode) ile dunya-uzayinda yapilir
 * (zoom ile olceklenir).
 */
public final class Mark {

    /** Bir mark'in dunya konumu (kare ve polar farkli oldugundan disaridan verilir): {x, y}. */
    public interface Mapper {
        float[] world(Mark mark);
    }

    private static final int CIRCLE_SEGMENTS = 48;

    /** ID -> mark (thread-safe). ID anahtar oldugu icin ayni hedef tekrar birikmez. */
    private static final ConcurrentHashMap<String, Mark> MARKS = new ConcurrentHashMap<>();

    private final int centerX;
    private final int centerY;
    private final String id;
    private final float gain;      // hedefin gain degeri: gain filtresiyle karsilastirilir
    private volatile boolean marked;   // sari cember cizilsin mi (sag tik menusu belirler)

    private Mark(int centerX, int centerY, String id, float gain) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.id = id;
        this.gain = gain;
    }

    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public String getId()   { return id; }
    public float getGain()  { return gain; }
    public boolean isMarked() { return marked; }

    /**
     * Dedektorun tespit ettigi hedefi kaydeder/gunceller. ID null ise yok sayar.
     *
     * <p>Hedefler <b>unmarked</b> baslar: kayit sadece ID etiketinin cizilmesini
     * saglar, sari cember icin kullanicinin sag tik menusunden isaretlemesi
     * gerekir. Her taramada tekrar cagrildigi icin mevcut isaret durumu korunur.
     */
    public static void register(int centerX, int centerY, String id, float gain) {
        if (id == null) return;
        MARKS.compute(id, (key, previous) -> {
            Mark created = new Mark(centerX, centerY, key, gain);
            created.marked = (previous != null) && previous.marked;
            return created;
        });
    }

    /** Verilen ID isaretli mi (sari cemberi var mi). */
    public static boolean isMarked(String id) {
        if (id == null) return false;
        Mark mark = MARKS.get(id);
        return mark != null && mark.marked;
    }

    /** Sag tik menusu - Mark: cember cizilmeye baslar. */
    public static void mark(String id) {
        setMarked(id, true);
    }

    /** Sag tik menusu - Unmark: cember kalkar, ID etiketi kalir. */
    public static void unmark(String id) {
        setMarked(id, false);
    }

    private static void setMarked(String id, boolean value) {
        if (id == null) return;
        Mark mark = MARKS.get(id);
        if (mark != null) mark.marked = value;
    }

    /** Kaydi tamamen siler (ID etiketi de kaybolur). */
    public static void remove(String id) {
        if (id == null) return;
        MARKS.remove(id);
    }

    public static Collection<Mark> all() {
        return MARKS.values();
    }

    public static void clear() {
        MARKS.clear();
    }

    /**
     * <b>Isaretli</b> hedeflerin etrafina {@link MarkStyle} ile secilen sekli
     * (kare / daire / ucgen, dunya-uzayi, zoom ile olceklenir) cizer.
     *
     * <p>Gain filtresi disinda kalan hedeflerin mark'i cizilmez (kayit silinmez,
     * hedef araliga geri girdiginde tekrar gorunur) - boylece ekrandaki gain
     * gorseli ile isaretler tutarli kalir.
     *
     * @param style               sekil tipi + boyut + renk (MarkBuilder uretir)
     * @param filterMin/filterMax gain filtresi araligi (shader ile ayni)
     */
    public static void draw(GL2 gl, float[] matrix, MarkStyle style,
                            float filterMin, float filterMax, Mapper mapper) {
        if (MARKS.isEmpty()) return;

        // sadece gain'i filtre araliginda ve isaretli olanlar
        Collection<Mark> marks = new java.util.ArrayList<>();
        for (Mark mark : MARKS.values()) {
            if (mark.marked && mark.gain >= filterMin && mark.gain <= filterMax) {
                marks.add(mark);
            }
        }
        if (marks.isEmpty()) return;

        // ---- sekiller: dunya-uzayi, kamera matrisi projeksiyonda ----
        gl.glUseProgram(0);
        // shader'dan kalan aktif vertex-attribute dizileri sabit-fonksiyonu bozar -> kapat
        for (int i = 0; i < 8; i++) {
            gl.glDisableVertexAttribArray(i);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadMatrixf(matrix, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        java.awt.Color color = style.getColor();
        float radius = style.getSize();
        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        gl.glLineWidth(2f);
        for (Mark mark : marks) {
            float[] p = mapper.world(mark);
            drawShape(gl, style.getType(), p[0], p[1], radius);
        }
        gl.glLineWidth(1f);
    }

    /** Secilen tipe gore merkezi (x,y) olan sekli LINE_LOOP ile cizer. */
    private static void drawShape(GL2 gl, MarkType type, float x, float y, float radius) {
        gl.glBegin(GL.GL_LINE_LOOP);
        switch (type) {
            case SQUARE:
                gl.glVertex2f(x - radius, y - radius);
                gl.glVertex2f(x + radius, y - radius);
                gl.glVertex2f(x + radius, y + radius);
                gl.glVertex2f(x - radius, y + radius);
                break;
            case TRIANGLE:
                // tepe yukarida esit kenar ucgen
                gl.glVertex2f(x, y + radius);
                gl.glVertex2f(x - radius * 0.866f, y - radius * 0.5f);
                gl.glVertex2f(x + radius * 0.866f, y - radius * 0.5f);
                break;
            case CIRCLE:
            default:
                for (int k = 0; k < CIRCLE_SEGMENTS; k++) {
                    double a = 2.0 * Math.PI * k / CIRCLE_SEGMENTS;
                    gl.glVertex2f(x + radius * (float) Math.cos(a),
                                  y + radius * (float) Math.sin(a));
                }
                break;
        }
        gl.glEnd();
    }
}
