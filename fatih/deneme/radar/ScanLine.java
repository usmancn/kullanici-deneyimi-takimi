package deneme.radar;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import deneme.radar.gl.Geometry;
import deneme.radar.gl.ShaderProgram;
import deneme.sim.Contact;
import deneme.sim.GainFilter;
import deneme.sim.Simulation;

/**
 * Asagidan yukari suzulen tarama cizgisi.
 * Her karede modeli zamanla ilerletir (gemiler hareket eder), tarama bandini
 * kaydirir ve banttaki, filtreyi gecen gemileri tespit listesine ekler.
 * Bir tam tur tamamlaninca liste temizlenir (radar afterglow davranisi).
 */
public class ScanLine {

    /** Bir tam taramanin suresi, saniye. */
    private static final float SCAN_PERIOD_SEC = 10f;

    /** Tarama cizgisinin kalinligi, dunya birimi. */
    private static final float LINE_THICKNESS = 3f;

    private final TargetGeometry geometry;
    private final Simulation model;
    private final GainFilter filter;

    private float scanY = 0f;
    private long lastTimeNs = 0L;
    private final List<Contact> detected = new ArrayList<>();
    private final float[] matrix = new float[16];

    public ScanLine(TargetGeometry geometry, Simulation model, GainFilter filter) {
        this.geometry = geometry;
        this.model = model;
        this.filter = filter;
    }

    public List<Contact> detected() { return detected; }
    public float scanY()            { return scanY; }

    public void reset() {
        scanY = 0f;
        lastTimeNs = 0L;
        detected.clear();
    }

    /** Zaman ilerlet: modeli guncelle, bandi kaydir, tespitleri topla. */
    public void advance() {
        final float resolution = Camera.WORLD_SIZE;

        long now = System.nanoTime();
        if (lastTimeNs == 0L) { lastTimeNs = now; return; }
        float deltaSec = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;

        model.update(deltaSec);

        float bandStart = scanY;
        float bandEnd   = scanY + (resolution * deltaSec) / SCAN_PERIOD_SEC;

        if (bandEnd >= resolution) {
            detected.clear();
            bandEnd = bandEnd % resolution;
            bandStart = 0f;
        }

        List<Contact> snapshot = model.snapshot();
        for (int i = 0; i < snapshot.size(); i++) {
            Contact c = snapshot.get(i);
            if (c.y >= bandStart && c.y < bandEnd && filter.accepts(c.gain)) {
                detected.add(c);
            }
        }

        scanY = bandEnd;
    }

    /** Ana ekranda tarama bandini cizer. */
    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        float thickness = LINE_THICKNESS * camera.rangeY() / Camera.WORLD_SIZE;

        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.greenColor.id());
        camera.modelMatrix(matrix, camera.centerX(), scanY, camera.rangeX(), thickness);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
    }
}
