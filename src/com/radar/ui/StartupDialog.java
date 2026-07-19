package com.radar.ui;

import com.radar.factory.GraphFactory.GraphType;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class StartupDialog extends JDialog {

    private boolean startSimulation = false;
    private int shipCount = 50;
    private int fps = 60;
    private double radarSpeed = 80.0;
    private GraphType selectedGraph = GraphType.RADAR;

    public StartupDialog() {
        setTitle("Radar Simülasyonu - Başlangıç Ayarları");
        setModal(true);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblShipCount = new JLabel("Maksimum Gemi Sayısı:");
        JSpinner spShipCount = new JSpinner(new SpinnerNumberModel(50, 1, 10000, 10));

        JLabel lblFps = new JLabel("Yenileme Hızı (FPS):");
        JSpinner spFps = new JSpinner(new SpinnerNumberModel(60, 10, 240, 10));

        JLabel lblRadarSpeed = new JLabel("Radar Hızı (px/s):");
        JSpinner spRadarSpeed = new JSpinner(new SpinnerNumberModel(80.0, 10.0, 500.0, 10.0));

        JLabel lblGraph = new JLabel("Başlangıç Ekranı:");
        JComboBox<GraphType> cbGraph = new JComboBox<>(GraphType.values());

        panel.add(lblShipCount);
        panel.add(spShipCount);
        panel.add(lblFps);
        panel.add(spFps);
        panel.add(lblRadarSpeed);
        panel.add(spRadarSpeed);
        panel.add(lblGraph);
        panel.add(cbGraph);

        JButton btnStart = new JButton("Simülasyonu Başlat");
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnStart.setBackground(new Color(40, 160, 80));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);
        // Windows temasinda arkaplan renginin gorunmesi icin gereklidir:
        btnStart.setOpaque(true);
        btnStart.setBorderPainted(false);

        btnStart.addActionListener(e -> {
            this.shipCount = (int) spShipCount.getValue();
            this.fps = (int) spFps.getValue();
            this.radarSpeed = (double) spRadarSpeed.getValue();
            this.selectedGraph = (GraphType) cbGraph.getSelectedItem();
            this.startSimulation = true;
            dispose();
        });

        panel.add(new JLabel("")); // Bosluk
        panel.add(btnStart);

        add(panel);
    }

    public boolean isStartSimulation() { return startSimulation; }
    public int getShipCount() { return shipCount; }
    public int getFps() { return fps; }
    public double getRadarSpeed() { return radarSpeed; }
    public GraphType getSelectedGraph() { return selectedGraph; }
}
