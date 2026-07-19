package com.radar.gl.core;



/** Radardaki tum sabit geometriler ve renk dizileri tek yerde toplanmistir. */
public final class Geometry {

    private Geometry() {}

    // ---------------- Hedef karesi (8 ucgen = 24 vertex) ----------------

    public static final int TARGET_VERTEX_COUNT = 24;

    public static final float[] TARGET_VERTICES = {
         0f,  0f, 0f,   -1f, -1f, 0f,    0f, -1f, 0f,
         0f,  0f, 0f,    0f, -1f, 0f,    1f, -1f, 0f,
         0f,  0f, 0f,    1f, -1f, 0f,    1f,  0f, 0f,
         0f,  0f, 0f,    1f,  0f, 0f,    1f,  1f, 0f,
         0f,  0f, 0f,    1f,  1f, 0f,    0f,  1f, 0f,
         0f,  0f, 0f,    0f,  1f, 0f,   -1f,  1f, 0f,
         0f,  0f, 0f,   -1f,  1f, 0f,   -1f,  0f, 0f,
         0f,  0f, 0f,   -1f,  0f, 0f,   -1f, -1f, 0f
    };

    /** Hedef: merkez parlak, kenarlar koyu zemine kaynayan yesil. */
    public static float[] targetColors() {
        float[] colors = new float[TARGET_VERTEX_COUNT * 3];
        for (int v = 0; v < TARGET_VERTEX_COUNT; v++) {
            boolean isCenter = (v % 3 == 0);
            colors[v * 3]     = isCenter ? 1.00f : 0.30f;
            colors[v * 3 + 1] = isCenter ? 0.35f : 0.02f;
            colors[v * 3 + 2] = isCenter ? 0.25f : 0.02f;
        }
        return colors;
    }

    /** Tarama cizgisi: hepsi parlak yesil. */
    public static float[] greenColors() {
        float[] colors = new float[TARGET_VERTEX_COUNT * 3];
        for (int v = 0; v < TARGET_VERTEX_COUNT; v++) {
            colors[v * 3]     = 0.3f;
            colors[v * 3 + 1] = 1.0f;
            colors[v * 3 + 2] = 0.4f;
        }
        return colors;
    }

    // ---------------- Isaret cemberi ----------------

    public static final int CIRCLE_SEGMENTS = 64;

    public static float[] circleVertices() {
        float[] vertices = new float[CIRCLE_SEGMENTS * 3];
        for (int s = 0; s < CIRCLE_SEGMENTS; s++) {
            double angle = 2.0 * Math.PI * s / CIRCLE_SEGMENTS;
            vertices[s * 3]     = (float) Math.cos(angle);
            vertices[s * 3 + 1] = (float) Math.sin(angle);
            vertices[s * 3 + 2] = 0f;
        }
        return vertices;
    }

    /** Isaret rengi: sari. */
    public static float[] circleColors() {
        float[] colors = new float[CIRCLE_SEGMENTS * 3];
        for (int s = 0; s < CIRCLE_SEGMENTS; s++) {
            colors[s * 3]     = 1.0f;
            colors[s * 3 + 1] = 0.9f;
            colors[s * 3 + 2] = 0.2f;
        }
        return colors;
    }

    // ---------------- Minimap gorunur alan dortgeni ----------------

    public static final int RECT_VERTEX_COUNT = 4;

    public static final float[] RECT_VERTICES = {
        -1f, -1f, 0f,
         1f, -1f, 0f,
         1f,  1f, 0f,
        -1f,  1f, 0f
    };

    /** Dortgen rengi: acik mavi. */
    public static final float[] RECT_COLORS = {
        0.4f, 0.8f, 1f,
        0.4f, 0.8f, 1f,
        0.4f, 0.8f, 1f,
        0.4f, 0.8f, 1f
    };

    // ---------------- Grid ----------------

    /** Grid cizgilerinin ekrandaki oransal konumlari. Zoom'dan bagimsiz, sabit. */
    public static final float[] GRID_FRACTIONS = { 0f, 0.25f, 0.5f, 0.75f, 1f };

    public static final int GRID_VERTEX_COUNT = GRID_FRACTIONS.length * 4;

    /** Grid cizgileri dogrudan NDC'de (-1..1) uretilir; birim matrisle cizilir. */
    public static float[] gridVertices() {
        float[] vertices = new float[GRID_VERTEX_COUNT * 3];
        int cursor = 0;
        for (int i = 0; i < GRID_FRACTIONS.length; i++) {
            float ndc = 2f * GRID_FRACTIONS[i] - 1f;

            vertices[cursor++] = ndc; vertices[cursor++] = -1f; vertices[cursor++] = 0f;
            vertices[cursor++] = ndc; vertices[cursor++] =  1f; vertices[cursor++] = 0f;
            vertices[cursor++] = -1f; vertices[cursor++] = ndc; vertices[cursor++] = 0f;
            vertices[cursor++] =  1f; vertices[cursor++] = ndc; vertices[cursor++] = 0f;
        }
        return vertices;
    }

    /** Grid rengi: beyaz. */
    public static float[] gridColors() {
        float[] colors = new float[GRID_VERTEX_COUNT * 3];
        for (int i = 0; i < colors.length; i++) colors[i] = 1f;
        return colors;
    }
}
