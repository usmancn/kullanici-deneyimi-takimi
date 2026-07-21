package deneme.GLCore;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
public class ShaderProgram {
	private static final String VERTEX_120 =
	        "#version 120\n" +
	        "uniform mat4 matrix;\n" +
	        "attribute vec2 inPosition;\n" +
	        "attribute vec4 inGain;\n" +
	        "varying float outGain;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    outGain = inGain;\n" +
	        "    gl_Position = matrix * vec4(inPosition, 0.0, 1.0);\n" +
	        "}\n";
	private static final String RASTER_120 =
	        "#version 120\n" +
	        "varying float outGain;\n" +
	        "uniform vec3 darkGreen;\n" +
	        "uniform vec3 lightGreen;\n" +
	        "uniform vec3 darkRed;\n" +
	        "uniform vec3 lightRed;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    float Gain = outGain;\n" +
	        "    vec3 color;\n" +
	        "    if(Gain < 0.2) {\n" +
	        "    float t = gain / 0.2 ;\n" +
	        "    color = mix(darkGreen, lightGreen, t)}\n" +
	        "    if(Gain >= 0.6) {\n" +
	        "    float t = (gain - 0.6) / 0.4 ;\n" +
	        "    color = mix(lightRed, darkRed, t) ;}\n" +
	        "    else {\n" +
	        "    Color = vec3(0.0, 0.0, 0.0) ; }\n" +
	        "    gl_FragColor = vec4(Color,1.0);\n" +
	        "}\n";
	private int program;
    private int attribPosition;
    private int attribGain;
    private int uniformDarkGreen;
    private int uniformLightGreen;
    private int uniformDarkRed;
    private int uniformLightRed;
    private int uniformMatrix;

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

        attribGain = gl.glGetAttribLocation(program, "inColor");
        gl.glEnableVertexAttribArray(attribGain);

        uniformMatrix = gl.glGetUniformLocation(program, "matrix");
        uniformDarkGreen = gl.glGetUniformLocation(program, "darkGreen");
        uniformLightGreen = gl.glGetUniformLocation(program, "lightGreen");
        uniformDarkRed = gl.glGetUniformLocation(program, "darkRed");
        uniformLightRed = gl.glGetUniformLocation(program, "lightRed");
        
    }

    public void use(GL2 gl) { 
    	gl.glUseProgram(program); 
    }
    
    public void enableAttribs(GL2 gl) {
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glEnableVertexAttribArray(attribGain);
    }

    public void disableAttribs(GL2 gl) {
        gl.glDisableVertexAttribArray(attribPosition);
        gl.glDisableVertexAttribArray(attribGain);
    }
    
    public void bindPosition(GL2 gl, int bufferId) {
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribPosition, 3, GL.GL_FLOAT, false, 0, 0L);
    }
    
    public void bindGain(GL2 gl, int bufferId) {
        gl.glEnableVertexAttribArray(attribColor);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribGain, 3, GL.GL_FLOAT, false, 0, 0L);
    }
    
    public void dispose(GL2 gl) {
        disableAttribs(gl);
        gl.glDeleteProgram(program);
    }
    
    public void setMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, matrix, 0);
    }
    
    public void setDarkGreen(GL2 gl, float r, float g, float b, float a) {
        gl.glUniform4f(uniformDarkGreen, r, g, b, a);
    }
    
    public void setLightGreen(GL2 gl, float r, float g, float b, float a) {
        gl.glUniform4f(uniformDarkGreen, r, g, b, a);
    }
    
    public void setDarkRed(GL2 gl, float r, float g, float b, float a) {
        gl.glUniform4f(uniformDarkGreen, r, g, b, a);
    }
    
    public void setLightRed(GL2 gl, float r, float g, float b, float a) {
        gl.glUniform4f(uniformDarkGreen, r, g, b, a);
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
