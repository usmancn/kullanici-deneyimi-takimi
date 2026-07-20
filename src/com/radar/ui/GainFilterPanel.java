package com.radar.ui;

import com.radar.config.SimulationConfig;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Radar sekmelerine yerlestirilen gain filtresi kontrol paneli.
 *
 * <p>{@link GainRangeSlider} ile [low, high] araligini secer ve degerleri
 * {@link SimulationConfig} uzerindeki gainFilterMin/Max alanlarina yazar.
 * Render thread her karede bu degerleri okuyarak aralik disindaki gemileri gizler.
 *
 * <p>Ayni config'i paylasan birden fazla panel (kare + yuvarlak radar) acik
 * oldugunda, biri degistiginde digerleri de senkron kalir (statik kayit listesi).
 */
public final class GainFilterPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;

    /** Senkronizasyon icin acik tum paneller. */
    private static final CopyOnWriteArrayList<GainFilterPanel> INSTANCES = new CopyOnWriteArrayList<>();

    private final SimulationConfig config;
    private final GainRangeSlider slider;
    private final JLabel valueLabel;

    public GainFilterPanel(SimulationConfig config) {
        this.config = config;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(14, 14, 20));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        int low  = toSlider(config.getGainFilterMin());
        int high = toSlider(config.getGainFilterMax());

        JLabel title = new JLabel("Gain Filtresi");
        title.setForeground(new Color(180, 190, 215));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.valueLabel = new JLabel();
        valueLabel.setForeground(new Color(120, 190, 255));
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.slider = new GainRangeSlider(SLIDER_MIN, SLIDER_MAX, low, high);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                applyToConfig();
                syncSiblings();
                updateLabel();
            }
        });

        add(title);
        add(valueLabel);
        add(slider);

        updateLabel();
        INSTANCES.add(this);
    }

    private void applyToConfig() {
        config.setGainFilterMin(slider.getLowValue()  / 100f);
        config.setGainFilterMax(slider.getHighValue() / 100f);
    }

    private void syncSiblings() {
        for (GainFilterPanel other : INSTANCES) {
            if (other == this) continue;
            other.slider.setRangeSilently(slider.getLowValue(), slider.getHighValue());
            other.updateLabel();
        }
    }

    private void updateLabel() {
        valueLabel.setText(String.format(
                "Gorunen aralik: %.2f  -  %.2f", slider.getLowValue() / 100f, slider.getHighValue() / 100f));
    }

    private static int toSlider(float gain) {
        int v = Math.round(gain * 100f);
        if (v < SLIDER_MIN) return SLIDER_MIN;
        if (v > SLIDER_MAX) return SLIDER_MAX;
        return v;
    }
}
