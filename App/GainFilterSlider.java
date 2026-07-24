package deneme.App;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import deneme.Simulation.GainFilterModel;

import javax.swing.JComponent;

/**
 * Gain filtresi: cift baslikli (low/high) slider + baslik/etiket, tek bir bilesende.
 * (GainFilterPanel + GainRangeSlider birlestirilmis hali.)
 *
 * <p>Secilen [min, max] araligi global (statik) tutulur; radar canvas'lari her
 * karede {@link #filterMin()} / {@link #filterMax()} degerlerini okuyup shader'a
 * gonderir ve bu aralik disindaki gain'leri gizler. Boylece kullanici araligi
 * yukari cekince (or. 0.60-1.00) sadece gemiler (yuksek gain) gorunur.
 */
public class GainFilterSlider extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;

    // --- Koyu/mavi tema renkleri ---
    private static final Color BG           = new Color(14, 14, 20);
    private static final Color TRACK_BG     = new Color(40, 44, 60);
    private static final Color TRACK_RANGE  = new Color(80, 160, 255);
    private static final Color THUMB        = new Color(230, 240, 255);
    private static final Color THUMB_BORDER = new Color(80, 160, 255);
    private static final Color TICK         = new Color(90, 96, 120);
    private static final Color LABEL        = new Color(150, 160, 185);
    private static final Color TITLE_COLOR  = new Color(180, 190, 215);
    private static final Color VALUE_COLOR  = new Color(120, 190, 255);

    private final GainFilterModel model;

    /** Acik tum slider'lar (senkron icin). */
    private static final CopyOnWriteArrayList<GainFilterSlider> INSTANCES = new CopyOnWriteArrayList<>();

    private final int min = SLIDER_MIN;
    private final int max = SLIDER_MAX;
    private int low;
    private int high;
    private boolean draggingLow = false;
    private boolean draggingHigh = false;

    public GainFilterSlider(GainFilterModel model) {
        this.model = model;
        this.low  = toSlider(model.filterMin());
        this.high = toSlider(model.filterMax());
        
        setOpaque(true);
        setBackground(BG);
        setPreferredSize(new Dimension(280, 74));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int lowX  = getXForValue(low);
                int highX = getXForValue(high);
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
                if (draggingLow)       setLowValue(val);
                else if (draggingHigh) setHighValue(val);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        INSTANCES.add(this);
    }

    private void setLowValue(int l) {
        this.low = Math.max(min, Math.min(l, high));
        applyAndSync();
    }

    private void setHighValue(int h) {
        this.high = Math.min(max, Math.max(h, low));
        applyAndSync();
    }

    private void applyAndSync() {
    	model.setRange(low / 100f, high / 100f);
        repaint();
        for (GainFilterSlider other : INSTANCES) {
            if (other == this) continue;
            other.low = this.low;
            other.high = this.high;
            other.repaint();
        }
    }

    private static int toSlider(float gain) {
        int v = Math.round(gain * 100f);
        if (v < SLIDER_MIN) return SLIDER_MIN;
        if (v > SLIDER_MAX) return SLIDER_MAX;
        return v;
    }

    private int getXForValue(int val) {
        int width = getWidth() - 24;
        return 12 + (int) (((double) (val - min) / (max - min)) * width);
    }

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

        // arka plan
        g2.setColor(BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // baslik + deger
        g2.setColor(TITLE_COLOR);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString("Gain Filtresi", 12, 16);

        g2.setColor(VALUE_COLOR);
        g2.setFont(getFont().deriveFont(11f));
        g2.drawString(String.format("Gorunen aralik: %.2f  -  %.2f", low / 100f, high / 100f), 12, 32);

        int y = 48;   // track ekseni
        int lowX = getXForValue(low);
        int highX = getXForValue(high);

        // track arka plani
        g2.setColor(TRACK_BG);
        g2.fillRoundRect(12, y - 2, getWidth() - 24, 4, 4, 4);

        // secili aralik
        g2.setColor(TRACK_RANGE);
        g2.fillRoundRect(lowX, y - 2, highX - lowX, 4, 4, 4);

        // tick + etiketler
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

        // tutamaklar
        g2.setColor(THUMB);
        g2.fillOval(lowX - 6, y - 6, 12, 12);
        g2.fillOval(highX - 6, y - 6, 12, 12);
        g2.setColor(THUMB_BORDER);
        g2.drawOval(lowX - 6, y - 6, 12, 12);
        g2.drawOval(highX - 6, y - 6, 12, 12);

        g2.dispose();
    }
}
