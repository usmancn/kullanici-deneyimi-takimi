package deneme.radar.gl;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

/** Tek bir VBO'yu saran ince kabuk: uretir, statik veri yukler, siler. */
public class GlBuffer {

    private int id = 0;

    public int id() { return id; }

    /** VBO'yu (gerekirse) olusturur ve veriyi GL_STATIC_DRAW olarak yukler. */
    public void upload(GL2 gl, float[] data) {
        if (id == 0) {
            int[] ids = new int[1];
            gl.glGenBuffers(1, ids, 0);
            id = ids[0];
        }
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, id);
        gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        (long) data.length * Float.BYTES, buffer, GL.GL_STATIC_DRAW);
    }

    public void dispose(GL2 gl) {
        if (id != 0) {
            gl.glDeleteBuffers(1, new int[] { id }, 0);
            id = 0;
        }
    }
}
