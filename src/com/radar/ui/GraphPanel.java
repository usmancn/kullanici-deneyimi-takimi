package com.radar.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * CPU veya GPU kullanımını zaman serisi olarak çizen Swing paneli.
 * Verileri bir {@link CircularBuffer}'dan alır ve her repaint'te yeniden çizer.
 */
@SuppressWarnings("serial")
public final class GraphPanel extends JPanel {

    private static final int   MIN_HEIGHT      = 150;
    private static final int   PADDING         = 20;
    private static final int   LABEL_MARGIN    = 40;
    private static final float LINE_THICKNESS  = 2.0f;
    private static final int   MAX_PERCENTAGE  = 100;
    private static final int   GRID_LINE_COUNT = 5;

    private final CircularBuffer dataBuffer;
    private final String         label;

    private Color lineColor      = new Color(0, 200, 80);
    private Color fillColorTop   = new Color(0, 200, 80, 80);
    private Color fillColorBottom = new Color(220, 50, 50, 0);

    /**
     * @param dataBuffer Veri kaynağı; null olamaz.
     * @param label      Grafiğin başlığı.
     */
    public GraphPanel(CircularBuffer dataBuffer, String label) {
        this.dataBuffer = dataBuffer;
        this.label      = label;
        setBackground(new Color(14, 14, 20));
        setMinimumSize(new Dimension(200, MIN_HEIGHT));
        setPreferredSize(new Dimension(300, MIN_HEIGHT));
    }

    /** Çizgi ve dolgu renklerini özelleştirmek için kullanılır. */
    public void setColors(Color line, Color fillTop, Color fillBottom) {
        this.lineColor       = line;
        this.fillColorTop    = fillTop;
        this.fillColorBottom = fillBottom;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        drawBackground(g2, w, h);
        drawGridLines(g2, w, h);
        drawTitle(g2, w);

        double[] data = dataBuffer.snapshot();
        if (data.length > 1) {
            drawFill(g2, data, w, h);
            drawLine(g2, data, w, h);
            drawCurrentValue(g2, data, w, h);
        }
    }

    private void drawBackground(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(14, 14, 20));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(30, 30, 40));
        g2.drawRect(LABEL_MARGIN, PADDING, w - LABEL_MARGIN - PADDING, h - 2 * PADDING);
    }

    private void drawGridLines(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(40, 40, 55));
        int chartH = h - 2 * PADDING;
        int chartW = w - LABEL_MARGIN - PADDING;
        for (int i = 0; i <= GRID_LINE_COUNT; i++) {
            int y = PADDING + (int) ((double) i / GRID_LINE_COUNT * chartH);
            g2.drawLine(LABEL_MARGIN, y, LABEL_MARGIN + chartW, y);

            int pct = MAX_PERCENTAGE - (i * MAX_PERCENTAGE / GRID_LINE_COUNT);
            g2.setColor(new Color(120, 120, 140));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g2.drawString(pct + "%", 2, y + 4);
            g2.setColor(new Color(40, 40, 55));
        }
    }

    private void drawTitle(Graphics2D g2, int w) {
        g2.setColor(new Color(180, 180, 200));
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(label, LABEL_MARGIN + 4, PADDING - 4);
    }

    private void drawLine(Graphics2D g2, double[] data, int w, int h) {
        int chartH = h - 2 * PADDING;
        int chartW = w - LABEL_MARGIN - PADDING;

        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(LINE_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D path = new Path2D.Float();
        for (int i = 0; i < data.length; i++) {
            int x = LABEL_MARGIN + (int) ((double) i / (data.length - 1) * chartW);
            int y = PADDING + chartH - (int) (clamp(data[i]) / MAX_PERCENTAGE * chartH);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        g2.draw(path);
    }

    private void drawFill(Graphics2D g2, double[] data, int w, int h) {
        int chartH = h - 2 * PADDING;
        int chartW = w - LABEL_MARGIN - PADDING;
        int baseline = PADDING + chartH;

        Path2D fill = new Path2D.Float();
        fill.moveTo(LABEL_MARGIN, baseline);
        for (int i = 0; i < data.length; i++) {
            int x = LABEL_MARGIN + (int) ((double) i / (data.length - 1) * chartW);
            int y = PADDING + chartH - (int) (clamp(data[i]) / MAX_PERCENTAGE * chartH);
            fill.lineTo(x, y);
        }
        fill.lineTo(LABEL_MARGIN + chartW, baseline);
        fill.closePath();

        GradientPaint gradient = new GradientPaint(
                0, PADDING, fillColorTop,
                0, PADDING + chartH, fillColorBottom
        );
        g2.setPaint(gradient);
        g2.fill(fill);
    }

    private void drawCurrentValue(Graphics2D g2, double[] data, int w, int h) {
        double last = data[data.length - 1];
        g2.setColor(lineColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(String.format("%.1f%%", last), w - 55, PADDING + 14);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(MAX_PERCENTAGE, value));
    }
}
