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
 * Cizgi (Line) grafik.
 *
 * <p><b>TODO (Ortak):</b> Bu iskelet sinifa Line grafigini implemente et.
 * {@link IGraph} arayuzunu zaten implement ediyor; {@link #startGraph()} ve
 * {@link #stopGraph()} metodlarini istege gore genisletebilirsin.</p>
 *
 * <p>Varliklara {@code entityManager.getAll()} ile ulasabilirsin.</p>
 */
@SuppressWarnings("serial")
public class LineGraph extends JPanel implements IGraph {

    /** Simülasyon konfigurasyonu; grafik parametreleri buradan alinabilir. */
    protected final SimulationConfig config;

    /** Osman'in varlik yoneticisi; hedeflerin konumlari buradan alinir. */
    protected final EntityManager entityManager;

    /**
     * @param config        Konfigürasyon; null olamaz.
     * @param entityManager Varlik yoneticisi; null olamaz.
     */
    public LineGraph(SimulationConfig config, EntityManager entityManager) {
        this.config        = config;
        this.entityManager = entityManager;
        buildPlaceholderUI();
    }

    @Override
    public void startGraph() {
        // TODO: Line graph animasyonunu baslatiniz
    }

    @Override
    public void stopGraph() {
        // TODO: Line graph animasyonunu durdurunuz
    }

    // ------------------------------------------------------------------ //
    // İskelet arayüzü (geliştirme tamamlanınca kaldırılacak)
    // ------------------------------------------------------------------ //

    private void buildPlaceholderUI() {
        setBackground(new Color(12, 10, 18));
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Line Graph — Yakında", SwingConstants.CENTER);
        label.setForeground(new Color(200, 160, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(label, BorderLayout.CENTER);
    }
}
