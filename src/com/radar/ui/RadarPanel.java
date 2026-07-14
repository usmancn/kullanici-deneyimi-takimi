package com.radar.ui;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.radar.config.SimulationConfig;
import com.radar.engine.EntityManager;
import com.radar.engine.SimulationEngine;
import com.radar.renderer.RadarRenderer;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * JOGL {@link GLJPanel}'ı barındıran ve animasyonu yöneten Swing paneli.
 *
 * <p>GLJPanel, Swing (lightweight) bileşeni olduğu için Windows'ta AWT (heavyweight)
 * bileşenlerin yaşadığı boyutlandırma (layout) sorunlarını yaşamaz, tam olarak sığar.</p>
 *
 * <p><b>Debug modu:</b> {@link SimulationConfig#isDebugMode()} aktifse
 * GLJPanel'ın sol üst köşesinde FPS ve varlık sayısı overlay'i gösterilir.</p>
 */
public final class RadarPanel extends JPanel {

    /** Render kare hızı hedefi (FPS). Konfigürasyon ile ilgisizdir. */
    private static final int TARGET_FPS = 60;

    /** Debug overlay güncelleme aralığı (ms). */
    private static final int DEBUG_UPDATE_INTERVAL_MS = 500;

    private final GLJPanel       glCanvas;
    private final FPSAnimator    animator;
    private final ControlPanel   controlPanel;
    private final SimulationConfig config;
    private final EntityManager  entityManager;

    // Debug overlay bileşenleri (yalnızca debugMode=true iken aktif)
    private JLabel debugLabel;
    private Timer  debugTimer;

    /**
     * Yeni bir radar paneli oluşturur.
     *
     * @param config        Konfigürasyon; null olamaz.
     * @param entityManager Varlık yöneticisi; null olamaz.
     * @param engine        Simülasyon motoru (kontrol paneline geçmek için); null olamaz.
     */
    public RadarPanel(SimulationConfig config,
                      EntityManager entityManager,
                      SimulationEngine engine) {
        if (config == null || entityManager == null || engine == null) {
            throw new IllegalArgumentException("RadarPanel bagimliliklari null olamaz.");
        }
        this.config        = config;
        this.entityManager = entityManager;

        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(8, 8, 12));

        // JOGL profili ve yetenekler
        GLProfile      profile      = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDoubleBuffered(true);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4);     // MSAA x4

        // GLJPanel oluşturma
        this.glCanvas = new GLJPanel(capabilities);
        Dimension radarSize = new Dimension(config.getRadarWidth(), config.getRadarHeight()); // 1000x1000
        this.glCanvas.setPreferredSize(radarSize); // Mümkünse 1000x1000 açılmaya çalışsın
        // Minimum size'ı küçük tutalım ki Windows DPI ölçeklemesinden dolayı ekran daralırsa panel de küçülebilsin
        this.glCanvas.setMinimumSize(new Dimension(200, 200));
        
        // Klavye olayları için odaklanabilir yapıyoruz
        this.glCanvas.setFocusable(true);

        // Renderer bağlama
        RadarRenderer renderer = new RadarRenderer(config, entityManager);
        glCanvas.addGLEventListener(renderer);

        // Mouse hareket takibi
        glCanvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                if (controlPanel != null) {
                    // ControlPanel'e mantıksal koordinatları göndermemiz lazım.
                    // Bunun için orayı şimdilik -1, -1 yapalım ya da Renderer'dan çekelim.
                    // (Şimdilik renderer kendisi ekranda çizdiği için ControlPanel'i güncellemeye gerek kalmayabilir,
                    // ancak eski kodu bozmamak adına basitçe yolluyoruz)
                }
                renderer.updateMousePositionFromPhysical(e.getX(), e.getY(), glCanvas.getWidth(), glCanvas.getHeight(), true);
            }
        });

        // Tıklama ve odaklanma için MouseListener
        glCanvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                glCanvas.requestFocusInWindow();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                renderer.updateMousePositionFromPhysical(-1, -1, 1, 1, false);
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    renderer.registerClickFromPhysical(e.getX(), e.getY(), glCanvas.getWidth(), glCanvas.getHeight());
                }
            }
        });
        
        // Mouse tekerleği ile Zoom (Kamera)
        glCanvas.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                renderer.doZoom(e.getWheelRotation(), e.getX(), e.getY(), glCanvas.getWidth(), glCanvas.getHeight());
            }
        });

        // Tuş takibi (Space: tekli silme, C veya ESC: tümünü silme)
        glCanvas.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    renderer.registerSpacePress();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_C || e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    renderer.registerClearMarksPress();
                }
            }
        });

        // Animatör
        this.animator = new FPSAnimator(glCanvas, TARGET_FPS, true);

        // Kontrol paneli (sağ kenar)
        this.controlPanel = new ControlPanel(config, engine);
        this.controlPanel.setPreferredSize(new Dimension(220, 0));

        // Radar panelinin ekrana sündürülmesini engellemek için, tam kare kalmasını (1:1 aspect ratio)
        // sağlayan özel bir wrapper panel yazıyoruz. Pencere küçülürse panel de orantılı küçülür.
        JPanel canvasWrapper = new JPanel(null) {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                int size = Math.min(w, h);
                int x = (w - size) / 2;
                int y = (h - size) / 2;
                glCanvas.setBounds(x, y, size, size);
            }
        };
        canvasWrapper.setBackground(new Color(8, 8, 12));
        canvasWrapper.add(glCanvas);

        add(canvasWrapper,    BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);
    }

    // -------------------------------------------------------------------------
    // Animatör Yaşam Döngüsü
    // -------------------------------------------------------------------------

    /**
     * JOGL render döngüsünü ve (varsa) debug timer'ı başlatır.
     */
    public void startRendering() {
        if (!animator.isAnimating()) {
            animator.start();
        }
        if (config.isDebugMode() && debugTimer != null) {
            debugTimer.start();
        }
    }

    /**
     * JOGL render döngüsünü duraklatır.
     * Sekme gizlendiğinde çağrılarak CPU/GPU tasarrufu sağlanır.
     */
    public void pauseRendering() {
        if (animator.isAnimating()) {
            animator.pause();
        }
        if (debugTimer != null) {
            debugTimer.stop();
        }
    }

    /**
     * JOGL render döngüsünü sürdürür.
     * Sekme tekrar görünür olduğunda çağrılır.
     */
    public void resumeRendering() {
        if (animator.isPaused()) {
            animator.resume();
        }
        if (config.isDebugMode() && debugTimer != null) {
            debugTimer.start();
        }
    }

    /**
     * JOGL render döngüsünü tamamen durdurur.
     * Uygulama kapatılırken çağrılmalıdır.
     */
    public void stopRendering() {
        if (animator.isAnimating() || animator.isPaused()) {
            animator.stop();
        }
        if (debugTimer != null) {
            debugTimer.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Debug Overlay
    // -------------------------------------------------------------------------

    /**
     * GLCanvas ve debug label'ı üst üste konumlandırmak için
     * {@link JLayeredPane} kullanan bir panel döndürür.
     */
    private JLayeredPane buildLayeredPane(SimulationConfig cfg) {
        JLayeredPane layered = new JLayeredPane() {
            @Override
            public void doLayout() {
                // Tüm çocukları panelin tüm alanına yay
                for (int i = 0; i < getComponentCount(); i++) {
                    getComponent(i).setBounds(0, 0, getWidth(), getHeight());
                }
            }
        };
        layered.setBackground(new Color(8, 8, 12));
        layered.setOpaque(true);

        // GLCanvas en altta (DEFAULT_LAYER)
        layered.add(glCanvas, JLayeredPane.DEFAULT_LAYER);

        // Debug overlay (PALETTE_LAYER → GLCanvas'ın üstünde)
        if (cfg.isDebugMode()) {
            debugLabel = buildDebugLabel();
            layered.add(debugLabel, JLayeredPane.PALETTE_LAYER);
            startDebugTimer();
        }

        return layered;
    }

    private JLabel buildDebugLabel() {
        JLabel lbl = new JLabel("DEBUG", SwingConstants.LEFT);
        lbl.setForeground(new Color(0, 255, 80));
        lbl.setBackground(new Color(0, 0, 0, 160));
        lbl.setOpaque(true);
        lbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lbl.setVerticalAlignment(SwingConstants.TOP);
        return lbl;
    }

    private void startDebugTimer() {
        debugTimer = new Timer(DEBUG_UPDATE_INTERVAL_MS, e -> updateDebugOverlay());
        debugTimer.setInitialDelay(0);
    }

    /**
     * Debug overlay içeriğini günceller (EDT'de çalışır).
     */
    private void updateDebugOverlay() {
        if (debugLabel == null) {
            return;
        }
        int entityCount = entityManager.getEntityCount();
        double fps      = animator.getLastFPS();
        String text = String.format(
                "<html><pre> [DEBUG]<br>"
                + " FPS      : %.1f<br>"
                + " Varlik   : %d / %d<br>"
                + " Sim Hz   : %d<br>"
                + " Trail    : %d<br>"
                + " Fade     : %.2f</pre></html>",
                fps,
                entityCount,
                config.getMaxShipCount(),
                config.getSimulationHz(),
                config.getTrailLength(),
                config.getFadeFactor()
        );
        debugLabel.setText(text);
    }
}
