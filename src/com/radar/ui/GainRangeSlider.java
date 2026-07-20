package com.radar.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Iki uctan cekilebilir (cift baslikli) ozel slider.
 * Alt ve ust tutamak bir [low, high] araligi tanimlar; her degisimde
 * kayitli {@link ChangeListener}'lari tetikler. Gain araligi filtresi icin
 * hem kare hem yuvarlak radarda kullanilir.
 *
 * <p>Kaynak: kullanici-deneyimi-takimi RangeSlider ornegi; radarin koyu/mavi
 * temasina uyacak sekilde renkleri uyarlanmis ve senkronizasyon icin
 * {@link #setRangeSilently(int, int)} eklenmistir.
 */
public class GainRangeSlider extends JComponent {

    private static final long serialVersionUID = 1L;

    // --- Koyu/mavi tema renkleri ---
    private static final Color TRACK_BG      = new Color(40, 44, 60);
    private static final Color TRACK_RANGE   = new Color(80, 160, 255);
    private static final Color THUMB         = new Color(230, 240, 255);
    private static final Color THUMB_BORDER  = new Color(80, 160, 255);
    private static final Color TICK          = new Color(90, 96, 120);
    private static final Color LABEL         = new Color(150, 160, 185);

    private final int min, max;
    private int low, high;
    private boolean draggingLow = false;
    private boolean draggingHigh = false;

    private final ArrayList<ChangeListener> listeners = new ArrayList<>();

    public GainRangeSlider(int min, int max, int low, int high) {
        this.min = min;
        this.max = max;
        this.low = low;
        this.high = high;
        setPreferredSize(new Dimension(240, 45));
        setOpaque(false);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int lowX = getXForValue(GainRangeSlider.this.low);
                int highX = getXForValue(GainRangeSlider.this.high);

                // Hangi basliga daha yakin tiklandigini bul (hata payi 12 piksel)
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

    public int getLowValue()  { return low; }
    public int getHighValue() { return high; }

    public void setLowValue(int l) {
        this.low = Math.max(min, Math.min(l, high)); // min ile high arasina hapset
        repaint();
        fireStateChanged();
    }

    public void setHighValue(int h) {
        this.high = Math.min(max, Math.max(h, low)); // low ile max arasina hapset
        repaint();
        fireStateChanged();
    }

    /**
     * Degerleri listener tetiklemeden gunceller. Kardes slider'lari ( or. kare ve
     * yuvarlak radar sekmeleri) senkron tutarken sonsuz dongu olusmasin diye kullanilir.
     */
    public void setRangeSilently(int l, int h) {
        this.low = Math.max(min, Math.min(l, max));
        this.high = Math.max(this.low, Math.min(h, max));
        repaint();
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

    // Degeri X koordinatina cevirir
    private int getXForValue(int val) {
        int width = getWidth() - 24; // Kenar bosluklari (padding)
        return 12 + (int) (((double) (val - min) / (max - min)) * width);
    }

    // X koordinatini degere cevirir
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

        int y = 15; // Cizginin Y eksenindeki yeri
        int lowX = getXForValue(low);
        int highX = getXForValue(high);

        // 1. Arka plan cizgisi (secili olmayan kisim)
        g2.setColor(TRACK_BG);
        g2.fillRoundRect(12, y - 2, getWidth() - 24, 4, 4, 4);

        // 2. Secili alanin cizgisi (mavi kisim)
        g2.setColor(TRACK_RANGE);
        g2.fillRoundRect(lowX, y - 2, highX - lowX, 4, 4, 4);

        // 3. Etiketleri ve tiklari (0.0, 0.25 vb.) ciz
        g2.setFont(getFont().deriveFont(10f));
        for (int i = 0; i <= 100; i += 25) {
            int tx = getXForValue(i);
            g2.setColor(TICK);
            g2.drawLine(tx, y + 4, tx, y + 8);
            g2.setColor(LABEL);
            String text = (i == 0 || i == 100) ? String.format("%.1f", i / 100f) : String.format("%.2f", i / 100f);
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, tx - textWidth / 2, y + 22);
        }

        // 4. Tutamaklari (thumbs) ciz
        g2.setColor(THUMB);
        g2.fillOval(lowX - 6, y - 6, 12, 12);
        g2.fillOval(highX - 6, y - 6, 12, 12);
        g2.setColor(THUMB_BORDER);
        g2.drawOval(lowX - 6, y - 6, 12, 12);
        g2.drawOval(highX - 6, y - 6, 12, 12);

        g2.dispose();
    }
}
