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
    private static final int TEXT_MARGIN_PX = 4;   // ID ile cember arasi bosluk (piksel)

    /** ID -> mark (thread-safe). ID anahtar oldugu icin ayni hedef tekrar birikmez. */
    private static final ConcurrentHashMap<String, Mark> MARKS = new ConcurrentHashMap<>();

    private final int centerX;
    private final int centerY;
    private final String id;

    private Mark(int centerX, int centerY, String id) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.id = id;
    }

    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public String getId()   { return id; }

    /** Tanimli (ID'li) bir hedefi mark olarak kaydeder/gunceller. ID null ise yok sayar. */
    public static void register(int centerX, int centerY, String id) {
        if (id == null) return;
        MARKS.put(id, new Mark(centerX, centerY, id));
    }

    public static Collection<Mark> all() {
        return MARKS.values();
    }

    public static void clear() {
        MARKS.clear();
    }

    /**
     * Tum marklarin etrafina cember (dunya-uzayi, zoom ile olceklenir) cizer,
     * ustune ID yazar (ekran-uzayi, sabit boyut - kararli, buyume yok).
     *
     * @param width/height drawable piksel boyutu (ID'yi piksele projekte etmek icin)
     * @param radius       cember yaricapi (dunya birimi)
     */
    public static void draw(GL2 gl, TextRenderer text, float[] matrix,
                            int width, int height, float radius, Mapper mapper) {
        Collection<Mark> marks = MARKS.values();
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

        gl.glColor3f(1f, 1f, 0f);
        gl.glLineWidth(2f);
        for (Mark mark : marks) {
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
        text.setColor(1f, 1f, 1f, 1f);
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
            text.draw(mark.id, pixelX - textWidth / 2, pixelY + radiusPx + TEXT_MARGIN_PX);
        }
        text.endRendering();
    }
}
