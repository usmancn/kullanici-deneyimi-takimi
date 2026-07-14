package com.radar.ui;

import com.radar.config.SimulationConfig;
import com.radar.engine.EntityManager;
import com.radar.engine.SimulationEngine;
import com.radar.metrics.CpuMetricsProvider;
import com.radar.metrics.GpuMetricsProvider;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Uygulamanın ana penceresi.
 *
 * <p>Üç sekme içerir:
 * <ol>
 *   <li><b>Radar</b>: JOGL tabanlı gemi radar simülasyonu + kontrol paneli.</li>
 *   <li><b>CPU</b>: Anlık CPU kullanım grafiği.</li>
 *   <li><b>GPU</b>: Anlık GPU kullanım grafiği (simüle edilmiş).</li>
 * </ol>
 * </p>
 *
 * <p><b>Thread güvenliği:</b> Tüm Swing bileşeni kurulumu EDT'de yapılmalıdır.
 * Bu sınıf yalnızca {@code SwingUtilities.invokeLater()} içinden
 * örneklenmelidir.</p>
 *
 * <p><b>Sekme değişimi:</b> Radar sekmesinden ayrılırken animator durdurulur,
 * geri dönünce yeniden başlatılır. Metrik panelleri yalnızca aktif sekmeyken
 * güncellenir.</p>
 */
public final class MainFrame extends JFrame {

    private static final String TAB_RADAR = "Radar";
    private static final String TAB_CPU   = "CPU";
    private static final String TAB_GPU   = "GPU";
    private static final String TAB_MARKED = "İşaretli Gemiler";


    private final RadarPanel        radarPanel;
    private final MetricsPanel      cpuPanel;
    private final MetricsPanel      gpuPanel;
    private final MarkedShipsPanel  markedShipsPanel;
    private final SimulationEngine  engine;

    /**
     * Yeni bir ana pencere oluşturur ve tüm bileşenleri bağlar.
     *
     * @param config        Konfigürasyon; null olamaz.
     * @param entityManager Varlık yöneticisi; null olamaz.
     * @param engine        Simülasyon motoru; null olamaz.
     */
    public MainFrame(SimulationConfig config,
                     EntityManager entityManager,
                     SimulationEngine engine) {
        super("Gemi Radar Simülasyonu");

        if (config == null || entityManager == null || engine == null) {
            throw new IllegalArgumentException("MainFrame bagimliliklari null olamaz.");
        }
        this.engine = engine;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        getContentPane().setBackground(new Color(10, 10, 16));

        // Panelleri oluştur
        this.radarPanel = new RadarPanel(config, entityManager, engine);
        this.cpuPanel   = new MetricsPanel(new CpuMetricsProvider(), config);
        this.gpuPanel   = new MetricsPanel(new GpuMetricsProvider(), config);
        this.markedShipsPanel = new MarkedShipsPanel(entityManager);

        // Sekmeli yapı
        JTabbedPane tabbedPane = buildTabbedPane();
        getContentPane().add(tabbedPane);

        // Pencere kapanışı
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        pack();
        setLocationRelativeTo(null); // Ekran ortasına konumlandır
    }

    // -------------------------------------------------------------------------
    // Başlatma ve Kapatma
    // -------------------------------------------------------------------------

    /**
     * Render ve metrik güncellemelerini başlatır.
     * Pencere görünür hale getirilmeden önce çağrılmalıdır.
     */
    public void startAll() {
        radarPanel.startRendering();
        cpuPanel.startUpdating();
        gpuPanel.startUpdating();
    }

    /**
     * Tüm bileşenleri temiz biçimde kapatır ve uygulamayı sonlandırır.
     */
    private void shutdown() {
        radarPanel.stopRendering();
        cpuPanel.stopUpdating();
        gpuPanel.stopUpdating();
        engine.stop();
        dispose();
        System.exit(0);
    }

    // -------------------------------------------------------------------------
    // Sekme Yapısı
    // -------------------------------------------------------------------------

    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(new Color(10, 10, 16));
        tabbedPane.setForeground(new Color(200, 200, 215));
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 13.0f));

        tabbedPane.addTab(TAB_RADAR, radarPanel);
        tabbedPane.addTab(TAB_CPU,   cpuPanel);
        tabbedPane.addTab(TAB_GPU,   gpuPanel);
        tabbedPane.addTab(TAB_MARKED, markedShipsPanel);

        // Sekme değişim yönetimi
        tabbedPane.addChangeListener(new ChangeListener() {
            private int previousTab = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedTab = tabbedPane.getSelectedIndex();

                // Önceki sekme Radar ise animasyonu durdur
                if (previousTab == 0) {
                    radarPanel.pauseRendering();
                }
                // Radar sekmesine geçildiyse animasyonu başlat
                if (selectedTab == 0) {
                    radarPanel.resumeRendering();
                }

                previousTab = selectedTab;
            }
        });

        return tabbedPane;
    }
}
