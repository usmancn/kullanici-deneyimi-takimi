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
 * Ana pencere. Sekmeli yapıda (JTabbedPane) grafik panellerini barındırır.
 *
 * <p>Sekmeler {@link GraphFactory} üzerinden üretilir; yeni bir sekme
 * eklemek için yalnızca {@code buildTabbedPane()} metoduna bir
 * {@link GraphFactory#create} çağrısı eklemek yeterlidir.</p>
 *
 * <p>Tab değişiminde yalnızca görünen sekmenin animasyonu aktif tutulur
 * (CPU / GPU tasarrufu).</p>
 */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    private static final String TAB_RADAR     = "Radar (Osman)";
    private static final String TAB_WATERFALL = "Waterfall (Fatih)";
    private static final String TAB_CIRCULAR  = "Circular (Altay)";
    private static final String TAB_METRICS   = "Sistem Metrikleri";

    private final SimulationEngine engine;

    // Her sekmenin bileşenine IGraph arayüzüyle erişiyoruz
    private final IGraph radarGraph;
    private final IGraph waterfallGraph;
    private final IGraph circularGraph;

    // Metrik panelleri (IGraph değil; kendi API'leri var)
    private final MetricsPanel cpuPanel;
    private final MetricsPanel gpuPanel;

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
        JTabbedPane tabbedPane = buildTabbedPane(
                radarComp, waterfallComp, circularComp, initialGraph);
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
        radarGraph.startGraph();
        cpuPanel.startUpdating();
        gpuPanel.startUpdating();
        // Waterfall ve Circular başlangıçta duraksatılır;
        // sekmeye gelindiğinde ChangeListener devreye alır.
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

    private JTabbedPane buildTabbedPane(Component radarComp,
                                        Component waterfallComp,
                                        Component circularComp,
                                        GraphType initialGraph) {

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setBackground(new Color(10, 10, 16));
        tabs.setForeground(new Color(200, 200, 215));
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 13.0f));

        // Metrik paneli: CPU + GPU yan yana
        MetricsSplitPanel metricsPanel = new MetricsSplitPanel(cpuPanel, gpuPanel);

        // AWT GLCanvas bileşeninin Swing içinde düzgün boyutlanması için JPanel (BorderLayout) ile sarıyoruz
        javax.swing.JPanel radarWrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        radarWrapper.add(radarComp, java.awt.BorderLayout.CENTER);

        tabs.addTab(TAB_RADAR,     radarWrapper);
        tabs.addTab(TAB_WATERFALL, waterfallComp);
        tabs.addTab(TAB_CIRCULAR,  circularComp);
        tabs.addTab(TAB_METRICS,   metricsPanel);

        // Başlangıç sekmesini ayarla
        int startIndex = graphTypeToIndex(initialGraph);
        if (startIndex >= 0) tabs.setSelectedIndex(startIndex);

        // Tab değişiminde animasyon yönetimi (CPU/GPU tasarrufu)
        tabs.addChangeListener(new ChangeListener() {
            private int previous = tabs.getSelectedIndex();
            private final IGraph[] graphs = { radarGraph, waterfallGraph, circularGraph, null };

            @Override
            public void stateChanged(ChangeEvent e) {
                int selected = tabs.getSelectedIndex();

                // Önceki sekmenin grafigini durdur
                if (previous >= 0 && previous < graphs.length && graphs[previous] != null) {
                    graphs[previous].stopGraph();
                }
                // Yeni sekmenin grafigini başlat
                if (selected >= 0 && selected < graphs.length && graphs[selected] != null) {
                    graphs[selected].startGraph();
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
