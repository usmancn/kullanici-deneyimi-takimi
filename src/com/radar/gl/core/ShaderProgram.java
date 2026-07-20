package com.radar.gl.core;



import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;

/**
 * Vertex + fragment shader'i derler/linkler; attribute ve uniform konumlarini
 * tutar; VBO baglama, matris ve tint (renk carpani) yukleme yardimcilarini saglar.
 *
 * Orijinaldeki shader'a "tint" (carpimsal) ve "addColor" (toplamsal) uniform'lari eklendi:
 *   outColor = inColor * tint + addColor
 * tint parlaklik/opaklik icin, addColor ise mevcut renge EKSTRA bir ton eklemek icin
 * kullanilir (or. kirmizi taban gradyanina gain'e bagli mavi eklemek). Ikisi de
 * varsayilan (tint=1,1,1,1; addColor=0,0,0,0) oldugu icin diger katmanlar degismez.
 */
public class ShaderProgram {

    private static final String VERTEX_120 =
        "#version 120\n" +
        "uniform mat4 matrix;\n" +
        "uniform vec4 tint;\n" +
        "uniform vec4 addColor;\n" +
        "attribute vec4 inPosition;\n" +
        "attribute vec4 inColor;\n" +
        "varying vec4 outColor;\n" +
        "void main()\n" +
        "{\n" +
        "    outColor = inColor * tint + addColor;\n" +
        "    gl_Position = matrix * inPosition;\n" +
        "}\n";

    private static final String RASTER_120 =
        "#version 120\n" +
        "varying vec4 outColor;\n" +
        "void main()\n" +
        "{\n" +
        "    gl_FragColor = outColor;\n" +
        "}\n";

    private int program;
    private int attribPosition;
    private int attribColor;
    private int uniformMatrix;
    private int uniformTint;
    private int uniformAddColor;

    public void init(GL2 gl) {
        int vertexShader   = compile(gl, GL2ES2.GL_VERTEX_SHADER,   VERTEX_120);
        int fragmentShader = compile(gl, GL2ES2.GL_FRAGMENT_SHADER, RASTER_120);

        program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glLinkProgram(program);

        int[] linkStatus = new int[1];
        gl.glGetProgramiv(program, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GL.GL_FALSE) {
            throw new RuntimeException("Link hatasi:\n" + programLog(gl, program));
        }

        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);
        gl.glUseProgram(program);

        attribPosition = gl.glGetAttribLocation(program, "inPosition");
        gl.glEnableVertexAttribArray(attribPosition);

        attribColor = gl.glGetAttribLocation(program, "inColor");
        gl.glEnableVertexAttribArray(attribColor);

        uniformMatrix   = gl.glGetUniformLocation(program, "matrix");
        uniformTint     = gl.glGetUniformLocation(program, "tint");
        uniformAddColor = gl.glGetUniformLocation(program, "addColor");
        resetTint(gl);
        resetAddColor(gl);
    }

    public void use(GL2 gl) { gl.glUseProgram(program); }

    public void enableAttribs(GL2 gl) {
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glEnableVertexAttribArray(attribColor);
    }

    public void disableAttribs(GL2 gl) {
        gl.glDisableVertexAttribArray(attribPosition);
        gl.glDisableVertexAttribArray(attribColor);
    }

    /** VBO'yu pozisyon attribute'una baglar (3 float / vertex). */
    public void bindPosition(GL2 gl, int bufferId) {
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribPosition, 3, GL.GL_FLOAT, false, 0, 0L);
    }

    /** VBO'yu renk attribute'una baglar (3 float / vertex). */
    public void bindColor(GL2 gl, int bufferId) {
        gl.glEnableVertexAttribArray(attribColor);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribColor, 3, GL.GL_FLOAT, false, 0, 0L);
    }
    
    /** RAM'deki bir FloatBuffer'i (VBO olmadan) pozisyon olarak baglar ve renk attribute'unu devre disi birakir. */
    public void bindPositionOnly(GL2 gl, java.nio.FloatBuffer buffer, int components) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glVertexAttribPointer(attribPosition, components, GL.GL_FLOAT, false, 0, buffer);
        
        gl.glDisableVertexAttribArray(attribColor);
        // Renk kapaliyken default inColor (1,1,1,1) olsun ki tint uniform'u calissin
        gl.glVertexAttrib4f(attribColor, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void setMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, matrix, 0);
    }

    public void setTint(GL2 gl, float r, float g, float b, float a) {
        gl.glUniform4f(uniformTint, r, g, b, a);
    }

    public void resetTint(GL2 gl) { setTint(gl, 1f, 1f, 1f, 1f); }

    /** Renge eklenecek toplamsal terim (outColor = inColor*tint + addColor). */
    public void setAddColor(GL2 gl, float r, float g, float b, float a) {
        gl.glUniform4f(uniformAddColor, r, g, b, a);
    }

    public void resetAddColor(GL2 gl) { setAddColor(gl, 0f, 0f, 0f, 0f); }

    public void dispose(GL2 gl) {
        disableAttribs(gl);
        gl.glDeleteProgram(program);
    }

    // ---- yardimcilar ----

    private int compile(GL2 gl, int shaderType, String source) {
        int shader = gl.glCreateShader(shaderType);
        gl.glShaderSource(shader, 1, new String[] { source }, new int[] { source.length() }, 0);
        gl.glCompileShader(shader);

        int[] compileStatus = new int[1];
        gl.glGetShaderiv(shader, GL2ES2.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == GL.GL_FALSE) {
            throw new RuntimeException("Derleme hatasi:\n" + shaderLog(gl, shader));
        }
        return shader;
    }

    private String shaderLog(GL2 gl, int shader) {
        int[] logLength = new int[1];
        gl.glGetShaderiv(shader, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);
        if (logLength[0] <= 0) return "(log bos)";
        byte[] logBytes = new byte[logLength[0]];
        gl.glGetShaderInfoLog(shader, logLength[0], new int[1], 0, logBytes, 0);
        return new String(logBytes).trim();
    }

    private String programLog(GL2 gl, int program) {
        int[] logLength = new int[1];
        gl.glGetProgramiv(program, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);
        if (logLength[0] <= 0) return "(log bos)";
        byte[] logBytes = new byte[logLength[0]];
        gl.glGetProgramInfoLog(program, logLength[0], new int[1], 0, logBytes, 0);
        return new String(logBytes).trim();
    }
}
