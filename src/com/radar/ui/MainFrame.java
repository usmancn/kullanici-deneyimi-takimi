package com.radar.ui;

import com.radar.config.SimulationConfig;
import com.radar.sim.engine.EntityManager;
import com.radar.sim.engine.SimulationEngine;
import com.radar.factory.GraphFactory;
import com.radar.factory.GraphFactory.GraphType;
import com.radar.graphs.IGraph;
import com.radar.metrics.CpuMetricsProvider;
import com.radar.metrics.GpuMetricsProvider;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Ana taşıyıcı arayüz penceresi.
 */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    private static final String TAB_RADAR     = "Radar";
    private static final String TAB_WATERFALL = "Waterfall";
    private static final String TAB_CIRCULAR  = "Circular";
    private static final String TAB_METRICS   = "Sistem Metrikleri";

    private final SimulationEngine engine;

    // Her sekmenin bileşenine IGraph arayüzüyle erişiyoruz
    private final IGraph radarGraph;
    private final IGraph waterfallGraph;
    private final IGraph circularGraph;

    // Metrik panelleri (IGraph değil; kendi API'leri var)
    private final MetricsPanel cpuPanel;
    private final MetricsPanel gpuPanel;

    private final JTabbedPane tabbedPane;

    /**
     * @param config        Konfigürasyon; null olamaz.
     * @param entityManager Varlık yöneticisi; null olamaz.
     * @param engine        Simülasyon motoru; null olamaz.
     * @param frequency     Radar render FPS hedefi.
     * @param initialGraph  Açılışta odaklanılacak sekme.
     */
    public MainFrame(SimulationConfig config,
                     EntityManager entityManager,
                     SimulationEngine engine,
                     int frequency,
                     GraphType initialGraph) {

        super("Radar Simülasyonu");
        this.engine = engine;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { shutdown(); }
        });

        setMinimumSize(new Dimension(900, 700));
        getContentPane().setBackground(new Color(10, 10, 16));

        // --- Grafik bileşenlerini GraphFactory'den üret ---
        Component radarComp     = GraphFactory.create(GraphType.RADAR,     config, entityManager, frequency);
        Component waterfallComp = GraphFactory.create(GraphType.WATERFALL, config, entityManager, frequency);
        Component circularComp  = GraphFactory.create(GraphType.CIRCULAR,  config, entityManager, frequency);

        this.radarGraph     = (IGraph) radarComp;
        this.waterfallGraph = (IGraph) waterfallComp;
        this.circularGraph  = (IGraph) circularComp;

        // --- Metrik panelleri ---
        this.cpuPanel = new MetricsPanel(new CpuMetricsProvider(), config);
        this.gpuPanel = new MetricsPanel(new GpuMetricsProvider(), config);

        // --- Sekmeli yapı ---
        this.tabbedPane = buildTabbedPane(
                config, radarComp, waterfallComp, circularComp, initialGraph);
        getContentPane().add(tabbedPane);

        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ //
    // Yaşam Döngüsü
    // ------------------------------------------------------------------ //

    /**
     * Tüm grafiklerin ve metrik panellerinin animasyonunu başlatır.
     * Pencere görünür hale getirilmeden önce çağrılmalıdır.
     */
    public void startAll() {
        // Waterfall ve Circular başlangıçta duraksatılır;
        // İlk seçili olan grafiğin animasyonunu başlat
        int sel = tabbedPane.getSelectedIndex();
        if (sel == 0) radarGraph.startGraph();
        else if (sel == 1) waterfallGraph.startGraph();
        else if (sel == 2) circularGraph.startGraph();
        else if (sel == 3) {
            // Metrikler sekmesi ile basliyorsak arka planda radar'i calistiralim
            radarGraph.startGraph();
            cpuPanel.startUpdating();
            gpuPanel.startUpdating();
        }
    }

    private void shutdown() {
        radarGraph.stopGraph();
        waterfallGraph.stopGraph();
        circularGraph.stopGraph();
        cpuPanel.stopUpdating();
        gpuPanel.stopUpdating();
        engine.stop();
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------ //
    // Sekme Yapısı
    // ------------------------------------------------------------------ //

    private JTabbedPane buildTabbedPane(SimulationConfig config,
                                        Component radarComp,
                                        Component waterfallComp,
                                        Component circularComp,
                                        GraphType initialGraph) {

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setBackground(new Color(10, 10, 16));
        tabs.setForeground(new Color(200, 200, 215));
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 13.0f));

        // Metrik paneli: CPU + GPU yan yana
        MetricsSplitPanel metricsPanel = new MetricsSplitPanel(cpuPanel, gpuPanel);

        // AWT GLCanvas bileşeninin Swing içinde düzgün boyutlanması ve her zaman KARE kalması için özel JPanel
        javax.swing.JPanel radarWrapper = new javax.swing.JPanel() {
            @Override
            public void doLayout() {
                if (getComponentCount() > 0) {
                    int size = Math.min(getWidth(), getHeight());
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;
                    getComponent(0).setBounds(x, y, size, size);
                }
            }
        };
        radarWrapper.setLayout(null);
        radarWrapper.setBackground(new Color(10, 10, 16));
        radarWrapper.add(radarComp);

        // Kare radar: canvas ortada, gain filtresi altta
        javax.swing.JPanel radarTab = new javax.swing.JPanel(new java.awt.BorderLayout());
        radarTab.setBackground(new Color(10, 10, 16));
        radarTab.add(radarWrapper, java.awt.BorderLayout.CENTER);
        radarTab.add(new GainFilterPanel(config), java.awt.BorderLayout.SOUTH);

        // Yuvarlak radar: canvas ortada, gain filtresi altta
        javax.swing.JPanel circularTab = new javax.swing.JPanel(new java.awt.BorderLayout());
        circularTab.setBackground(new Color(10, 10, 16));
        circularTab.add(circularComp, java.awt.BorderLayout.CENTER);
        circularTab.add(new GainFilterPanel(config), java.awt.BorderLayout.SOUTH);

        tabs.addTab(TAB_RADAR,     radarTab);
        tabs.addTab(TAB_WATERFALL, waterfallComp);
        tabs.addTab(TAB_CIRCULAR,  circularTab);
        tabs.addTab(TAB_METRICS,   metricsPanel);

        // Başlangıç sekmesini ayarla
        int startIndex = graphTypeToIndex(initialGraph);
        if (startIndex >= 0) tabs.setSelectedIndex(startIndex);

        // Tab değişiminde animasyon yönetimi (CPU/GPU tasarrufu)
        tabs.addChangeListener(new ChangeListener() {
            private int previous = tabs.getSelectedIndex();
            private final IGraph[] graphs = { radarGraph, waterfallGraph, circularGraph, null };
            private int activeGraph = (previous == 3) ? 0 : previous; // Metriklerde baslarsa arka planda radar varsayalim

            @Override
            public void stateChanged(ChangeEvent e) {
                int selected = tabs.getSelectedIndex();

                if (selected != 3) {
                    // Bir grafiğe geçiş yapıldı
                    if (activeGraph != selected) {
                        if (activeGraph >= 0 && activeGraph < graphs.length && graphs[activeGraph] != null) {
                            graphs[activeGraph].stopGraph();
                        }
                        if (selected >= 0 && selected < graphs.length && graphs[selected] != null) {
                            graphs[selected].startGraph();
                        }
                        activeGraph = selected;
                    }
                    cpuPanel.stopUpdating();
                    gpuPanel.stopUpdating();
                } else {
                    // Metrikler sekmesine geçildi. 
                    // Aktif grafiği (ör. Radar) DURDURMUYORUZ ki canlı metrik akışı devam etsin.
                    cpuPanel.startUpdating();
                    gpuPanel.startUpdating();
                }

                previous = selected;
            }
        });

        return tabs;
    }

    private static int graphTypeToIndex(GraphType type) {
        switch (type) {
            case WATERFALL: return 1;
            case CIRCULAR:  return 2;
            default:        return 0;
        }
    }
}
