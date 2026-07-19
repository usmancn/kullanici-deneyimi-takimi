package com.radar.gl.core;




/**
 * Gorunur dunya araligini (viewMin/Max) tutan kamera.
 * Zoom ve pan sadece bu araligi degistirir; cizim matrisleri buradan uretilir.
 * Alanlar volatile: girdi (EDT) yazar, cizim (GL thread) okur.
 */
public class Camera {

    public static final float WORLD_SIZE = 1000f;
    public static final float MIN_VIEW_RANGE = 40f;

    private volatile float minX = 0f, maxX = WORLD_SIZE;
    private volatile float minY = 0f, maxY = WORLD_SIZE;

    public float minX() { return minX; }
    public float maxX() { return maxX; }
    public float minY() { return minY; }
    public float maxY() { return maxY; }

    public float rangeX()  { return maxX - minX; }
    public float rangeY()  { return maxY - minY; }
    public float centerX() { return (minX + maxX) / 2f; }
    public float centerY() { return (minY + maxY) / 2f; }

    public void setRangeX(float min, float max) { this.minX = min; this.maxX = max; }
    public void setRangeY(float min, float max) { this.minY = min; this.maxY = max; }

    // ---------------- koordinat donusumu ----------------

    public float screenToWorldX(int screenX, int width) {
        return minX + (float) screenX / width * rangeX();
    }

    /** Y ekseni ters: AWT yukaridan, OpenGL asagidan sayar. */
    public float screenToWorldY(int screenY, int height) {
        return minY + (1f - (float) screenY / height) * rangeY();
    }

    // ---------------- matrisler ----------------

    /** Ana gorunum: zoom ve pan uygulanmis aralik. */
    public void modelMatrix(float[] matrix, float cx, float cy, float w, float h) {
        Matrices.range(matrix, cx, cy, w, h, minX, maxX, minY, maxY);
    }

    /** Tum dunya: zoom'dan bagimsiz (minimap icin). */
    public static void worldMatrix(float[] matrix, float cx, float cy, float w, float h) {
        Matrices.range(matrix, cx, cy, w, h, 0f, WORLD_SIZE, 0f, WORLD_SIZE);
    }

    // ---------------- pan yardimcilari ----------------

    /** Araligi, genisligini koruyarak verilen noktaya ortalar. */
    public void centerOn(float targetX, float targetY) {
        float[] sx = shift(minX, maxX, targetX - centerX());
        minX = sx[0]; maxX = sx[1];
        float[] sy = shift(minY, maxY, targetY - centerY());
        minY = sy[0]; maxY = sy[1];
    }

    /** Araligi kaydirir, dunya sinirlarinin disina tasmasini engeller. */
    public static float[] shift(float min, float max, float delta) {
        float range = max - min;
        min += delta;
        max += delta;
        if (min < 0f)          { min = 0f;          max = range; }
        if (max > WORLD_SIZE)  { max = WORLD_SIZE;  min = WORLD_SIZE - range; }
        return new float[] { min, max };
    }
}
