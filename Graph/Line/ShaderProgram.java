package deneme.Graph.Line;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;

public class ShaderProgram {
	private static final String VERTEX_120 =
	        "#version 120\n" +
	        "uniform mat4 matrix;\n" +
	        "attribute vec2 inPosition;\n" +
	        "varying float vGain;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    vGain = inPosition.y / 1000.0;\n" +   // y = gain*1000 -> gain
	        "    gl_Position = matrix * vec4(inPosition, 0.0, 1.0);\n" +
	        "}\n";
	private static final String RASTER_120 =
	        "#version 120\n" +
	        "uniform vec3 lineColor;\n" +
	        "uniform float filterMin;\n" +
	        "uniform float filterMax;\n" +
	        "varying float vGain;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    if (vGain < filterMin || vGain > filterMax) discard;\n" +
	        "    gl_FragColor = vec4(lineColor, 1.0);\n" +
	        "}\n";
	private static final String AVERAGE_RASTER_120 =
	        "#version 120\n" +
	        "uniform vec3 averageLineColor;\n" +
	        "uniform float filterMin;\n" +
	        "uniform float filterMax;\n" +
	        "varying float vGain;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    if (vGain < filterMin || vGain > filterMax) discard;\n" +
	        "    gl_FragColor = vec4(averageLineColor, 0.3);\n" +
	        "}\n";

	private int lineProgram;
	private int averageLineProgram;

	private int attribPosition;
	private int uniformMatrix;
	private int uniformLineColor;
	private int uniformFilterMin;
	private int uniformFilterMax;

	private int averageAttribPosition;
	private int averageUniformMatrix;
	private int uniformAverageLineColor;
	private int averageFilterMin;
	private int averageFilterMax;

	public void init(GL2 gl) {
		int vertexShader    = compile(gl, GL2ES2.GL_VERTEX_SHADER,   VERTEX_120);
		int fragmentShader1 = compile(gl, GL2ES2.GL_FRAGMENT_SHADER, RASTER_120);
		int fragmentShader2 = compile(gl, GL2ES2.GL_FRAGMENT_SHADER, AVERAGE_RASTER_120);

		lineProgram = gl.glCreateProgram();
		gl.glAttachShader(lineProgram, vertexShader);
		gl.glAttachShader(lineProgram, fragmentShader1);
		gl.glLinkProgram(lineProgram);

		int[] linkStatus = new int[1];
		gl.glGetProgramiv(lineProgram, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
		if (linkStatus[0] == GL.GL_FALSE) {
			throw new RuntimeException("Link hatasi:\n" + programLog(gl, lineProgram));
		}

		attribPosition   = gl.glGetAttribLocation(lineProgram, "inPosition");
		uniformMatrix    = gl.glGetUniformLocation(lineProgram, "matrix");
		uniformLineColor = gl.glGetUniformLocation(lineProgram, "lineColor");
		uniformFilterMin = gl.glGetUniformLocation(lineProgram, "filterMin");
		uniformFilterMax = gl.glGetUniformLocation(lineProgram, "filterMax");

		averageLineProgram = gl.glCreateProgram();
		gl.glAttachShader(averageLineProgram, vertexShader);
		gl.glAttachShader(averageLineProgram, fragmentShader2);
		gl.glLinkProgram(averageLineProgram);

		gl.glGetProgramiv(averageLineProgram, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
		if (linkStatus[0] == GL.GL_FALSE) {
			throw new RuntimeException("Link hatasi:\n" + programLog(gl, averageLineProgram));
		}

		averageAttribPosition   = gl.glGetAttribLocation(averageLineProgram, "inPosition");
		averageUniformMatrix    = gl.glGetUniformLocation(averageLineProgram, "matrix");
		uniformAverageLineColor = gl.glGetUniformLocation(averageLineProgram, "averageLineColor");
		averageFilterMin        = gl.glGetUniformLocation(averageLineProgram, "filterMin");
		averageFilterMax        = gl.glGetUniformLocation(averageLineProgram, "filterMax");

		
		gl.glDeleteShader(vertexShader);
		gl.glDeleteShader(fragmentShader1);
		gl.glDeleteShader(fragmentShader2);

		
		gl.glUseProgram(lineProgram);
		setLineColor(gl, 0.3f, 1.0f, 0.4f);
		setGainFilter(gl, 0.0f, 1.0f);
		gl.glUseProgram(averageLineProgram);
		setAverageLineColor(gl, 1.0f, 0.3f, 0.4f);
		setAverageGainFilter(gl, 0.0f, 1.0f);
		gl.glUseProgram(0);
	}

	
	public void use(GL2 gl) {
		gl.glUseProgram(lineProgram);
	}

	
	public void useAverage(GL2 gl) {
		gl.glUseProgram(averageLineProgram);
	}

	public void enableAttribs(GL2 gl) {
		gl.glEnableVertexAttribArray(attribPosition);
	}

	public void disableAttribs(GL2 gl) {
		gl.glDisableVertexAttribArray(attribPosition);
	}

	
	public void bindVertices(GL2 gl, int bufferId) {
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
		int stride = 2 * Float.BYTES;
		gl.glEnableVertexAttribArray(attribPosition);
		gl.glVertexAttribPointer(attribPosition, 2, GL.GL_FLOAT, false, stride, 0L);
	}

	
	public void bindAverageVertices(GL2 gl, int bufferId) {
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
		int stride = 2 * Float.BYTES;
		gl.glEnableVertexAttribArray(averageAttribPosition);
		gl.glVertexAttribPointer(averageAttribPosition, 2, GL.GL_FLOAT, false, stride, 0L);
	}

	
	public void setMatrix(GL2 gl, float[] matrix) {
		gl.glUniformMatrix4fv(uniformMatrix, 1, false, matrix, 0);
	}

	
	public void setAverageMatrix(GL2 gl, float[] matrix) {
		gl.glUniformMatrix4fv(averageUniformMatrix, 1, false, matrix, 0);
	}

	public void setLineColor(GL2 gl, float r, float g, float b) {
		gl.glUniform3f(uniformLineColor, r, g, b);
	}

	public void setAverageLineColor(GL2 gl, float r, float g, float b) {
		gl.glUniform3f(uniformAverageLineColor, r, g, b);
	}

	/** Ana cizgi programi icin gain filtre araligi (use() sonrasi cagrilir). */
	public void setGainFilter(GL2 gl, float min, float max) {
		gl.glUniform1f(uniformFilterMin, min);
		gl.glUniform1f(uniformFilterMax, max);
	}

	/** Ortalama cizgi programi icin gain filtre araligi (useAverage() sonrasi cagrilir). */
	public void setAverageGainFilter(GL2 gl, float min, float max) {
		gl.glUniform1f(averageFilterMin, min);
		gl.glUniform1f(averageFilterMax, max);
	}

	public void dispose(GL2 gl) {
		gl.glUseProgram(0);
		gl.glDeleteProgram(lineProgram);
		gl.glDeleteProgram(averageLineProgram);
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
