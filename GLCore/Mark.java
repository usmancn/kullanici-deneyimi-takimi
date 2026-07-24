package deneme.GLCore;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Tanimli (ID'li) hedef isaretleri icin tek sinifta toplanmis kayit + cizim.
 *
 * <p>Dedektor bir objeyi tanimlayinca (ID'si varsa) {@link #register} ile buraya
 * yazar; ayni hedef her taramada ayni ID ile guncellenir (tekrar birikmez).
 * Square ve Circular canvas'lar her karede {@link #draw} ile tum marklarin
 * etrafina cember cizip ustune ID yazar. Konum donusumu (kare / polar) disaridan
 * {@link Mapper} ile verilir.
 *
 * <p>Cizim: cember sabit-fonksiyon (immediate mode), ID metni dunya-uzayinda
 * {@link TextRenderer} ile (zoom ile olceklenir). TextRenderer bir GL context'e
 * bagli oldugundan her canvas kendi ornegini verir.
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
     * Tespit edilen tum hedeflerin ustune ID'sini yazar (ekran-uzayi, sabit
     * boyut - kararli, buyume yok); sadece <b>isaretli</b> olanlarin etrafina
     * ayrica sari cember (dunya-uzayi, zoom ile olceklenir) cizer.
     *
     * <p>Gain filtresi disinda kalan hedeflerin mark'i cizilmez (kayit silinmez,
     * hedef araliga geri girdiginde tekrar gorunur) - boylece ekrandaki gain
     * gorseli ile isaretler tutarli kalir.
     *
     * @param width/height drawable piksel boyutu (ID'yi piksele projekte etmek icin)
     * @param radius       cember yaricapi (dunya birimi)
     * @param filterMin/filterMax gain filtresi araligi (shader ile ayni)
     */
    public static void draw(GL2 gl, TextRenderer text, float[] matrix,
                            int width, int height, float radius,
                            float filterMin, float filterMax, Mapper mapper) {
        draw(gl, text, matrix, width, height, radius,
             filterMin, filterMax, TargetStyle.defaults(), mapper);
    }

    public static void draw(GL2 gl, TextRenderer text, float[] matrix,
                            int width, int height, float radius,
                            float filterMin, float filterMax,
                            TargetStyle style, Mapper mapper) {
        if (MARKS.isEmpty()) return;

        // sadece gain'i filtre araliginda olanlar
        Collection<Mark> marks = new java.util.ArrayList<>();
        for (Mark mark : MARKS.values()) {
            if (mark.gain >= filterMin && mark.gain <= filterMax) {
                marks.add(mark);
            }
        }
        if (marks.isEmpty()) return;

        // ---- cemberler (sari) : dunya-uzayi, kamera matrisi projeksiyonda ----
        gl.glUseProgram(0);
        // shader'dan kalan aktif vertex-attribute dizileri TextRenderer'i bozar -> kapat (bkz. LabelLayer)
        for (int i = 0; i < 8; i++) {
            gl.glDisableVertexAttribArray(i);
        }
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadMatrixf(matrix, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glColor3f(style.markRed, style.markGreen, style.markBlue);
        gl.glLineWidth(style.markLineWidth);
        for (Mark mark : marks) {
            if (!mark.marked) continue;      // isaretlenmemis hedefte sadece ID yazisi olur
            float[] p = mapper.world(mark);
            gl.glBegin(GL.GL_LINE_LOOP);
            for (int k = 0; k < CIRCLE_SEGMENTS; k++) {
                double a = 2.0 * Math.PI * k / CIRCLE_SEGMENTS;
                gl.glVertex2f(p[0] + radius * (float) Math.cos(a),
                              p[1] + radius * (float) Math.sin(a));
            }
            gl.glEnd();
        }
        gl.glLineWidth(1f);

        // ---- ID metinleri : ekran-uzayi, hedefin uzerinde (dunya -> piksel) ----
        // cemberin dikey yaricapinin piksel karsiligi: matrix[5] = 2/rangeY, ndc->px = height/2
        text.beginRendering(width, height);
        text.setColor(style.labelRed, style.labelGreen, style.labelBlue, 1f);
        for (Mark mark : marks) {
            float[] p = mapper.world(mark);
            float clipX = matrix[0] * p[0] + matrix[4] * p[1] + matrix[12];
            float clipY = matrix[1] * p[0] + matrix[5] * p[1] + matrix[13];
            float clipW = matrix[3] * p[0] + matrix[7] * p[1] + matrix[15];
            if (clipW == 0f) continue;
            int pixelX = Math.round((clipX / clipW * 0.5f + 0.5f) * width);
            int pixelY = Math.round((clipY / clipW * 0.5f + 0.5f) * height);

            // cemberin ust kenarini piksel olarak bul -> etiketi hemen ustune, yatayda ortali koy
            int radiusPx = Math.round(radius * Math.abs(matrix[5]) * height * 0.5f);
            int textWidth = Math.round((float) text.getBounds(mark.id).getWidth());
            text.draw(mark.id, pixelX - textWidth / 2, pixelY + radiusPx + style.labelMarginPx);
        }
        text.endRendering();
    }
}
