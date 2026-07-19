package com.radar.ui;

import com.radar.config.SimulationConfig;
import com.radar.engine.SimulationEngine;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Kullanıcının simülasyon parametrelerini çalışma zamanında değiştirebildiği
 * kontrol paneli.
 *
 * <p>Tüm Swing bileşenleri ve listener'lar EDT üzerinde çalışır.
 * Konfigürasyon değişiklikleri anında {@link SimulationConfig} singleton'una
 * yansıtılır; motor bir sonraki tick'te güncel değerleri okur.</p>
 */
public final class ControlPanel extends JPanel {

    private static final int MIN_SHIP_COUNT   = 1;
    private static final int MAX_SHIP_COUNT   = 100;
    private static final int MIN_SIM_HZ       = 1;
    private static final int MAX_SIM_HZ       = 120;
    private static final int OPACITY_SLIDER_MAX = 100;
    private static final float MIN_FADE       = 0.50f;
    private static final float MAX_FADE       = 0.99f;
    private static final int FADE_SLIDER_MAX  = 100;

    private final SimulationConfig  config;
    private final SimulationEngine  engine;

    private JLabel mouseCoordsLabel;
    private JLabel engineStatusLabel;

    /**
     * Yeni bir kontrol paneli oluşturur.
     *
     * @param config Konfigürasyon; null olamaz.
     * @param engine Motor (durum göstergesi için); null olamaz.
     */
    public ControlPanel(SimulationConfig config, SimulationEngine engine) {
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        if (engine == null) {
            throw new IllegalArgumentException("SimulationEngine null olamaz.");
        }
        this.config = config;
        this.engine = engine;

        setBackground(new Color(14, 14, 20));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new GridLayout(0, 1, 4, 4));

        add(buildSectionTitle("Simülasyon Parametreleri"));
        add(buildShipCountSpinner());
        add(buildSimHzSpinner());
        add(buildFadeFactorSlider());
        add(buildMinOpacitySlider());
        add(buildDebugModeCheckbox());

        // Motor durum göstergesi
        add(buildSectionTitle("Motor Durumu"));
        engineStatusLabel = new JLabel("", SwingConstants.LEFT);
        engineStatusLabel.setFont(engineStatusLabel.getFont().deriveFont(Font.BOLD, 12.0f));
        add(engineStatusLabel);
        updateEngineStatusLabel();

        // 500ms'de bir durumu yenile
        new Timer(500, e -> updateEngineStatusLabel()).start();

        // Mouse koordinatları için alan
        add(buildSectionTitle("İmleç Konumu"));
        mouseCoordsLabel = new JLabel("X: 0, Y: 0", SwingConstants.LEFT);
        mouseCoordsLabel.setForeground(new Color(200, 200, 215));
        mouseCoordsLabel.setFont(mouseCoordsLabel.getFont().deriveFont(Font.BOLD, 12.0f));
        add(mouseCoordsLabel);
    }

    private void updateEngineStatusLabel() {
        if (engine.isRunning()) {
            engineStatusLabel.setText("\u25CF Çalışıyor");
            engineStatusLabel.setForeground(new Color(0, 220, 80));
        } else {
            engineStatusLabel.setText("\u25CF Durdu");
            engineStatusLabel.setForeground(new Color(220, 60, 60));
        }
    }

    /**
     * Dışarıdan fare koordinatlarını güncellemek için kullanılır.
     */
    public void updateMouseCoords(int x, int y) {
        if (mouseCoordsLabel != null) {
            mouseCoordsLabel.setText(String.format("X: %d, Y: %d", x, y));
        }
    }

    // -------------------------------------------------------------------------
    // Bileşen Oluşturucular
    // -------------------------------------------------------------------------

    private JLabel buildSectionTitle(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(new Color(200, 60, 60));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13.0f));
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 30, 30)));
        return label;
    }

    private JPanel buildShipCountSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                config.getMaxShipCount(), MIN_SHIP_COUNT, MAX_SHIP_COUNT, 1
        ));
        styleSpinner(spinner);
        spinner.addChangeListener(e -> {
            int val = (Integer) spinner.getValue();
            config.setMaxShipCount(val);
        });
        return buildLabeledRow("Maks. Gemi Sayısı:", spinner);
    }

    private JPanel buildSimHzSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                config.getSimulationHz(), MIN_SIM_HZ, MAX_SIM_HZ, 1
        ));
        styleSpinner(spinner);
        spinner.addChangeListener(e -> {
            int val = (Integer) spinner.getValue();
            config.setSimulationHz(val);
        });
        return buildLabeledRow("Simülasyon Hz:", spinner);
    }

    private JPanel buildFadeFactorSlider() {
        // fadeFactor [0.50, 0.99] → slider [50, 99]
        int initialValue = (int) (config.getFadeFactor() * FADE_SLIDER_MAX);
        JSlider slider = new JSlider(
                (int) (MIN_FADE * FADE_SLIDER_MAX),
                (int) (MAX_FADE * FADE_SLIDER_MAX),
                initialValue
        );
        slider.setBackground(new Color(14, 14, 20));
        slider.setForeground(new Color(200, 60, 60));

        JLabel valueLabel = new JLabel(String.format("%.2f", config.getFadeFactor()));
        valueLabel.setForeground(new Color(200, 200, 215));
        valueLabel.setFont(valueLabel.getFont().deriveFont(11.0f));

        slider.addChangeListener(e -> {
            float val = slider.getValue() / (float) FADE_SLIDER_MAX;
            config.setFadeFactor(val);
            valueLabel.setText(String.format("%.2f", val));
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(new Color(14, 14, 20));
        JLabel lbl = new JLabel("Sönükleme Mesafesi:");
        lbl.setForeground(new Color(180, 180, 195));
        lbl.setFont(lbl.getFont().deriveFont(11.0f));
        row.add(lbl);
        row.add(slider);
        row.add(valueLabel);
        return row;
    }

    private JPanel buildMinOpacitySlider() {
        // minShipOpacity [0.0, 0.5] → slider [0, 50]
        int initialValue = (int) (config.getMinShipOpacity() * OPACITY_SLIDER_MAX);
        JSlider slider = new JSlider(0, 50, initialValue);
        slider.setBackground(new Color(14, 14, 20));
        slider.setForeground(new Color(200, 60, 60));

        JLabel valueLabel = new JLabel(String.format("%.2f", config.getMinShipOpacity()));
        valueLabel.setForeground(new Color(200, 200, 215));
        valueLabel.setFont(valueLabel.getFont().deriveFont(11.0f));

        slider.addChangeListener(e -> {
            float val = slider.getValue() / (float) OPACITY_SLIDER_MAX;
            config.setMinShipOpacity(val);
            valueLabel.setText(String.format("%.2f", val));
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(new Color(14, 14, 20));
        JLabel lbl = new JLabel("Min. Opaklık:");
        lbl.setForeground(new Color(180, 180, 195));
        lbl.setFont(lbl.getFont().deriveFont(11.0f));
        row.add(lbl);
        row.add(slider);
        row.add(valueLabel);
        return row;
    }

    private JPanel buildDebugModeCheckbox() {
        JCheckBox checkBox = new JCheckBox("Debug Overlay", config.isDebugMode());
        checkBox.setBackground(new Color(14, 14, 20));
        checkBox.setForeground(new Color(0, 220, 80));
        checkBox.setFont(checkBox.getFont().deriveFont(Font.BOLD, 11.0f));
        checkBox.addActionListener(e -> config.setDebugMode(checkBox.isSelected()));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(new Color(14, 14, 20));
        row.add(checkBox);
        return row;
    }

    // -------------------------------------------------------------------------
    // Stil Yardımcıları
    // -------------------------------------------------------------------------

    private JPanel buildLabeledRow(String labelText, JSpinner spinner) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(new Color(14, 14, 20));

        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(new Color(180, 180, 195));
        lbl.setFont(lbl.getFont().deriveFont(11.0f));
        lbl.setPreferredSize(new Dimension(150, 22));

        row.add(lbl);
        row.add(spinner);
        return row;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(new Color(28, 28, 38));
        spinner.setForeground(new Color(220, 220, 235));
        spinner.setFont(spinner.getFont().deriveFont(12.0f));
        spinner.setPreferredSize(new Dimension(80, 24));
    }
}
