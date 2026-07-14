package com.radar.renderer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.radar.config.SimulationConfig;
import com.radar.core.ISimulationEntity;
import com.radar.engine.EntityManager;

import java.util.List;
import java.util.logging.Logger;

/**
 * JOGL sahnesi için ana render yöneticisi.
 *
 * <p>Bu sınıf {@link GLEventListener} interface'ini implement eder ve
 * {@link com.jogamp.opengl.awt.GLCanvas}'a bağlanır. Tüm OpenGL yaşam
 * döngüsü olayları ({@code init}, {@code reshape}, {@code display},
 * {@code dispose}) buradan yönetilir.</p>
 *
 * <p><b>Anti-Aliasing:</b> {@code init()} metodunda etkinleştirilir:
 * <ul>
 *   <li>{@code GL_LINE_SMOOTH}</li>
 *   <li>{@code GL_POINT_SMOOTH}</li>
 *   <li>{@code GL_POLYGON_SMOOTH}</li>
 *   <li>{@code GL_BLEND} ({@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA})</li>
 * </ul>
 * </p>
 *
 * <p><b>Koordinat sistemi:</b> {@code gluOrtho2D(0, width, 0, height)} ile
 * sol-alt köşe {@code (0, 0)} olacak şekilde ayarlanır.</p>
 *
 * <p><b>Thread güvenliği:</b> Tüm GL çağrıları JOGL render thread'inde yapılır.
 * Varlık listesi {@link EntityManager#getAll()} üzerinden okunur;
 * bu metot thread-safe bir görünüm döndürür.</p>
 */
public final class RadarRenderer implements GLEventListener {

    private static final Logger LOGGER = Logger.getLogger(RadarRenderer.class.getName());

    private final SimulationConfig config;
    private final EntityManager    entityManager;

    /**
     * Yeni bir radar renderer oluşturur.
     *
     * @param config        Konfigürasyon nesnesi; null olamaz.
     * @param entityManager Render sırasında çizilecek varlıkları içeren yönetici; null olamaz.
     */
    public RadarRenderer(SimulationConfig config, EntityManager entityManager) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        if (entityManager == null) {
            throw new IllegalArgumentException("EntityManager null olamaz.");
        }
        this.config        = config;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------------------------
    // GLEventListener
    // -------------------------------------------------------------------------

    /**
     * OpenGL bağlamı ilk oluşturulduğunda çağrılır.
     * Anti-aliasing, harmanlama (blending) ve arka plan rengi burada ayarlanır.
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Anti-aliasing
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_POLYGON_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT,    GL.GL_NICEST);
        gl.glHint(GL2.GL_POINT_SMOOTH_HINT,  GL.GL_NICEST);
        gl.glHint(GL2.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);

        // Alfa harmanlama (opaklık için zorunlu)
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // Arka plan rengi (konfigürasyondan)
        gl.glClearColor(
                config.getBgColorR(),
                config.getBgColorG(),
                config.getBgColorB(),
                1.0f
        );

        LOGGER.info("RadarRenderer baslatildi; anti-aliasing ve blend aktif.");
    }

    /**
     * GLCanvas boyutu değiştiğinde çağrılır.
     * Viewport ve projeksiyon matrisi güncellenir.
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        // Koordinat sistemi: sol-alt (0,0), sağ-üst (radarWidth, radarHeight)
        gl.glOrtho(
                0, config.getRadarWidth(),
                0, config.getRadarHeight(),
                -1.0, 1.0
        );

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * Her kare çiziminde çağrılır (FPSAnimator tarafından tetiklenir).
     * Tüm aktif varlıklar render edilir.
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // EntityManager thread-safe görünüm döndürür; iterasyon güvenlidir
        List<ISimulationEntity> entities = entityManager.getAll();
        for (ISimulationEntity entity : entities) {
            entity.render(gl);
        }

        gl.glFlush();
    }

    /**
     * OpenGL bağlamı yok edilmeden önce çağrılır.
     * Kaynak temizleme işlemleri buraya eklenebilir.
     */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        LOGGER.info("RadarRenderer kapatiliyor.");
    }
}
