package com.radar.ui;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
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
 * JOGL {@link GLCanvas}'ı barındıran ve animasyonu yöneten Swing paneli.
 *
 * <p>Bu panel {@link MainFrame}'deki "Radar" sekmesine yerleştirilir.
 * GLCanvas (AWT bileşeni) lightweight Swing bileşenlerinin üstünde render
 * olduğundan, sekme değiştirme sırasında animator durdurulup başlatılır
 * (z-order çakışmasını önlemek için).</p>
 *
 * <p><b>Debug modu:</b> {@link SimulationConfig#isDebugMode()} aktifse
 * GLCanvas'ın sol üst köşesinde FPS ve varlık sayısı overlay'i gösterilir.</p>
 */
public final class RadarPanel extends JPanel {

    /** Render kare hızı hedefi (FPS). Konfigürasyon ile ilgisizdir. */
    private static final int TARGET_FPS = 60;

    /** Debug overlay güncelleme aralığı (ms). */
    private static final int DEBUG_UPDATE_INTERVAL_MS = 500;

    private final GLCanvas       glCanvas;
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

        // GLCanvas oluşturma — preferred size SABIT değil, layout manager doldurur
        this.glCanvas = new GLCanvas(capabilities);
        // Minimum kullanılabilir boyut; daha büyük pencerede tam dolar
        this.glCanvas.setMinimumSize(new Dimension(400, 400));

        // Renderer bağlama
        RadarRenderer renderer = new RadarRenderer(config, entityManager);
        glCanvas.addGLEventListener(renderer);

        // Animatör
        this.animator = new FPSAnimator(glCanvas, TARGET_FPS, true);

        // Kontrol paneli (sağ kenar)
        this.controlPanel = new ControlPanel(config, engine);
        this.controlPanel.setPreferredSize(new Dimension(220, 0));

        // GLCanvas + debug overlay'i JLayeredPane içine al
        JLayeredPane layeredPane = buildLayeredPane(config);

        add(layeredPane,  BorderLayout.CENTER);
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
