package com.radar.ui;

import com.radar.config.SimulationConfig;
import com.radar.metrics.IMetricsProvider;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Bir {@link IMetricsProvider} kaynağının anlık kullanım değerini
 * grafik olarak gösteren Swing paneli.
 *
 * <p>Panel, {@link Timer} (Swing EDT-safe) aracılığıyla belirli
 * aralıklarla metrik değeri okuyarak {@link CircularBuffer}'a ekler
 * ve {@link GraphPanel}'i yeniden çizdirir.</p>
 *
 * <p><b>Thread güvenliği:</b> Timer EDT'de çalışır; tüm Swing bileşen
 * güncellemeleri güvenlidir. Metrik okuma ({@link IMetricsProvider#getCurrentUsage()})
 * da EDT'den yapılır; provider'ın thread-safe olduğu varsayılır.</p>
 */
public final class MetricsPanel extends JPanel {

    private final IMetricsProvider  provider;
    private final SimulationConfig  config;
    private final CircularBuffer    buffer;
    private final GraphPanel        graphPanel;
    private final JLabel            statusLabel;
    private       Timer             updateTimer;

    /**
     * Yeni bir metrik paneli oluşturur.
     *
     * @param provider Metrik kaynağı; null olamaz.
     * @param config   Simülasyon konfigürasyonu (güncelleme Hz için); null olamaz.
     */
    public MetricsPanel(IMetricsProvider provider, SimulationConfig config) {
        if (provider == null) {
            throw new IllegalArgumentException("IMetricsProvider null olamaz.");
        }
        if (config == null) {
            throw new IllegalArgumentException("SimulationConfig null olamaz.");
        }
        this.provider = provider;
        this.config   = config;
        this.buffer   = new CircularBuffer(config.getMetricsHistorySize());

        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(14, 14, 20));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Başlık
        JPanel headerPanel = buildHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Grafik
        this.graphPanel = new GraphPanel(buffer, provider.getLabel());
        add(graphPanel, BorderLayout.CENTER);

        // Durum etiketi (alt)
        this.statusLabel = new JLabel("Baglaniyor...");
        this.statusLabel.setForeground(new Color(140, 140, 160));
        this.statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11.0f));
        add(statusLabel, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Yaşam Döngüsü
    // -------------------------------------------------------------------------

    /**
     * Periyodik metrik okumayı başlatır.
     * Panel görünür hale geldiğinde çağrılmalıdır.
     */
    public void startUpdating() {
        if (updateTimer != null && updateTimer.isRunning()) {
            return;
        }
        int delayMs = 1000 / config.getMetricsUpdateHz();
        updateTimer = new Timer(delayMs, e -> fetchAndUpdate());
        updateTimer.setInitialDelay(0);
        updateTimer.start();
    }

    /**
     * Periyodik metrik okumayı durdurur.
     * Panel gizlendiğinde veya kapatıldığında çağrılmalıdır.
     */
    public void stopUpdating() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Güncelleme
    // -------------------------------------------------------------------------

    /**
     * Metrik kaynağından değer okur ve grafik panelini günceller.
     * Yalnızca EDT üzerinde çalışır (Timer callback).
     */
    private void fetchAndUpdate() {
        double value = provider.getCurrentUsage();
        buffer.add(value);
        graphPanel.repaint();

        if (value < 0) {
            statusLabel.setText(provider.getLabel() + ": N/A");
        } else {
            statusLabel.setText(provider.getLabel() + ": " + String.format("%.1f%%", value));
        }
    }

    // -------------------------------------------------------------------------
    // Yardımcı
    // -------------------------------------------------------------------------

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(new Color(14, 14, 20));

        JLabel title = new JLabel(provider.getLabel());
        title.setForeground(new Color(230, 230, 245));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16.0f));
        panel.add(title);

        return panel;
    }
}
