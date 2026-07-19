package deneme;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

public class SecondGraph extends GLCanvas implements GLEventListener {
	
	private static final float BG_R = 0.02f;
    private static final float BG_G = 0.15f;
    private static final float BG_B = 0.08f;
    
	private static final int SCREEN_RESOLUTION = 1000;
	
	private static final int RADIAL_LINES = 12;
	
	private static final int CIRCLE_RADIUS = SCREEN_RESOLUTION / 2;
	
	private static final int CIRCLE_CENTER = SCREEN_RESOLUTION / 2;
	
	private static final int MSAA_SAMPLES = 8;
	
	private static final float GL_LINE_WIDTH = 3f;
	
	private static final int CIRCLE_STEP = 128;
	
	private float scanY = 0f;
	private long lastTimeNs = 0L;
	private static final float SCAN_PERIOD_SEC = 10f;
	
    private static final float MARK_RADIUS = 20f;
    private static final float PICK_TOLERANCE = 15f;
    private final List<float[]> marks = new CopyOnWriteArrayList<>();
    
    private volatile int selectedMark = -1;
    private volatile boolean markKeyDown = false;
    
    private static final float ZONE_LOW = 0.4f;
    private static final float ZONE_HIGH = 0.6f;
    private static final float ZOOM_STEP = 1.15f;
    private static final float MIN_VIEW_RANGE = 40f;
    private static final int ANCHOR_LOW = -1;
    private static final int ANCHOR_CENTER = 0;
    private static final int ANCHOR_HIGH = 1;
    
    private volatile float viewMinX = 0f;
    private volatile float viewMaxX = SCREEN_RESOLUTION;
    private volatile float viewMinY = 0f;
    private volatile float viewMaxY = SCREEN_RESOLUTION;
    
    private int pressX = 0;
    private int pressY = 0;
    private int lastDragX = 0;
    private int lastDragY = 0;
    private boolean dragging = false;
    private static final int DRAG_THRESHOLD = 5;
	
	private final float[] modelMatrix = new float[16];
	
	private static final float[] CIRCLE_VERTICES = circleVertices();

    private static float[] circleVertices() {
        float[] vertices = new float[CIRCLE_STEP * 3];
        for (int segment = 0; segment < CIRCLE_STEP ; segment++) {
            double angle = 2.0 * Math.PI * segment / CIRCLE_STEP;
            vertices[segment * 3]     = (float) Math.cos(angle);
            vertices[segment * 3 + 1] = (float) Math.sin(angle);
            vertices[segment * 3 + 2] = 0f;
        }
        return vertices;
    }
    
    private static final float[] CIRCLE_COLORS = circleColors(1.0f,1.0f,1.0f);

    private static float[] circleColors(float red, float green, float blue) {
        float[] colors = new float[CIRCLE_STEP * 3];
        for (int segment = 0; segment < CIRCLE_STEP ; segment++) {
            colors[segment * 3]     = red;
            colors[segment * 3 + 1] = green;
            colors[segment * 3 + 2] = blue;
        }
        return colors;
    }
    
    private static final float[] LINES = gridVertices();
    private static float[] gridVertices() {
        float[] vertices = new float[RADIAL_LINES * 2 * 3];
        int k = 0;
        for (int i = 0; i < RADIAL_LINES ; i++) {
            double angle = 2.0 * Math.PI / RADIAL_LINES * i;
            vertices[k++] = CIRCLE_CENTER;
            vertices[k++] = CIRCLE_CENTER;
            vertices[k++] = 0f;
            vertices[k++] = CIRCLE_CENTER + CIRCLE_RADIUS * (float) Math.cos(angle);
            vertices[k++] = CIRCLE_CENTER + CIRCLE_RADIUS * (float) Math.sin(angle);
            vertices[k++] = 0f;
        }
        return vertices;
    }
    
    private static final float[] SCAN_COLOR = circleColors(0.3f,1f,0.4f);
    private static final float[] MARK_COLOR = circleColors(1.0f,0.9f,0.2f);
    
    private static final float[] ENEMIES = visualizeEnemies();
    private static float[] visualizeEnemies() {
    	final int STEP = 32; 
    	float[] vertices = new float[STEP * 2 * 3];
    	int k = 0;
    	for(int i = 0; i < STEP; i++) {
    		
    	}
    	return vertices;
    }
    
    private int shaderProgram;
    private final int[] vbo = new int[5]; // 0 = pozisyon, 1 = renk, 2 = grid pozisyon, 3 = scan renk, 4 mark renk
    private int attribPosition;
    private int attribColor;
    private int uniformMatrix;
    
    FPSAnimator animator;
	
	Simulator model;
	
	private static final String VERTEX_120 =
	        "#version 120\n" +
	        "uniform mat4 matrix;\n" +
	        "attribute vec4 inPosition;\n" +
	        "attribute vec4 inColor;\n" +
	        "varying vec4 outColor;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    outColor = inColor;\n" +
	        "    gl_Position = matrix * inPosition;\n" +
	        "}\n";

	    private static final String RASTER_120 =
	        "#version 120\n" +
	        "varying vec4 outColor;\n" +
	        "void main()\n" +
	        "{\n" +
	        "    gl_FragColor = outColor;\n" +
	        "}\n";
	
	private static GLCapabilities caps() {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(MSAA_SAMPLES);
        return capabilities;
    }
	
	public void executeGraph(int frequency) {
        if (frequency <= 0) return;


        if (animator == null) {
            animator = new FPSAnimator(this, frequency);
            animator.start();
            return;
        }
        if (animator.isStarted()) animator.stop();
        animator.setFPS(frequency);
        animator.start();
    }
	
	public SecondGraph(int count, int frequency) {
		super(caps());
        this.model = new Simulator(count);
        setPreferredSize(new Dimension(SCREEN_RESOLUTION, SCREEN_RESOLUTION));
        addGLEventListener(this);
        installInputHandlers();
        executeGraph(frequency);
	}
	
	private float screenToWorldX(int screenX) {
        float fraction = (float) screenX / getWidth();
        return 0 + fraction * SCREEN_RESOLUTION;
    }

    private float screenToWorldY(int screenY) {
        float fraction = 1f - (float) screenY / getHeight();
        return 0 + fraction * SCREEN_RESOLUTION;
    }
    
    private int pickMark(float worldX, float worldY) {
        for (int markIndex = 0; markIndex < marks.size(); markIndex++) {
            float[] mark = marks.get(markIndex);
            float deltaX = worldX - mark[0];
            float deltaY = worldY - mark[1];
            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (Math.abs(distance - MARK_RADIUS) <= PICK_TOLERANCE) {
                return markIndex;
            }
        }
        return -1;
    }

	
    private void installInputHandlers() {
        setFocusable(true);

        // TAB varsayilan olarak odak gezinme tusudur; KeyListener'a ulasmasi icin kapatiyoruz.
        setFocusTraversalKeysEnabled(false);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                if (event.getButton() != MouseEvent.BUTTON1) return;

                pressX = event.getX();
                pressY = event.getY();
                lastDragX = pressX;
                lastDragY = pressY;
                dragging = false;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) return;

                if (dragging) {
                    dragging = false;
                    return;
                }

                float worldX = screenToWorldX(event.getX());
                float worldY = screenToWorldY(event.getY());

                if (markKeyDown) {
                    marks.add(new float[] { worldX, worldY });
                    selectedMark = marks.size() - 1;
                } else {
                    selectedMark = pickMark(worldX, worldY);
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                requestFocusInWindow();

                double rotation = event.getPreciseWheelRotation();
                if (rotation == 0.0) return;

                boolean zoomIn = rotation < 0.0;
                applyZoom(event.getX(), event.getY(), zoomIn);
            }
        };

        MouseMotionAdapter motionHandler = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {

                if (markKeyDown) return;

                if (!dragging) {
                    int totalDeltaX = Math.abs(event.getX() - pressX);
                    int totalDeltaY = Math.abs(event.getY() - pressY);
                    if (totalDeltaX < DRAG_THRESHOLD && totalDeltaY < DRAG_THRESHOLD) return;
                    dragging = true;
                }

                int deltaPixelX = event.getX() - lastDragX;
                int deltaPixelY = event.getY() - lastDragY;
                lastDragX = event.getX();
                lastDragY = event.getY();

                applyPan(deltaPixelX, deltaPixelY);
            }
        };

        addMouseListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        addMouseMotionListener(motionHandler);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_M) {
                    markKeyDown = true;
                }
                 else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    int index = selectedMark;
                    if (index >= 0 && index < marks.size()) {
                        marks.remove(index);
                        selectedMark = -1;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_M) {
                    markKeyDown = false;
                }
            }
        });
    }
	
	/** Farenin bulundugu bolgeye gore hangi kenarin sabit kalacagini belirler. */
    private static int anchorFor(float fraction) {
        if (fraction < ZONE_LOW) return ANCHOR_LOW;
        if (fraction > ZONE_HIGH) return ANCHOR_HIGH;
        return ANCHOR_CENTER;
    }

    private void applyZoom(int screenX, int screenY, boolean zoomIn) {
        float fractionX = (float) screenX / getWidth();
        float fractionY = 1f - (float) screenY / getHeight();

        float factor = zoomIn ? (1f / ZOOM_STEP) : ZOOM_STEP;

        float[] rangeX = { viewMinX, viewMaxX };
        zoomRange(rangeX, factor, anchorFor(fractionX));
        viewMinX = rangeX[0];
        viewMaxX = rangeX[1];

        float[] rangeY = { viewMinY, viewMaxY };
        zoomRange(rangeY, factor, anchorFor(fractionY));
        viewMinY = rangeY[0];
        viewMaxY = rangeY[1];
    }

    /**
     * Araligi verilen carpanla daraltir/genisletir.
     * ANCHOR_LOW: alt kenar sabit, ust kenar yaklasir.
     * ANCHOR_HIGH: ust kenar sabit, alt kenar yaklasir.
     * ANCHOR_CENTER: iki kenar esit yaklasir.
     */
    private static void zoomRange(float[] minMax, float factor, int anchor) {
        float min = minMax[0];
        float max = minMax[1];

        float newRange = (max - min) * factor;
        if (newRange < MIN_VIEW_RANGE) newRange = MIN_VIEW_RANGE;
        if (newRange > SCREEN_RESOLUTION) newRange = SCREEN_RESOLUTION;

        if (anchor == ANCHOR_LOW) {
            max = min + newRange;
        } else if (anchor == ANCHOR_HIGH) {
            min = max - newRange;
        } else {
            float center = (min + max) / 2f;
            min = center - newRange / 2f;
            max = center + newRange / 2f;
        }

        if (min < 0f) {
            max -= min;
            min = 0f;
        }
        if (max > SCREEN_RESOLUTION) {
            min -= (max - SCREEN_RESOLUTION);
            max = SCREEN_RESOLUTION;
        }
        if (min < 0f) min = 0f;

        minMax[0] = min;
        minMax[1] = max;
    }

    // ---------------- PAN ----------------

    private void applyPan(int deltaPixelX, int deltaPixelY) {
        float rangeX = viewMaxX - viewMinX;
        float rangeY = viewMaxY - viewMinY;

        float worldDeltaX = -(float) deltaPixelX / getWidth() * rangeX;
        float worldDeltaY = (float) deltaPixelY / getHeight() * rangeY;

        float[] shiftedX = shiftRange(viewMinX, viewMaxX, worldDeltaX);
        viewMinX = shiftedX[0];
        viewMaxX = shiftedX[1];

        float[] shiftedY = shiftRange(viewMinY, viewMaxY, worldDeltaY);
        viewMinY = shiftedY[0];
        viewMaxY = shiftedY[1];
    }

    private static float[] shiftRange(float min, float max, float delta) {
        float range = max - min;

        min += delta;
        max += delta;

        if (min < 0f) {
            min = 0f;
            max = range;
        }
        if (max > SCREEN_RESOLUTION) {
            max = SCREEN_RESOLUTION;
            min = SCREEN_RESOLUTION - range;
        }

        return new float[] { min, max };
    }
	
	public void stopGraph() {
        if (animator != null && animator.isStarted()) animator.stop();
    }
	
    private static float dataToRadius(float y) {
        return y / SCREEN_RESOLUTION * CIRCLE_RADIUS;
    }

    private static float dataToAngle(float x) {
        return x / SCREEN_RESOLUTION * (float) (2.0 * Math.PI);
    }
    
	
		private static void setRangeMatrix(float[] matrix, float centerX, float centerY, float widthWorld, float heightWorld,
	            float minX, float maxX, float minY, float maxY) {
		float rangeX = maxX - minX;
		float rangeY = maxY - minY;
		
		float translateX = 2f * (centerX - minX) / rangeX - 1f;
		float translateY = 2f * (centerY - minY) / rangeY - 1f;
		float scaleX = widthWorld / rangeX;
		float scaleY = heightWorld / rangeY;
		
		Arrays.fill(matrix, 0f);
		matrix[0]  = scaleX;      // 1. sutun: x olcegi
		matrix[5]  = scaleY;      // 2. sutun: y olcegi
		matrix[10] = 1f;          // 3. sutun
		matrix[12] = translateX;  // 4. sutun: konum
		matrix[13] = translateY;
		matrix[15] = 1f;
		}
	
		private static void setWorldMatrix(float[] matrix, float centerX, float centerY,
	            float widthWorld, float heightWorld) {
	setRangeMatrix(matrix, centerX, centerY, widthWorld, heightWorld, 0f, SCREEN_RESOLUTION, 0f, SCREEN_RESOLUTION);
	}
	
		private void setViewMatrix(float[] matrix) {
	        setRangeToCoordinate(matrix, viewMinX, viewMaxX, viewMinY, viewMaxY);
	    }
		private void setModelMatrix(float[] matrix, float centerX, float centerY,
                float widthWorld, float heightWorld) {
setRangeMatrix(matrix, centerX, centerY, widthWorld, heightWorld,
       viewMinX, viewMaxX, viewMinY, viewMaxY);
}

	    private static void setRangeToCoordinate(float[] matrix, float minX, float maxX, float minY, float maxY) {
	        float rangeX = maxX - minX;
	        float rangeY = maxY - minY;
	        Arrays.fill(matrix, 0f);
	        matrix[0]  = 2f / rangeX;
	        matrix[5]  = 2f / rangeY;
	        matrix[10] = 1f;
	        matrix[12] = -2f * minX / rangeX - 1f;
	        matrix[13] = -2f * minY / rangeY - 1f;
	        matrix[15] = 1f;
	    }
	    
	    private void advanceScan() {
	        final float resolution = SCREEN_RESOLUTION;

	        long now = System.nanoTime();
	        if (lastTimeNs == 0L) {
	            lastTimeNs = now;
	            return;
	        }
	        float deltaSec = (now - lastTimeNs) / 1_000_000_000f;
	        lastTimeNs = now;

	        float bandEnd   = scanY + (resolution * deltaSec) / SCAN_PERIOD_SEC;

	        if (bandEnd >= resolution) {
	            bandEnd = bandEnd % resolution;
	        }
	        scanY = bandEnd;
	    }
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glUseProgram(shaderProgram);
        
        advanceScan();
        
        gl.glLineWidth(GL_LINE_WIDTH);
        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[1]);
        for(float i = 1; i > 0; i -= 0.25) {
        	setModelMatrix(modelMatrix, CIRCLE_CENTER, CIRCLE_CENTER, i * SCREEN_RESOLUTION, i * SCREEN_RESOLUTION);
	        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
	        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_STEP);
        }
        
        bindPositionBuffer(gl, vbo[2]);
        bindColorBuffer(gl, vbo[1]);
        setViewMatrix(modelMatrix);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_LINES, 0, RADIAL_LINES * 2);
        
        float scanRadius = dataToRadius(scanY);
        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[3]);
        gl.glLineWidth(GL_LINE_WIDTH);
        
        setModelMatrix(modelMatrix, CIRCLE_CENTER, CIRCLE_CENTER, 2f * scanRadius, 2f * scanRadius);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_STEP);
        
        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[4]);
        for (int markIndex = 0; markIndex < marks.size(); markIndex++) {
            float[] mark = marks.get(markIndex);
            float radius = (markIndex == selectedMark) ? MARK_RADIUS * 1.15f : MARK_RADIUS;
            setModelMatrix(modelMatrix, mark[0], mark[1], radius, radius);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_STEP);
        }
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
        gl.glDisableVertexAttribArray(attribPosition);
        gl.glDisableVertexAttribArray(attribColor);
        gl.glDeleteBuffers(5, vbo, 0);
        gl.glDeleteProgram(shaderProgram);
        stopGraph();
		
	}
	
	private static void uploadBuffer(GL2 gl, int bufferId, float[] data) {
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        (long) data.length * Float.BYTES, buffer, GL.GL_STATIC_DRAW);
    }

    private void bindPositionBuffer(GL2 gl, int bufferId) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribPosition, 3, GL.GL_FLOAT, false, 0, 0L);
    }

    private void bindColorBuffer(GL2 gl, int bufferId) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribColor, 3, GL.GL_FLOAT, false, 0, 0L);
    }


	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

        System.out.println("GL_VERSION : " + gl.glGetString(GL.GL_VERSION));
        System.out.println("GLSL       : " + gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION));

        int vertexShader   = compile(gl, GL2ES2.GL_VERTEX_SHADER, VERTEX_120);
        int fragmentShader = compile(gl, GL2ES2.GL_FRAGMENT_SHADER, RASTER_120);

        shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);
        gl.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        gl.glGetProgramiv(shaderProgram, GL2ES2.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GL.GL_FALSE) {
            throw new RuntimeException("Link hatasi:\n" + programLog(gl, shaderProgram));
        }
        
        gl.glGenBuffers(5, vbo, 0);
        
        uploadBuffer(gl, vbo[0], CIRCLE_VERTICES);
        uploadBuffer(gl, vbo[1], CIRCLE_COLORS);
        uploadBuffer(gl, vbo[2], LINES);
        uploadBuffer(gl, vbo[3], SCAN_COLOR);
        uploadBuffer(gl, vbo[4], MARK_COLOR);
        
        attribPosition = gl.glGetAttribLocation(shaderProgram, "inPosition");
        gl.glEnableVertexAttribArray(attribPosition);
        bindPositionBuffer(gl, vbo[0]);

        attribColor = gl.glGetAttribLocation(shaderProgram, "inColor");
        gl.glEnableVertexAttribArray(attribColor);
        bindColorBuffer(gl, vbo[1]);
        
        
        
        uniformMatrix = gl.glGetUniformLocation(shaderProgram, "matrix");

        // ________KOYU YESIL ARKA PLAN___________
        gl.glClearColor(BG_R, BG_G, BG_B, 1f);

        // ________ANTIALIASING___________
        gl.glEnable(GL.GL_MULTISAMPLE);

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glLineWidth(GL_LINE_WIDTH);
		
	}
	

	@Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().glViewport(0, 0, width, height);
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