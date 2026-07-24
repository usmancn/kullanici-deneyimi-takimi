package deneme.App;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.concurrent.CopyOnWriteArrayList;

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

    // --- Koyu/mavi tema default renkleri ---
    private static final Color DEFAULT_BG           = new Color(14, 14, 20);
    private static final Color DEFAULT_TRACK_BG     = new Color(40, 44, 60);
    private static final Color DEFAULT_TRACK_RANGE  = new Color(80, 160, 255);
    private static final Color DEFAULT_THUMB        = new Color(230, 240, 255);
    private static final Color DEFAULT_THUMB_BORDER = new Color(80, 160, 255);
    private static final Color DEFAULT_TICK         = new Color(90, 96, 120);
    private static final Color DEFAULT_LABEL        = new Color(150, 160, 185);
    private static final Color DEFAULT_TITLE_COLOR  = new Color(180, 190, 215);
    private static final Color DEFAULT_VALUE_COLOR  = new Color(120, 190, 255);

    // ---- GainSliderBuilder ile ayarlanan ozellikler (hepsinin default'u var) ----
    private Color bg          = DEFAULT_BG;
    private Color trackBg     = DEFAULT_TRACK_BG;
    private Color trackRange  = DEFAULT_TRACK_RANGE;
    private Color thumb       = DEFAULT_THUMB;
    private Color thumbBorder = DEFAULT_THUMB_BORDER;
    private Color tick        = DEFAULT_TICK;
    private Color label       = DEFAULT_LABEL;
    private Color titleColor  = DEFAULT_TITLE_COLOR;
    private Color valueColor  = DEFAULT_VALUE_COLOR;
    private boolean hasTitle = true;         // "Gain Filtresi" basligi
    private boolean hasSecondTitle = true;   // "Gorunen aralik ..." satiri

    /** Global filtre araligi (0..1). Canvas'lar buradan okur. */
    private static volatile float gFilterMin = 0f;
    private static volatile float gFilterMax = 1f;

    /** Acik tum slider'lar (senkron icin). */
    private static final CopyOnWriteArrayList<GainFilterSlider> INSTANCES = new CopyOnWriteArrayList<>();

    private final int min = SLIDER_MIN;
    private final int max = SLIDER_MAX;
    private int low;
    private int high;

    /**
     * Fare kontrolu artik slider'in icinde degil:
     * {@link deneme.Controller.GainFilterController} install() ile baglar.
     */
    public GainFilterSlider() {
        this.low  = toSlider(gFilterMin);
        this.high = toSlider(gFilterMax);

        setOpaque(true);
        setBackground(bg);
        setPreferredSize(new Dimension(280, 74));

        INSTANCES.add(this);
    }

    // ---- global filtre erisimi (canvas'lar kullanir) ----
    public static float filterMin() { return gFilterMin; }
    public static float filterMax() { return gFilterMax; }

    // ---- builder erisimi ----
    public void setBackgroundColor(Color c)      { if (c != null) { this.bg = c; setBackground(c); } }
    public void setTrackBackgroundColor(Color c) { if (c != null) this.trackBg = c; }
    public void setTrackRangeColor(Color c)      { if (c != null) this.trackRange = c; }
    public void setThumbColor(Color c)           { if (c != null) this.thumb = c; }
    public void setThumbBorderColor(Color c)     { if (c != null) this.thumbBorder = c; }
    public void setTickColor(Color c)            { if (c != null) this.tick = c; }
    public void setLabelColor(Color c)           { if (c != null) this.label = c; }
    public void setTitleColor(Color c)           { if (c != null) this.titleColor = c; }
    public void setValueColor(Color c)           { if (c != null) this.valueColor = c; }
    public void setHasTitle(boolean hasTitle)            { this.hasTitle = hasTitle; }
    public void setHasSecondTitle(boolean hasSecondTitle) { this.hasSecondTitle = hasSecondTitle; }

    /** AppBuilder'in gainSliderSize secimi. */
    public void setSliderSize(int width, int height) {
        if (width > 0 && height > 0) setPreferredSize(new Dimension(width, height));
    }

    // ---- GainFilterController erisimi ----
    /** Alt tutamagin ekran x konumu. */
    public int lowThumbX()  { return getXForValue(low); }

    /** Ust tutamagin ekran x konumu. */
    public int highThumbX() { return getXForValue(high); }

    /** Ekran x konumunun karsilik geldigi slider degeri. */
    public int valueForX(int x) { return getValueForX(x); }

    public void setLowValue(int l) {
        this.low = Math.max(min, Math.min(l, high));
        applyAndSync();
    }

    public void setHighValue(int h) {
        this.high = Math.min(max, Math.max(h, low));
        applyAndSync();
    }

    private void applyAndSync() {
        gFilterMin = low / 100f;
        gFilterMax = high / 100f;
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
        g2.setColor(bg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // baslik + deger (builder ile kapatilabilir)
        if (hasTitle) {
            g2.setColor(titleColor);
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString("Gain Filtresi", 12, 16);
        }

        if (hasSecondTitle) {
            g2.setColor(valueColor);
            g2.setFont(getFont().deriveFont(11f));
            g2.drawString(String.format("Gorunen aralik: %.2f  -  %.2f", low / 100f, high / 100f), 12, 32);
        }

        int y = 48;   // track ekseni
        int lowX = getXForValue(low);
        int highX = getXForValue(high);

        // track arka plani
        g2.setColor(trackBg);
        g2.fillRoundRect(12, y - 2, getWidth() - 24, 4, 4, 4);

        // secili aralik
        g2.setColor(trackRange);
        g2.fillRoundRect(lowX, y - 2, highX - lowX, 4, 4, 4);

        // tick + etiketler
        g2.setFont(getFont().deriveFont(10f));
        for (int i = 0; i <= 100; i += 25) {
            int tx = getXForValue(i);
            g2.setColor(tick);
            g2.drawLine(tx, y + 4, tx, y + 8);
            g2.setColor(label);
            String text = (i == 0 || i == 100) ? String.format("%.1f", i / 100f) : String.format("%.2f", i / 100f);
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, tx - textWidth / 2, y + 22);
        }

        // tutamaklar
        g2.setColor(thumb);
        g2.fillOval(lowX - 6, y - 6, 12, 12);
        g2.fillOval(highX - 6, y - 6, 12, 12);
        g2.setColor(thumbBorder);
        g2.drawOval(lowX - 6, y - 6, 12, 12);
        g2.drawOval(highX - 6, y - 6, 12, 12);

        g2.dispose();
    }
}
