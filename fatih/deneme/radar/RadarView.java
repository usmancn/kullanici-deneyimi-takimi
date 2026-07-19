package deneme.radar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import deneme.sim.GainFilter;
import deneme.sim.Simulation;

/**
 * Radar tuvali ve gain araligi filtresi.
 *
 * Yalnizca minimum ve maksimum gain degerleri arasinda kalan
 * gemiler radar uzerinde gosterilir.
 */
public class RadarView extends JPanel {

    private final RadarCanvas canvas;
    private final GainFilter filter = new GainFilter();

    public RadarView(int frequency, int shipCount) {
        super(new BorderLayout());

        Simulation model = new Simulation(shipCount, Camera.WORLD_SIZE);
        canvas = new RadarCanvas(frequency, model, filter);

        add(canvas, BorderLayout.CENTER);
        add(buildFilterBar(), BorderLayout.SOUTH);
    }

    public RadarCanvas canvas() {
        return canvas;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(new Color(15, 25, 20));
        bar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Gain aralığı");
        title.setForeground(Color.WHITE);
        title.setPreferredSize(new Dimension(90, 20));

        final JLabel valueLabel = new JLabel("0.00 - 1.00");
        valueLabel.setForeground(new Color(120, 230, 150));
        valueLabel.setPreferredSize(new Dimension(90, 20));
        valueLabel.setHorizontalAlignment(JLabel.RIGHT);

        // İki başlıklı özel Slider'ımızı oluşturuyoruz
        final RangeSlider rangeSlider = new RangeSlider(0, 100, 0, 100);

        rangeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float minGain = rangeSlider.getLowValue() / 100f;
                float maxGain = rangeSlider.getHighValue() / 100f;

                filter.setMinGain(minGain);
                filter.setMaxGain(maxGain);

                valueLabel.setText(String.format("%.2f - %.2f", minGain, maxGain));

                canvas.repaint();
            }
        });

        /*
         * Filtrenin ilk degerleri.
         */
        filter.setMinGain(0.0f);
        filter.setMaxGain(1.0f);

        bar.add(title, BorderLayout.WEST);
        bar.add(rangeSlider, BorderLayout.CENTER);
        bar.add(valueLabel, BorderLayout.EAST);

        return bar;
    }

    /**
     * İki uçtan çekilebilir (Çift Başlıklı) Özel Slider Sınıfı
     */
    private class RangeSlider extends JComponent {
        private int min, max;
        private int low, high;
        private boolean draggingLow = false;
        private boolean draggingHigh = false;
        
        private final ArrayList<ChangeListener> listeners = new ArrayList<>();

        public RangeSlider(int min, int max, int low, int high) {
            this.min = min;
            this.max = max;
            this.low = low;
            this.high = high;
            setPreferredSize(new Dimension(200, 45));

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int x = e.getX();
                    int lowX = getXForValue(RangeSlider.this.low);
                    int highX = getXForValue(RangeSlider.this.high);

                    // Hangi başlığa daha yakın tıklandığını bul (hata payı 12 piksel)
                    if (Math.abs(x - lowX) < 12 && x <= highX) {
                        draggingLow = true;
                    } else if (Math.abs(x - highX) < 12) {
                        draggingHigh = true;
                    } else if (x < lowX) {
                        setLowValue(getValueForX(x));
                        draggingLow = true;
                    } else if (x > highX) {
                        setHighValue(getValueForX(x));
                        draggingHigh = true;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggingLow = false;
                    draggingHigh = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    int val = getValueForX(e.getX());
                    if (draggingLow) {
                        setLowValue(val);
                    } else if (draggingHigh) {
                        setHighValue(val);
                    }
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        public int getLowValue() { return low; }
        public int getHighValue() { return high; }

        public void setLowValue(int l) {
            this.low = Math.max(min, Math.min(l, high)); // min ile high arasına hapset
            repaint();
            fireStateChanged();
        }

        public void setHighValue(int h) {
            this.high = Math.min(max, Math.max(h, low)); // low ile max arasına hapset
            repaint();
            fireStateChanged();
        }

        public void addChangeListener(ChangeListener cl) {
            listeners.add(cl);
        }

        private void fireStateChanged() {
            ChangeEvent ce = new ChangeEvent(this);
            for (ChangeListener cl : listeners) {
                cl.stateChanged(ce);
            }
        }

        // Değeri X koordinatına çevirir
        private int getXForValue(int val) {
            int width = getWidth() - 24; // Kenar boşlukları (padding)
            return 12 + (int) (((double) (val - min) / (max - min)) * width);
        }

        // X koordinatını değere çevirir
        private int getValueForX(int x) {
            int width = getWidth() - 24;
            double percentage = (double) (x - 12) / width;
            return min + (int) Math.round(percentage * (max - min));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int y = 15; // Çizginin Y eksenindeki yeri
            int lowX = getXForValue(low);
            int highX = getXForValue(high);

            // 1. Arka plan çizgisi (Seçili olmayan kısım)
            g2.setColor(Color.DARK_GRAY);
            g2.fillRoundRect(12, y - 2, getWidth() - 24, 4, 4, 4);

            // 2. Seçili alanın çizgisi (Yeşil kısım)
            g2.setColor(new Color(120, 230, 150));
            g2.fillRoundRect(lowX, y - 2, highX - lowX, 4, 4, 4);

            // 3. Etiketleri ve tıkları (0.0, 0.25 vb.) çiz
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i <= 100; i += 25) {
                int tx = getXForValue(i);
                // Minik çentikler
                g2.setColor(Color.GRAY);
                g2.drawLine(tx, y + 4, tx, y + 8);
                // Etiket yazıları
                g2.setColor(Color.LIGHT_GRAY);
                String text = (i == 0 || i == 100) ? String.format("%.1f", i / 100f) : String.format("%.2f", i / 100f);
                int textWidth = g2.getFontMetrics().stringWidth(text);
                g2.drawString(text, tx - textWidth / 2, y + 22);
            }

            // 4. Tutamakları (Thumbs) çiz
            g2.setColor(Color.WHITE);
            g2.fillOval(lowX - 6, y - 6, 12, 12);
            g2.fillOval(highX - 6, y - 6, 12, 12);

            g2.dispose();
        }
    }
}