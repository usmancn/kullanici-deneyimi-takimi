package deneme.GLCore;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import deneme.Interfaces.ShaderLifecycle;

public class ShaderProgram implements ShaderLifecycle {
	// ---- Gain (data) programi ----
	private static final String VERTEX_120 =
	        "#version 120\n" +
	        "uniform mat4 matrix;\n" +
	        "attribute vec2 inPosition;\n" +
	        "attribute vec2 inUV;\n" +
	        "varying vec2 outUV;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    outUV = inUV;\n" +
	        "    gl_Position = matrix * vec4(inPosition, 0.0, 1.0);\n" +
	        "}\n";
	private static final String RASTER_120 =
	        "#version 120\n" +
	        "varying vec2 outUV;\n" +
	        "uniform sampler2D gainTex;\n" +
	        "uniform vec3 darkGreen;\n" +
	        "uniform vec3 lightGreen;\n" +
	        "uniform vec3 darkRed;\n" +
	        "uniform vec3 lightRed;\n" +
	        "uniform float filterMin;\n" +
	        "uniform float filterMax;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    float gain = texture2D(gainTex, outUV).r;\n" +
	        "    if (gain < filterMin || gain > filterMax) {\n" +
	        "        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +   // aralik disi -> gizle
	        "        return;\n" +
	        "    }\n" +
	        "    vec3 color;\n" +
	        "    \n" +
	        "        color = mix(darkGreen, lightGreen, gain);\n" +
	        "    \n" +
	        "    gl_FragColor = vec4(color, 1.0);\n" +
	        "}\n";

	//Scanline programi
	private static final String SCAN_VERTEX_120 =
	        "#version 120\n" +
	        "uniform mat4 matrix;\n" +
	        "attribute vec2 inPosition;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    gl_Position = matrix * vec4(inPosition, 0.0, 1.0);\n" +
	        "}\n";
	private static final String SCAN_RASTER_120 =
	        "#version 120\n" +
	        "uniform vec3 scanlineColor;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    gl_FragColor = vec4(scanlineColor, 1.0);\n" +
	        "}\n";

	// gain programi
	private int program;
    private int attribPosition;
    private int attribUV;
    private int uniformDarkGreen;
    private int uniformLightGreen;
    private int uniformDarkRed;
    private int uniformLightRed;
    private int uniformMatrix;
    private int uniformGainTex;
    private int uniformFilterMin;
    private int uniformFilterMax;

    // scanline programi
    private int scanProgram;
    private int scanAttribPosition;
    private int scanUniformMatrix;
    private int scanUniformColor;

    public void init(GL2 gl) {
        // ---- gain programi ----
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

        attribPosition = gl.glGetAttribLocation(program, "inPosition");
        attribUV       = gl.glGetAttribLocation(program, "inUV");

        uniformMatrix     = gl.glGetUniformLocation(program, "matrix");
        uniformGainTex    = gl.glGetUniformLocation(program, "gainTex");
        uniformDarkGreen  = gl.glGetUniformLocation(program, "darkGreen");
        uniformLightGreen = gl.glGetUniformLocation(program, "lightGreen");
        uniformDarkRed    = gl.glGetUniformLocation(program, "darkRed");
        uniformLightRed   = gl.glGetUniformLocation(program, "lightRed");
        uniformFilterMin  = gl.glGetUniformLocation(program, "filterMin");
        uniformFilterMax  = gl.glGetUniformLocation(program, "filterMax");

        // ---- scanline programi ----
        int scanVs = compile(gl, GL2ES2.GL_VERTEX_SHADER,   SCAN_VERTEX_120);
        int scanFs = compile(gl, GL2ES2.GL_FRAGMENT_SHADER, SCAN_RASTER_120);

        scanProgram = gl.glCreateProgram();
        gl.glAttachShader(scanProgram, scanVs);
        gl.glAttachShader(scanProgram, scanFs);
        gl.glLinkProgram(scanProgram);

        gl.glGetProgramiv(scanProgram, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GL.GL_FALSE) {
            throw new RuntimeException("Scanline link hatasi:\n" + programLog(gl, scanProgram));
        }

        gl.glDeleteShader(scanVs);
        gl.glDeleteShader(scanFs);

        scanAttribPosition = gl.glGetAttribLocation(scanProgram, "inPosition");
        scanUniformMatrix  = gl.glGetUniformLocation(scanProgram, "matrix");
        scanUniformColor   = gl.glGetUniformLocation(scanProgram, "scanlineColor");

        gl.glUseProgram(program);
        gl.glUniform1i(uniformGainTex, 0);   // gainTex -> texture unit 0
        setGainColorMap(gl, GainColorMap.green());
        setLightRed(gl, 0.6f, 0.0f, 0.0f);
        setDarkRed(gl,  0.4f, 0.0f, 0.0f);
        setGainFilter(gl, 0.0f, 1.0f);   // baslangicta hepsi gorunur

        gl.glUseProgram(scanProgram);
        setScanColor(gl, 0.3f, 1.0f, 0.4f);
    }

    // ---- gain programi kullanimi ----
    public void use(GL2 gl) {
    	gl.glUseProgram(program);
    }

    public void enableAttribs(GL2 gl) {
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glEnableVertexAttribArray(attribUV);
    }

    public void disableAttribs(GL2 gl) {
        gl.glDisableVertexAttribArray(attribPosition);
        gl.glDisableVertexAttribArray(attribUV);
    }

    /**
     * Interleaved quad buffer'i baglar: her vertex [x, y, u, v] (4 float).
     * inPosition -> ilk 2 float, inUV -> sonraki 2 float.
     */
    public void bindVertices(GL2 gl, int bufferId) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        int stride = 4 * Float.BYTES;
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glVertexAttribPointer(attribPosition, 2, GL.GL_FLOAT, false, stride, 0L);
        gl.glEnableVertexAttribArray(attribUV);
        gl.glVertexAttribPointer(attribUV, 2, GL.GL_FLOAT, false, stride, 2L * Float.BYTES);
    }

    public void setMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, matrix, 0);
    }

    public void setDarkGreen(GL2 gl, float r, float g, float b) {
        gl.glUniform3f(uniformDarkGreen, r, g, b);
    }

    public void setLightGreen(GL2 gl, float r, float g, float b) {
        gl.glUniform3f(uniformLightGreen, r, g, b);
    }

    public void setGainColorMap(GL2 gl, GainColorMap colorMap) {
        setDarkGreen(gl, colorMap.lowRed, colorMap.lowGreen, colorMap.lowBlue);
        setLightGreen(gl, colorMap.highRed, colorMap.highGreen, colorMap.highBlue);
    }

    public void setDarkRed(GL2 gl, float r, float g, float b) {
        gl.glUniform3f(uniformDarkRed, r, g, b);
    }

    public void setLightRed(GL2 gl, float r, float g, float b) {
        gl.glUniform3f(uniformLightRed, r, g, b);
    }

    /** Gain filtre araligi; [min,max] disindaki gain'ler gizlenir. */
    public void setGainFilter(GL2 gl, float min, float max) {
        gl.glUniform1f(uniformFilterMin, min);
        gl.glUniform1f(uniformFilterMax, max);
    }

    // ---- scanline programi kullanimi ----
    public void useScan(GL2 gl) {
        gl.glUseProgram(scanProgram);
    }

    public void setScanMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(scanUniformMatrix, 1, false, matrix, 0);
    }

    public void setScanColor(GL2 gl, float r, float g, float b) {
        gl.glUniform3f(scanUniformColor, r, g, b);
    }

    public void bindScanPosition(GL2 gl, int bufferId) {
        gl.glEnableVertexAttribArray(scanAttribPosition);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(scanAttribPosition, 2, GL.GL_FLOAT, false, 0, 0L);
    }

    public void dispose(GL2 gl) {
        disableAttribs(gl);
        gl.glDeleteProgram(program);
        gl.glDeleteProgram(scanProgram);
    }

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
