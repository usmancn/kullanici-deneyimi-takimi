package com.radar.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * Bir metrik kaynağının geçmiş değerlerini çizgi grafik olarak gösteren panel.
 *
 * <p>Veriler {@link CircularBuffer} üzerinden beslenir. Her {@code repaint()}
 * çağrısında {@code paintComponent()} override'ı tüm arabelleği okuyarak
 * grafik çizer.</p>
 *
 * <p><b>Thread güvenliği:</b> Yalnızca EDT'den kullanılmalıdır.</p>
 */
public final class GraphPanel extends JPanel {

    private static final int   MIN_HEIGHT      = 150;
    private static final int   PADDING         = 20;
    private static final int   LABEL_MARGIN    = 40;
    private static final float LINE_THICKNESS  = 2.0f;
    private static final int   MAX_PERCENTAGE  = 100;
    private static final int   GRID_LINE_COUNT = 5;

    private final CircularBuffer dataBuffer;
    private final String         label;

    /** Grafik çizgi rengi. */
    private Color lineColor = new Color(220, 50, 50);

    /** Grafik doldurma gradyan alt rengi (şeffaf). */
    private Color fillColorTop = new Color(220, 50, 50, 120);
    private Color fillColorBottom = new Color(220, 50, 50, 0);

    /**
     * Belirtilen tampon ve etiketle yeni bir grafik paneli oluşturur.
     *
     * @param dataBuffer Veri kaynağı; null olamaz.
     * @param label      Başlık etiketi; null olamaz.
     */
    public GraphPanel(CircularBuffer dataBuffer, String label) {
        if (dataBuffer == null) {
            throw new IllegalArgumentException("CircularBuffer null olamaz.");
        }
        if (label == null) {
            throw new IllegalArgumentException("Etiket null olamaz.");
        }
        this.dataBuffer = dataBuffer;
        this.label      = label;

        setBackground(new Color(18, 18, 24));
        setMinimumSize(new Dimension(200, MIN_HEIGHT));
        setPreferredSize(new Dimension(400, MIN_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();

        drawBackground(g2, w, h);
        drawGridLines(g2, w, h);
        drawTitle(g2, w);

        double[] data = dataBuffer.toArray();
        if (data.length >= 2) {
            drawFill(g2, data, w, h);
            drawLine(g2, data, w, h);
        }

        drawCurrentValue(g2, data, w, h);
    }

    // -------------------------------------------------------------------------
    // Çizim Yardımcı Metotları
    // -------------------------------------------------------------------------

    private void drawBackground(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(18, 18, 24));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(40, 40, 55));
        g2.drawRect(0, 0, w - 1, h - 1);
    }

    private void drawGridLines(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(45, 45, 60));
        Stroke dashed = new BasicStroke(
                1.0f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[]{4, 4},
                0
        );
        g2.setStroke(dashed);

        int graphH = h - 2 * PADDING;
        for (int i = 0; i <= GRID_LINE_COUNT; i++) {
            int y = PADDING + (graphH * i / GRID_LINE_COUNT);
            g2.drawLine(LABEL_MARGIN, y, w - PADDING, y);
            int pct = MAX_PERCENTAGE - (MAX_PERCENTAGE * i / GRID_LINE_COUNT);
            g2.setColor(new Color(90, 90, 110));
            g2.drawString(pct + "%", 2, y + 4);
            g2.setColor(new Color(45, 45, 60));
        }
        g2.setStroke(new BasicStroke(1.0f));
    }

    private void drawTitle(Graphics2D g2, int w) {
        g2.setColor(new Color(200, 200, 215));
        g2.setFont(getFont().deriveFont(12.0f));
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(label)) / 2;
        g2.drawString(label, x, PADDING - 4);
    }

    private void drawLine(Graphics2D g2, double[] data, int w, int h) {
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(LINE_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int graphW = w - LABEL_MARGIN - PADDING;
        int graphH = h - 2 * PADDING;
        int n      = data.length;

        for (int i = 0; i < n - 1; i++) {
            int x1 = LABEL_MARGIN + (int) ((double) i       / (dataBuffer.getCapacity() - 1) * graphW);
            int x2 = LABEL_MARGIN + (int) ((double) (i + 1) / (dataBuffer.getCapacity() - 1) * graphW);
            int y1 = PADDING + graphH - (int) (clamp(data[i])     / MAX_PERCENTAGE * graphH);
            int y2 = PADDING + graphH - (int) (clamp(data[i + 1]) / MAX_PERCENTAGE * graphH);
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawFill(Graphics2D g2, double[] data, int w, int h) {
        int graphW = w - LABEL_MARGIN - PADDING;
        int graphH = h - 2 * PADDING;
        int n      = data.length;
        int baseY  = PADDING + graphH;

        int[] xPoints = new int[n + 2];
        int[] yPoints = new int[n + 2];

        for (int i = 0; i < n; i++) {
            xPoints[i] = LABEL_MARGIN + (int) ((double) i / (dataBuffer.getCapacity() - 1) * graphW);
            yPoints[i] = PADDING + graphH - (int) (clamp(data[i]) / MAX_PERCENTAGE * graphH);
        }
        xPoints[n]     = xPoints[n - 1];
        yPoints[n]     = baseY;
        xPoints[n + 1] = LABEL_MARGIN;
        yPoints[n + 1] = baseY;

        GradientPaint gradient = new GradientPaint(
                0, PADDING,  fillColorTop,
                0, baseY,    fillColorBottom
        );
        g2.setPaint(gradient);
        g2.fillPolygon(xPoints, yPoints, n + 2);
    }

    private void drawCurrentValue(Graphics2D g2, double[] data, int w, int h) {
        String text;
        if (data.length == 0) {
            text = "N/A";
        } else {
            double last = data[data.length - 1];
            text = last < 0 ? "N/A" : String.format("%.1f%%", last);
        }
        g2.setColor(new Color(230, 80, 80));
        g2.setFont(getFont().deriveFont(java.awt.Font.BOLD, 14.0f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, w - fm.stringWidth(text) - PADDING, h - PADDING / 2);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(MAX_PERCENTAGE, value));
    }
}
