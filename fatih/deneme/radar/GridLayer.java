package deneme.radar;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import deneme.radar.gl.Geometry;
import deneme.radar.gl.GlBuffer;
import deneme.radar.gl.Matrices;
import deneme.radar.gl.ShaderProgram;

/** Ekrana sabit civili grid cizgileri (zoom'dan bagimsiz, birim matris ile). */
public class GridLayer {

    private final GlBuffer position = new GlBuffer();
    private final GlBuffer color    = new GlBuffer();
    private final float[] matrix = new float[16];

    public void init(GL2 gl) {
        position.upload(gl, Geometry.gridVertices());
        color.upload(gl, Geometry.gridColors());
    }

    public void draw(GL2 gl, ShaderProgram shader) {
        shader.bindPosition(gl, position.id());
        shader.bindColor(gl, color.id());
        Matrices.identity(matrix);
        shader.setMatrix(gl, matrix);
        gl.glDrawArrays(GL.GL_LINES, 0, Geometry.GRID_VERTEX_COUNT);
    }

    public void dispose(GL2 gl) {
        position.dispose(gl);
        color.dispose(gl);
    }
}
