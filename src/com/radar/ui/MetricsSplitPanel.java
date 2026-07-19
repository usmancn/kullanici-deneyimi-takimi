package com.radar.ui;

import javax.swing.JPanel;
import java.awt.GridLayout;

/**
 * CPU ve GPU metrik panellerini yan yana gosteren yardimci panel.
 */
@SuppressWarnings("serial")
final class MetricsSplitPanel extends JPanel {

    MetricsSplitPanel(MetricsPanel cpu, MetricsPanel gpu) {
        setLayout(new GridLayout(1, 2, 4, 0));
        add(cpu);
        add(gpu);
    }
}
