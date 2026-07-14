package com.radar.renderer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.gl2.GLUT;
import com.radar.config.SimulationConfig;
import com.radar.core.ISimulationEntity;
import com.radar.engine.EntityManager;

import java.util.List;
import java.util.logging.Logger;

/**
 * JOGL sahnesi için ana render yöneticisi — radar ekranı görünümü.
 *
 * <p>Her kare şu sırayla çizilir:
 * <ol>
 *   <li>Arka plan (koyu yeşilimsi) temizlenir.</li>
 *   <li>Izgara (grid) çizgileri ve koordinat etiketleri çizilir.</li>
 *   <li>Sweep çizgisi Y pozisyonu delta-time ile ilerletilir.</li>
 *   <li>Tüm varlıklar sweep-tabanlı opaklıkla render edilir.</li>
 *   <li>Sweep çizgisi ve fosfor glow efekti üstten çizilir.</li>
 * </ol>
 * </p>
 *
 * <p><b>Koordinat sistemi:</b> {@code glOrtho(0, W, 0, H)} ile sol-alt köşe
 * {@code (0, 0)} olarak ayarlanır. Sweep {@code y=0}'dan başlayıp
 * {@code y=H}'e doğru ilerler, sonra sıfırlanır.</p>
 *
 * <p><b>Anti-aliasing:</b> {@code init()} içinde etkinleştirilir.</p>
 */
public final class RadarRenderer implements GLEventListener {

    private static final Logger LOGGER = Logger.getLogger(RadarRenderer.class.getName());

    // Sweep glow şeridinin yüksekliği (dünya koordinatı, piksel)
    private static final float SWEEP_GLOW_HEIGHT = 80.0f;

    // Sweep çizgisi genişliği (piksel, glLineWidth)
    private static final float SWEEP_LINE_WIDTH = 2.0f;

    // Grid çizgi rengi: koyu yeşil, yarı saydam
    private static final float[] GRID_COLOR  = {0.08f, 0.35f, 0.08f, 0.65f};

    // Koordinat etiketi rengi: açık yeşil
    private static final float[] LABEL_COLOR = {0.25f, 0.85f, 0.25f, 0.80f};

    // Sweep çizgisi rengi: parlak yeşil
    private static final float[] SWEEP_COLOR = {0.30f, 1.00f, 0.30f, 1.00f};

    private final SimulationConfig config;
    private final EntityManager    entityManager;
    private final GLUT             glut;

    /** Sweep tarama çizgisinin mevcut Y koordinatı (dünya birimleri). */
    private double sweepY;

    /** Son display() çağrısının nanosaniye zaman damgası. */
    private long lastDisplayNanos;

    /** Mevcut viewport genişliği (piksel), reshape() tarafından güncellenir. */
    private int viewportW;

    /** Mevcut viewport yüksekliği (piksel), reshape() tarafından güncellenir. */
    private int viewportH;

    // Etkileşim durumları
    private double mouseLogicalX = -100.0;
    private double mouseLogicalY = -100.0;
    private boolean showMouseCoords = false;
    
    private boolean pendingClick = false;
    private double clickX, clickY;
    
    private boolean pendingSpace = false;

    /**
     * Yeni bir radar renderer oluşturur.
     *
     * @param config        Konfigürasyon nesnesi; null olamaz.
     * @param entityManager Varlık yöneticisi; null olamaz.
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
        this.glut          = new GLUT();
        this.sweepY        = 0.0;
        this.lastDisplayNanos = System.nanoTime();
    }

    public void updateMousePosition(double logicalX, double logicalY, boolean visible) {
        this.mouseLogicalX = logicalX;
        this.mouseLogicalY = logicalY;
        this.showMouseCoords = visible;
    }

    public void registerClick(double logicalX, double logicalY) {
        this.clickX = logicalX;
        this.clickY = logicalY;
        this.pendingClick = true;
    }

    public void registerSpacePress() {
        this.pendingSpace = true;
    }

    // -------------------------------------------------------------------------
    // GLEventListener
    // -------------------------------------------------------------------------

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Anti-aliasing
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_POLYGON_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT,     GL.GL_NICEST);
        gl.glHint(GL2.GL_POINT_SMOOTH_HINT,   GL.GL_NICEST);
        gl.glHint(GL2.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);

        // Alfa harmanlama
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // Arka plan: koyu yeşilimsi
        gl.glClearColor(
                config.getBgColorR(),
                config.getBgColorG(),
                config.getBgColorB(),
                1.0f
        );

        LOGGER.info("RadarRenderer baslatildi; anti-aliasing ve blend aktif.");
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();

        this.viewportW = width;
        this.viewportH = height;

        gl.glViewport(0, 0, width, height);

        // Projeksiyon: Mantıksal simülasyon alanı HER ZAMAN config'deki boyuttadır (1000x1000).
        // glViewport tüm pencereyi kapladığı için, 1000x1000'lik bu alan
        // ekranın tamamına esnetilir. Böylece koordinatlar hep 1000 üzerinde kalır.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, config.getRadarWidth(), 0, config.getRadarHeight(), -1.0, 1.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Delta-time hesabı
        long nowNanos  = System.nanoTime();
        double deltaSec = (nowNanos - lastDisplayNanos) / 1_000_000_000.0;
        lastDisplayNanos = nowNanos;

        // Sweep çizgisini ilerlet
        advanceSweep(deltaSec);

        // Arka planı temizle
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // 1. Etkileşim İşleme (Tıklama ve Space)
        processInteractions();

        // 2. Izgara çizgileri
        drawGrid(gl);

        // 3. Varlıkları sweep-tabanlı opaklıkla render et
        RenderContext ctx = new RenderContext(sweepY, glut);
        List<ISimulationEntity> entities = entityManager.getAll();
        for (ISimulationEntity entity : entities) {
            entity.render(gl, ctx);
        }

        // 4. Sweep çizgisi (varlıkların üstünde)
        drawSweepLine(gl);

        // 5. Koordinat etiketleri (en üstte)
        drawGridLabels(gl);
        
        // 6. Fare koordinat göstergesi
        drawMouseCoords(gl);

        gl.glFlush();
    }

    private void processInteractions() {
        if (pendingSpace) {
            pendingSpace = false;
            // Space basıldığında sadece farenin altındaki (hover olan) geminin işaretini kaldır
            for (ISimulationEntity entity : entityManager.getAll()) {
                if (entity instanceof com.radar.model.Ship) {
                    com.radar.model.Ship ship = (com.radar.model.Ship) entity;
                    if (ship.isMarked() && ship.hitTest(mouseLogicalX, mouseLogicalY)) {
                        ship.setMarked(false);
                        break;
                    }
                }
            }
        }

        if (pendingClick) {
            pendingClick = false;
            // Tıklanan yere en yakın gemiyi bul
            for (ISimulationEntity entity : entityManager.getAll()) {
                if (entity instanceof com.radar.model.Ship) {
                    com.radar.model.Ship ship = (com.radar.model.Ship) entity;
                    if (ship.hitTest(clickX, clickY)) {
                        ship.setMarked(!ship.isMarked()); // İşaretle veya kaldır
                        break; // Aynı anda sadece birine tıklanmış olsun
                    }
                }
            }
        }
    }

    private void drawMouseCoords(GL2 gl) {
        if (showMouseCoords && mouseLogicalX >= 0 && mouseLogicalY >= 0 
            && mouseLogicalX <= config.getRadarWidth() && mouseLogicalY <= config.getRadarHeight()) {
            
            gl.glColor4f(1.0f, 0.8f, 0.0f, 1.0f); // Sarı/Turuncu renk
            // Farenin 15 birim sağ-üstüne yaz
            gl.glRasterPos2f((float) mouseLogicalX + 15.0f, (float) mouseLogicalY + 15.0f);
            String text = String.format("X:%d Y:%d", (int) mouseLogicalX, (int) mouseLogicalY);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, text);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        LOGGER.info("RadarRenderer kapatiliyor.");
    }

    // -------------------------------------------------------------------------
    // Sweep
    // -------------------------------------------------------------------------

    /**
     * Sweep Y pozisyonunu delta-time ile ilerletir.
     * radarHeight'i aşınca sıfırlanır (alt-üst döngüsü).
     */
    private void advanceSweep(double deltaSec) {
        sweepY += config.getSweepSpeedPps() * deltaSec;
        if (sweepY > config.getRadarHeight()) {
            sweepY = 0.0;
        }
    }

    /**
     * Sweep çizgisi ve altındaki fosfor glow efektini çizer.
     */
    private void drawSweepLine(GL2 gl) {
        float sw = (float) sweepY;
        float radarW = config.getRadarWidth();

        // Fosfor glow: sweep'in altında giderek solan yeşil şerit
        float glowBottom = sw - SWEEP_GLOW_HEIGHT;

        gl.glBegin(GL2.GL_QUADS);
        // Alt kenar → tamamen şeffaf
        gl.glColor4f(SWEEP_COLOR[0], SWEEP_COLOR[1], SWEEP_COLOR[2], 0.0f);
        gl.glVertex2f(0,      glowBottom);
        gl.glVertex2f(radarW, glowBottom);
        // Üst kenar (sweep hattı) → hafif yeşil
        gl.glColor4f(SWEEP_COLOR[0], SWEEP_COLOR[1], SWEEP_COLOR[2], 0.13f);
        gl.glVertex2f(radarW, sw);
        gl.glVertex2f(0,      sw);
        gl.glEnd();

        // Ana sweep çizgisi: parlak yeşil, 2px kalınlık
        gl.glLineWidth(SWEEP_LINE_WIDTH);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor4f(SWEEP_COLOR[0], SWEEP_COLOR[1], SWEEP_COLOR[2], SWEEP_COLOR[3]);
        gl.glVertex2f(0,      sw);
        gl.glVertex2f(radarW, sw);
        gl.glEnd();
        gl.glLineWidth(1.0f);
    }

    // -------------------------------------------------------------------------
    // Grid
    // -------------------------------------------------------------------------

    /**
     * Yatay ve dikey ızgara çizgilerini çizer.
     * Her bölümde eşit aralıklı çizgiler oluşturulur.
     */
    private void drawGrid(GL2 gl) {
        int   divisions = config.getGridDivisions();
        float radarW    = config.getRadarWidth();
        float radarH    = config.getRadarHeight();
        float stepX     = radarW / divisions;
        float stepY     = radarH / divisions;

        gl.glColor4f(GRID_COLOR[0], GRID_COLOR[1], GRID_COLOR[2], GRID_COLOR[3]);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINES);

        // Dikey çizgiler (X ekseninde bölümler)
        for (int i = 0; i <= divisions; i++) {
            float x = i * stepX;
            gl.glVertex2f(x, 0);
            gl.glVertex2f(x, radarH);
        }

        // Yatay çizgiler (Y ekseninde bölümler)
        for (int j = 0; j <= divisions; j++) {
            float y = j * stepY;
            gl.glVertex2f(0,      y);
            gl.glVertex2f(radarW, y);
        }

        gl.glEnd();
    }

    /**
     * Grid çizgilerinin koordinat etiketlerini GLUT bitmap font ile çizer.
     *
     * <p>X ekseni etiketleri alt kenara, Y ekseni etiketleri sol kenara
     * yakın konumlandırılır.</p>
     */
    private void drawGridLabels(GL2 gl) {
        int   divisions = config.getGridDivisions();
        float radarW    = config.getRadarWidth();
        float radarH    = config.getRadarHeight();
        float stepX     = radarW / divisions;
        float stepY     = radarH / divisions;

        // GLUT metni dünya koordinatında rasterPos ile konumlandırılır
        gl.glColor4f(LABEL_COLOR[0], LABEL_COLOR[1], LABEL_COLOR[2], LABEL_COLOR[3]);

        // X ekseni etiketleri (alt): her dikey çizgi noktasına
        for (int i = 0; i <= divisions; i++) {
            float x = i * stepX;
            String label = String.valueOf((int) x);
            // Etiket biraz yukarıda, sol kenarda başlar
            gl.glRasterPos2f(x + 3f, 5f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, label);
        }

        // Y ekseni etiketleri (sol): her yatay çizgi noktasına (0 hariç)
        for (int j = 1; j <= divisions; j++) {
            float y = j * stepY;
            String label = String.valueOf((int) y);
            gl.glRasterPos2f(4f, y + 3f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, label);
        }
    }
}
