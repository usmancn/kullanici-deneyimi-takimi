package deneme.radar;

import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import deneme.radar.gl.Geometry;
import deneme.radar.gl.GlBuffer;
import deneme.radar.gl.ShaderProgram;

/** Kullanici isaretleri: sari cemberler (LINE_LOOP). Secili olan biraz buyuk cizilir. */
public class MarkLayer {

    private final GlBuffer position = new GlBuffer();
    private final GlBuffer color    = new GlBuffer();
    private final MarkController marks;
    private final float[] matrix = new float[16];

    public MarkLayer(MarkController marks) { this.marks = marks; }

    public void init(GL2 gl) {
        position.upload(gl, Geometry.circleVertices());
        color.upload(gl, Geometry.circleColors());
    }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera) {
        shader.bindPosition(gl, position.id());
        shader.bindColor(gl, color.id());

        List<float[]> list = marks.marks();
        for (int i = 0; i < list.size(); i++) {
            float[] mark = list.get(i);
            float radius = (i == marks.selected())
                    ? MarkController.MARK_RADIUS * 1.15f
                    : MarkController.MARK_RADIUS;
            camera.modelMatrix(matrix, mark[0], mark[1], radius, radius);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, Geometry.CIRCLE_SEGMENTS);
        }
    }

    public void dispose(GL2 gl) {
        position.dispose(gl);
        color.dispose(gl);
    }
}
