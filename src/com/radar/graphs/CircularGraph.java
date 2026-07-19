package com.radar.graphs;

import com.radar.config.SimulationConfig;
import com.radar.sim.engine.EntityManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Dairesel (Circular) grafik.
 *
 * <p><b>TODO:</b> Bu iskelet sinifa Circular grafigini implemente et.
 * {@link IGraph} arayuzunu zaten implement ediyor; {@link #startGraph()} ve
 * {@link #stopGraph()} metodlarini istege gore genisletebilirsin.</p>
 *
 * <p>Varliklara {@code entityManager.getAll()} ile ulasabilirsin.</p>
 */
@SuppressWarnings("serial")
public class CircularGraph extends JPanel implements IGraph {

    /** Simülasyon konfigurasyonu; grafik parametreleri buradan alinabilir. */
    protected final SimulationConfig config;

    /** Varlik yoneticisi; hedeflerin konumlari buradan alinir. */
    protected final EntityManager entityManager;

    /**
     * @param config        Konfigürasyon; null olamaz.
     * @param entityManager Varlik yoneticisi; null olamaz.
     */
    public CircularGraph(SimulationConfig config, EntityManager entityManager) {
        this.config        = config;
        this.entityManager = entityManager;
        buildPlaceholderUI();
    }

    @Override
    public void startGraph() {
        // TODO: animasyonu / timer'i baslatiniz
    }

    @Override
    public void stopGraph() {
        // TODO: animasyonu / timer'i durdurunuz
    }

    // ------------------------------------------------------------------ //
    // İskelet arayüzü (geliştirme tamamlanınca kaldırılacak)
    // ------------------------------------------------------------------ //

    private void buildPlaceholderUI() {
        setBackground(new Color(10, 14, 12));
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Circular Graph — Yakında", SwingConstants.CENTER);
        label.setForeground(new Color(80, 255, 160));
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(label, BorderLayout.CENTER);
    }
}
