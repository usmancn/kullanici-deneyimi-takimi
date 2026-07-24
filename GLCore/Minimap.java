package deneme.GLCore;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public final class Minimap {

    public static final float FRACTION = 0.25f;

    /** Karartma bandinin kalinligi (minimap kenarinin orani). */
    private static final float SHADOW_FRACTION = 0.09f;
    private static final float SHADOW_ALPHA = 0.5f;

    /** Gorunur alan dortgeninin dunya sinirlarindan iceri cekilme payi. */
    private static final float RECT_INSET = 3f;

    // ---- MinimapBuilder ile ayarlanan ozellikler (hepsinin default'u var) ----
    private volatile float fraction = FRACTION;      // minimap kenari / cizim alani orani
    private volatile boolean closable = true;        // TAB ile ac/kapa yapilabilir mi
    private java.awt.Color borderColor = null;       // minimap cercevesi (null: cizilmez)
    private java.awt.Color squareColor = new java.awt.Color(102, 204, 255);  // gorunur alan dortgeni

    private volatile boolean visible = true;
    private final float[] matrix = new float[16];

    // begin()'de hesaplanip end()'de kullanilan piksel dikdortgeni
    private int mapX, mapY, mapSide;

    public boolean isVisible() {
        return visible;
    }

    /** TAB ile ac/kapa; closable=false ise yok sayilir. */
    public void toggle() {
        if (closable) visible = !visible;
    }

    public void setFraction(double fraction) {
        if (fraction > 0 && fraction <= 1) this.fraction = (float) fraction;
    }

    public void setClosable(boolean closable) { this.closable = closable; }
    public void setBorderColor(java.awt.Color color) { this.borderColor = color; }
    public void setSquareColor(java.awt.Color color) { if (color != null) this.squareColor = color; }

    public boolean begin(GL2 gl, Viewport viewport) {
        if (!visible) return false;

        mapSide = Math.round(viewport.side() * fraction);
        if (mapSide < 8) return false;

        mapX = viewport.offsetX();
        mapY = viewport.offsetY() + viewport.side() - mapSide;   // sol ust kose

        gl.glViewport(mapX, mapY, mapSide, mapSide);
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(mapX, mapY, mapSide, mapSide);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        return true;
    }

    /**
     * Kameranin o an gordugu alani dortgen olarak cizer, kenar karartmasini
     * ekler ve ana viewport'a geri doner.
     */
    public void end(GL2 gl, Viewport viewport, Camera camera) {
        boolean blendWasOn = gl.glIsEnabled(GL.GL_BLEND);

        // sabit-fonksiyon cizimi: shader'dan kalan attribute dizileri kapatilir
        gl.glUseProgram(0);
        for (int i = 0; i < 8; i++) {
            gl.glDisableVertexAttribArray(i);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisable(GL.GL_TEXTURE_2D);

        // ---- gorunur alan dortgeni: dunya uzayi, tum dunyayi kaplayan matris ----
        Camera.worldMatrix(matrix, 0f, 0f, 2f, 2f);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadMatrixf(matrix, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        float minX = camera.minX() + RECT_INSET, maxX = camera.maxX() - RECT_INSET;
        float minY = camera.minY() + RECT_INSET, maxY = camera.maxY() - RECT_INSET;

        gl.glColor3f(squareColor.getRed() / 255f, squareColor.getGreen() / 255f,
                     squareColor.getBlue() / 255f);
        gl.glLineWidth(2f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex2f(minX, minY);
        gl.glVertex2f(maxX, minY);
        gl.glVertex2f(maxX, maxY);
        gl.glVertex2f(minX, maxY);
        gl.glEnd();
        gl.glLineWidth(1f);

        // ---- minimap cercevesi (istenirse) : NDC kenarlari ----
        if (borderColor != null) {
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glColor3f(borderColor.getRed() / 255f, borderColor.getGreen() / 255f,
                         borderColor.getBlue() / 255f);
            gl.glLineWidth(2f);
            gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex2f(-1f, -1f);
            gl.glVertex2f( 1f, -1f);
            gl.glVertex2f( 1f,  1f);
            gl.glVertex2f(-1f,  1f);
            gl.glEnd();
            gl.glLineWidth(1f);
        }

        // ---- alt ve sag kenar karartmasi: disa dogru koyulasan bant ----
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        float band = 2f * SHADOW_FRACTION;   

        gl.glBegin(GL2.GL_QUADS);
        // sag kenar: icerisi saydam -> kenar koyu
        gl.glColor4f(0f, 0f, 0f, 0f);           gl.glVertex2f(1f - band, -1f);
        gl.glColor4f(0f, 0f, 0f, SHADOW_ALPHA); gl.glVertex2f(1f, -1f);
        gl.glColor4f(0f, 0f, 0f, SHADOW_ALPHA); gl.glVertex2f(1f,  1f);
        gl.glColor4f(0f, 0f, 0f, 0f);           gl.glVertex2f(1f - band,  1f);

        // alt kenar
        gl.glColor4f(0f, 0f, 0f, SHADOW_ALPHA); gl.glVertex2f(-1f, -1f);
        gl.glColor4f(0f, 0f, 0f, SHADOW_ALPHA); gl.glVertex2f( 1f, -1f);
        gl.glColor4f(0f, 0f, 0f, 0f);           gl.glVertex2f( 1f, -1f + band);
        gl.glColor4f(0f, 0f, 0f, 0f);           gl.glVertex2f(-1f, -1f + band);
        gl.glEnd();

        gl.glColor4f(1f, 1f, 1f, 1f);
        if (!blendWasOn) gl.glDisable(GL.GL_BLEND);

        // ---- ana viewport'a don ----
        gl.glViewport(viewport.offsetX(), viewport.offsetY(), viewport.side(), viewport.side());
    }

    // ---- fare ----

    /** Tik minimap'in icinde mi (bilesen uzayi, sol-ust orijin). */
    public boolean contains(int eventX, int eventY, int componentWidth, int componentHeight) {
        if (!visible) return false;
        int side = Math.round(Viewport.side(componentWidth, componentHeight) * fraction);
        int localX = eventX - Viewport.offsetX(componentWidth, componentHeight);
        int localY = eventY - Viewport.offsetY(componentWidth, componentHeight);
        return localX >= 0 && localX < side && localY >= 0 && localY < side;
    }

    /** Tiklanan nokta ana gorunumun merkezi olur. */
    public void navigate(Camera camera, int eventX, int eventY,
                         int componentWidth, int componentHeight) {
        int side = Math.round(Viewport.side(componentWidth, componentHeight) * fraction);
        if (side <= 0) return;

        float fractionX = (eventX - Viewport.offsetX(componentWidth, componentHeight)) / (float) side;
        float fractionY = 1f - (eventY - Viewport.offsetY(componentWidth, componentHeight)) / (float) side;

        camera.centerOn(clamp01(fractionX) * Camera.WORLD_SIZE,
                        clamp01(fractionY) * Camera.WORLD_SIZE);
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }
}
