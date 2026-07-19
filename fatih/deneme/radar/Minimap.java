package deneme.radar;

import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import deneme.radar.gl.Geometry;
import deneme.radar.gl.GlBuffer;
import deneme.radar.gl.ShaderProgram;
import deneme.sim.Contact;
import deneme.sim.GainFilter;

/**
 * Sol ustte, ayri viewport'ta tum dunyayi gosteren minimap.
 * Hedefleri (gain'e gore parlaklikta), tarama cizgisini ve ana gorunumun
 * dortgenini cizer. TAB ile acilip kapanir (durumu RadarCanvas tutar).
 */
public class Minimap {

    /** Minimap ekranin bu oraninda; sol ustte durur. */
    public static final float FRACTION = 0.25f;

    private static final float SQUARE_SIZE = 10f;
    private static final float SCAN_THICKNESS = 6f;   // ana LINE_THICKNESS * 2
    private static final float RECT_INSET = 6f;

    /** Minimap zemini: ana zeminden biraz koyu. */
    private static final float BG_R = 0.01f, BG_G = 0.09f, BG_B = 0.05f;
    /** Ana zemin (scissor sonrasi geri yuklemek icin). */
    private static final float MAIN_BG_R = 0.02f, MAIN_BG_G = 0.15f, MAIN_BG_B = 0.08f;

    private final TargetGeometry geometry;
    private final GlBuffer rectPosition = new GlBuffer();
    private final GlBuffer rectColor    = new GlBuffer();
    private final float[] matrix = new float[16];

    public Minimap(TargetGeometry geometry) { this.geometry = geometry; }

    public void init(GL2 gl) {
        rectPosition.upload(gl, Geometry.RECT_VERTICES);
        rectColor.upload(gl, Geometry.RECT_COLORS);
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<Contact> detected, GainFilter filter, float scanY,
                     int surfaceWidth, int surfaceHeight) {

        int minimapWidth  = Math.round(surfaceWidth  * FRACTION);
        int minimapHeight = Math.round(surfaceHeight * FRACTION);
        int minimapX = 0;
        int minimapY = surfaceHeight - minimapHeight;

        gl.glViewport(minimapX, minimapY, minimapWidth, minimapHeight);

        // koyu zemin (yalniz minimap bolgesine)
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(minimapX, minimapY, minimapWidth, minimapHeight);
        gl.glClearColor(BG_R, BG_G, BG_B, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glClearColor(MAIN_BG_R, MAIN_BG_G, MAIN_BG_B, 1f);

        // hedefler (gain'e gore parlaklik, filtreli)
        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());
        for (int i = 0; i < detected.size(); i++) {
            Contact c = detected.get(i);
            if (!filter.accepts(c.gain)) continue;
            float brightness = 0.35f + 0.65f * c.gain;
            shader.setTint(gl, brightness, brightness, brightness, 1f);
            Camera.worldMatrix(matrix, c.x, c.y, SQUARE_SIZE, SQUARE_SIZE);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }
        shader.resetTint(gl);

        // tarama cizgisi
        shader.bindColor(gl, geometry.greenColor.id());
        Camera.worldMatrix(matrix, Camera.WORLD_SIZE / 2f, scanY, Camera.WORLD_SIZE, SCAN_THICKNESS);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);

        // gorunur alan dortgeni
        shader.bindPosition(gl, rectPosition.id());
        shader.bindColor(gl, rectColor.id());
        float viewCenterX = camera.centerX();
        float viewCenterY = camera.centerY();
        float viewWidth   = camera.rangeX() - 2f * RECT_INSET;
        float viewHeight  = camera.rangeY() - 2f * RECT_INSET;
        if (viewWidth  < 2f) viewWidth  = 2f;
        if (viewHeight < 2f) viewHeight = 2f;
        Camera.worldMatrix(matrix, viewCenterX, viewCenterY, viewWidth, viewHeight);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, Geometry.RECT_VERTEX_COUNT);

        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
    }

    public void dispose(GL2 gl) {
        rectPosition.dispose(gl);
        rectColor.dispose(gl);
    }
}
