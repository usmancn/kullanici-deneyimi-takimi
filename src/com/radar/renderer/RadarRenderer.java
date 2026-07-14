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
    private boolean pendingClearMarks = false;
    
    // Kamera (Zoom ve Pan) durumları
    private double zoomLevel = 1.0;
    private double cameraX = 0.0;
    private double cameraY = 0.0;

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

    public void updateMousePositionFromPhysical(int px, int py, int w, int h, boolean visible) {
        this.showMouseCoords = visible;
        if (!visible) return;
        
        float fx = (float) px / w;
        float fy = (float) (h - py) / h; // Y ekseni tersine çevrilir
        
        double visibleWidth = config.getRadarWidth() / zoomLevel;
        double visibleHeight = config.getRadarHeight() / zoomLevel;
        
        this.mouseLogicalX = cameraX + fx * visibleWidth;
        this.mouseLogicalY = cameraY + fy * visibleHeight;
    }

    public void registerClickFromPhysical(int px, int py, int w, int h) {
        float fx = (float) px / w;
        float fy = (float) (h - py) / h;
        
        double visibleWidth = config.getRadarWidth() / zoomLevel;
        double visibleHeight = config.getRadarHeight() / zoomLevel;
        
        this.clickX = cameraX + fx * visibleWidth;
        this.clickY = cameraY + fy * visibleHeight;
        this.pendingClick = true;
    }
    
    public void doZoom(int zoomDelta, int mousePx, int mousePy, int w, int h) {
        // Zoom mantığı
        double zoomFactor = 1.2;
        double newZoomLevel = zoomLevel;
        
        if (zoomDelta < 0) { // İleri kaydırma (Yakınlaşma)
            newZoomLevel *= zoomFactor;
        } else if (zoomDelta > 0) { // Geri kaydırma (Uzaklaşma)
            newZoomLevel /= zoomFactor;
        }
        
        // Sınırları belirle [1.0, 5.0]
        newZoomLevel = Math.max(1.0, Math.min(newZoomLevel, 5.0));
        
        if (newZoomLevel == zoomLevel) return; // Değişim yok
        
        // Fare konumunu koruma mantığı
        float fx = (float) mousePx / w;
        float fy = (float) (h - mousePy) / h;
        
        double oldVisibleWidth = config.getRadarWidth() / zoomLevel;
        double oldVisibleHeight = config.getRadarHeight() / zoomLevel;
        
        double logicalMouseX = cameraX + fx * oldVisibleWidth;
        double logicalMouseY = cameraY + fy * oldVisibleHeight;
        
        double newVisibleWidth = config.getRadarWidth() / newZoomLevel;
        double newVisibleHeight = config.getRadarHeight() / newZoomLevel;
        
        // Farenin altında kalan noktanın yine farenin altında kalmasını sağla
        double newCameraX = logicalMouseX - fx * newVisibleWidth;
        double newCameraY = logicalMouseY - fy * newVisibleHeight;
        
        // Uzaklaşırken kenarlara çarpınca kamerayı dışarı taşırma (Clamping)
        newCameraX = Math.max(0, Math.min(newCameraX, config.getRadarWidth() - newVisibleWidth));
        newCameraY = Math.max(0, Math.min(newCameraY, config.getRadarHeight() - newVisibleHeight));
        
        this.zoomLevel = newZoomLevel;
        this.cameraX = newCameraX;
        this.cameraY = newCameraY;
        
        // Fare koordinatını da hemen güncelle
        this.mouseLogicalX = logicalMouseX;
        this.mouseLogicalY = logicalMouseY;
    }

    public void registerSpacePress() {
        this.pendingSpace = true;
    }
    
    public void registerClearMarksPress() {
        this.pendingClearMarks = true;
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
        this.viewportW = width;
        this.viewportH = height;
        // Projection artık display() içinde zoom'a göre ayarlanıyor
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        long frameStartNanos = System.nanoTime();
        
        GL2 gl = drawable.getGL().getGL2();

        // Delta-time hesabı
        double deltaSec = (frameStartNanos - lastDisplayNanos) / 1_000_000_000.0;
        long frameIntervalNanos = frameStartNanos - lastDisplayNanos;
        lastDisplayNanos = frameStartNanos;

        // Sweep çizgisini ilerlet
        advanceSweep(deltaSec);

        // Arka planı temizle
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // --- ANA ÇİZİM (KAMERA İLE) ---
        gl.glViewport(0, 0, viewportW, viewportH);
        
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        double visibleWidth = config.getRadarWidth() / zoomLevel;
        double visibleHeight = config.getRadarHeight() / zoomLevel;
        gl.glOrtho(cameraX, cameraX + visibleWidth, cameraY, cameraY + visibleHeight, -1.0, 1.0);
        
        gl.glMatrixMode(GL2.GL_MODELVIEW);
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
        
        // --- MİNİMAP ÇİZİMİ ---
        drawMinimap(gl, ctx);

        gl.glFlush();
        
        // Gerçek GPU/Render Kullanımını Bildir
        long frameEndNanos = System.nanoTime();
        long renderTimeNanos = frameEndNanos - frameStartNanos;
        com.radar.metrics.GpuMetricsProvider.reportRenderTime(renderTimeNanos, frameIntervalNanos);
    }
    
    private void drawMinimap(GL2 gl, RenderContext ctx) {
        // Radar mantıksal olarak 1000x1000 ve grid aralığı 250 (4 parça)
        // Sol üstteki tek bir grid karesine tam oturması için viewportW / 4 alıyoruz.
        int minimapSize = viewportW / 4; 
        
        // Sağ üst ya da sol üst, biz SOL ÜST yapacağız.
        // glViewport'ta (0,0) sol alt köşedir. Sol üst için y = viewportH - minimapSize
        gl.glViewport(0, viewportH - minimapSize, minimapSize, minimapSize);
        
        // Minimap projeksiyonu (Sürekli 0-1000'e sabit)
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, config.getRadarWidth(), 0, config.getRadarHeight(), -1.0, 1.0);
        
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        
        // Arka plan: Siyah yerine, radarın kendi arka plan renginin biraz daha koyusu
        gl.glColor4f(config.getBgColorR() * 0.6f, config.getBgColorG() * 0.6f, config.getBgColorB() * 0.6f, 0.95f);
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f(0, 0);
        gl.glVertex2f((float)config.getRadarWidth(), 0);
        gl.glVertex2f((float)config.getRadarWidth(), (float)config.getRadarHeight());
        gl.glVertex2f(0, (float)config.getRadarHeight());
        gl.glEnd();
        
        // Sınır çizgisi: Grid rengiyle aynı olsun ki tam bir kare gibi dursun
        gl.glColor4f(GRID_COLOR[0], GRID_COLOR[1], GRID_COLOR[2], 1.0f);
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(0, 0);
        gl.glVertex2f((float)config.getRadarWidth(), 0);
        gl.glVertex2f((float)config.getRadarWidth(), (float)config.getRadarHeight());
        gl.glVertex2f(0, (float)config.getRadarHeight());
        gl.glEnd();
        gl.glLineWidth(1.0f);
        
        // Varlıkları çiz
        for (ISimulationEntity entity : entityManager.getAll()) {
            entity.render(gl, ctx); // Sweep efektleri dahil olmak üzere
        }
        
        // Kameranın baktığı alanı (kadrajı) çiz (Beyaz Dikdörtgen)
        float cx = (float) cameraX;
        float cy = (float) cameraY;
        float cw = (float) (config.getRadarWidth() / zoomLevel);
        float ch = (float) (config.getRadarHeight() / zoomLevel);
        
        gl.glColor4f(1.0f, 1.0f, 1.0f, 0.9f); // Parlak beyaz
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(cx, cy);
        gl.glVertex2f(cx + cw, cy);
        gl.glVertex2f(cx + cw, cy + ch);
        gl.glVertex2f(cx, cy + ch);
        gl.glEnd();
        gl.glLineWidth(1.0f);
    }

    private void processInteractions() {
        if (pendingClearMarks) {
            pendingClearMarks = false;
            for (ISimulationEntity entity : entityManager.getAll()) {
                if (entity instanceof com.radar.model.Ship) {
                    ((com.radar.model.Ship) entity).setMarked(false);
                }
            }
        }
        
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
