package deneme;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.nio.FloatBuffer;
import java.util.ArrayList;
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

/**
 * FirstGraph'in polar (PPI radar) surumu.
 *
 * Kartezyen yerine kutupsal yerlesim:
 *   x (0..1000) -> aci   (0..360 derece)
 *   y (0..1000) -> yaricap (0..RADAR_RADIUS), yani cember capi 1000px.
 *
 * Tarama cizgisi merkezden buyuyen bir cember olarak ilerler; tespit mantigi
 * FirstGraph ile aynidir cunku scanY yine y'yi (= yarici) tarar.
 *
 * Etiketler bu surumde kaldirildi.
 */
public class yedek extends GLCanvas implements GLEventListener {

    private FPSAnimator animator;
    private static final int SCREEN_RESOLUTION = 1000;

    /** Orijinal karenin kartezyen boyutu; aci genisligini bundan tureiyoruz. */
    private static final float SQUARE_SIZE = 10f;

    /** Bir tam taramanin suresi, saniye. */
    private static final float SCAN_PERIOD_SEC = 10f;

    /** Cizgi kalinligi (varsayilan). */
    private static final float GL_LINE_WIDTH = 2f;

    /** Grid cizgilerinin kalinligi. */
    private static final float GRID_LINE_WIDTH = 1.2f;

    /** Tarama cemberinin kalinligi. */
    private static final float SCAN_LINE_WIDTH = 3f;

    /** MSAA ornek sayisi. */
    private static final int MSAA_SAMPLES = 8;

    // ---------------- POLAR YERLESIM ----------------

    /** Radar merkezi (dunya koordinati). */
    private static final float RADAR_CENTER = SCREEN_RESOLUTION / 2f;   // 500

    /** Radar yaricapi; cap = 1000px istegine gore 500. */
    private static final float RADAR_RADIUS = SCREEN_RESOLUTION / 2f;   // 500

    /** Cember/yay cozunurlukleri. */
    private static final int CIRCLE_SEG = 128;   // grid halkalari, tarama, isaretler
    private static final int DISC_SEG = 24;      // minimap blip (dolu disk)

    /** 360 / 36 = 10 radyal cizgi. */
    private static final int RADIAL_LINES = 10;

    /** Mesafe halkalari: y = 250/500/750/1000 -> yaricap 125/250/375/500. */
    private static final float[] GRID_RING_DATA = { 250f, 500f, 750f, 1000f };
    private static final float[] GRID_RING_RADII = ringRadii();

    /** Hedefin acisal genisligi: orijinal 10px, 1000px tam cevreye orani kadar = 2π/100 (sabit aci). */
    private static final float TARGET_ANGULAR_SPAN =
            (float) (2.0 * Math.PI / (SCREEN_RESOLUTION / SQUARE_SIZE));

    /** Hedefin radyal kalinligi (dunya birimi). Sadece yay uzunlugu belirtildigi icin bu serbest; istege gore ayarla. */
    private static final float TARGET_RADIAL_THICKNESS = 10f;

    /** Bir hedef yayini kac dilime bolerek pürüzsüz cizecegiz. */
    private static final int TARGET_ARC_SEGMENTS = 6;
    private static final int TARGET_VERTS = TARGET_ARC_SEGMENTS * 6;  // dilim basi 2 ucgen = 6 vertex

    /** Minimap'te hedef blipinin cap benzeri boyutu (dunya birimi). */
    private static final float MINIMAP_BLIP_SIZE = 50f;

    // ---------------- ISARET ----------------

    private static final float MARK_RADIUS = 20f;
    private static final float PICK_TOLERANCE = 15f;
    private static final int DRAG_THRESHOLD = 5;

    // ---------------- MINIMAP ----------------

    private static final float MINIMAP_FRACTION = 0.25f;
    private static final float MINIMAP_RECT_INSET = 6f;
    private static final float MINIMAP_BG_R = 0.01f;
    private static final float MINIMAP_BG_G = 0.09f;
    private static final float MINIMAP_BG_B = 0.05f;

    // ---------------- ZOOM ----------------

    private static final float ZONE_LOW = 0.4f;
    private static final float ZONE_HIGH = 0.6f;
    private static final float ZOOM_STEP = 1.15f;
    private static final float MIN_VIEW_RANGE = 40f;

    private static final int ANCHOR_LOW = -1;
    private static final int ANCHOR_CENTER = 0;
    private static final int ANCHOR_HIGH = 1;

    // ---------------- RENKLER ----------------

    private static final float BG_R = 0.02f;
    private static final float BG_G = 0.15f;
    private static final float BG_B = 0.08f;

    private static final float[] GRID_COLOR = { 0.16f, 0.5f, 0.32f };
    private static final float[] SCAN_COLOR = { 0.35f, 1.0f, 0.5f };
    private static final float[] MARK_COLOR = { 1.0f, 0.9f, 0.2f };
    private static final float[] RECT_COLOR = { 0.4f, 0.8f, 1.0f };
    private static final float[] TARGET_COLOR = { 1.0f, 0.5f, 0.25f };

    // ================================================================

    private final Simulator model;
    private final int shipCount;
    private final int maxTargetVerts;

    private int shaderProgram;

    // 0=birim cember, 1=birim disk, 2=radyal grid poz, 3=birim dortgen,
    // 4=grid renk, 5=tarama renk, 6=isaret renk, 7=dortgen renk, 8=blip renk,
    // 9=hedef poz (dinamik), 10=hedef renk (amber)
    private final int[] vbo = new int[11];

    private int attribPosition;
    private int attribColor;
    private int uniformMatrix;

    private final float[] modelMatrix = new float[16];

    /** Gorunur dunya araligi. Zoom ve pan sadece bunlari degistirir. */
    private volatile float viewMinX = 0f;
    private volatile float viewMaxX = SCREEN_RESOLUTION;
    private volatile float viewMinY = 0f;
    private volatile float viewMaxY = SCREEN_RESOLUTION;

    private volatile boolean minimapVisible = true;

    private int pressX = 0;
    private int pressY = 0;
    private int lastDragX = 0;
    private int lastDragY = 0;
    private boolean dragging = false;
    private boolean minimapDragging = false;

    /** Tarama cemberinin anlik y (= yaricap kaynagi) konumu ve zaman referansi. */
    private float scanY = 0f;
    private long lastTimeNs = 0L;

    private final List<int[]> detected = new ArrayList<>();

    private final List<float[]> marks = new CopyOnWriteArrayList<>();
    private volatile int selectedMark = -1;
    private volatile boolean markKeyDown = false;

    // ---------------- STATIK GEOMETRI URETICILERI ----------------

    private static float[] ringRadii() {
        float[] r = new float[GRID_RING_DATA.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = dataToRadius(GRID_RING_DATA[i]);
        }
        return r;
    }

    /** Birim cember cevresi (LINE_LOOP). */
    private static float[] unitCircleOutline() {
        float[] v = new float[CIRCLE_SEG * 3];
        for (int i = 0; i < CIRCLE_SEG; i++) {
            double a = 2.0 * Math.PI * i / CIRCLE_SEG;
            v[i * 3]     = (float) Math.cos(a);
            v[i * 3 + 1] = (float) Math.sin(a);
            v[i * 3 + 2] = 0f;
        }
        return v;
    }

    /** Birim dolu disk (TRIANGLE_FAN): merkez + cevre + kapanis. */
    private static float[] unitDisc() {
        float[] v = new float[(DISC_SEG + 2) * 3];
        v[0] = 0f; v[1] = 0f; v[2] = 0f;
        for (int i = 0; i <= DISC_SEG; i++) {
            double a = 2.0 * Math.PI * i / DISC_SEG;
            v[(i + 1) * 3]     = (float) Math.cos(a);
            v[(i + 1) * 3 + 1] = (float) Math.sin(a);
            v[(i + 1) * 3 + 2] = 0f;
        }
        return v;
    }

    /** Radyal grid cizgileri: merkezden kenara, her 36 derecede bir (dunya koordinati). */
    private static float[] radialGridPositions() {
        float[] v = new float[RADIAL_LINES * 2 * 3];
        int c = 0;
        for (int k = 0; k < RADIAL_LINES; k++) {
            double a = 2.0 * Math.PI * k / RADIAL_LINES;
            v[c++] = RADAR_CENTER;
            v[c++] = RADAR_CENTER;
            v[c++] = 0f;
            v[c++] = RADAR_CENTER + (float) (RADAR_RADIUS * Math.cos(a));
            v[c++] = RADAR_CENTER + (float) (RADAR_RADIUS * Math.sin(a));
            v[c++] = 0f;
        }
        return v;
    }

    /** Birim dortgen cercevesi (-1..1): minimap gorunur alan dortgeni. */
    private static final int RECT_VERTEX_COUNT = 4;
    private static final float[] RECT_VERTICES = {
        -1f, -1f, 0f,
         1f, -1f, 0f,
         1f,  1f, 0f,
        -1f,  1f, 0f
    };

    /** Tek renkli vertex renk dizisi. */
    private static float[] solidColor(int count, float[] rgb) {
        float[] c = new float[count * 3];
        for (int i = 0; i < count; i++) {
            c[i * 3]     = rgb[0];
            c[i * 3 + 1] = rgb[1];
            c[i * 3 + 2] = rgb[2];
        }
        return c;
    }

    // ---------------- POLAR DONUSUM ----------------

    /** y (0..1000) -> dunya yaricapi (0..RADAR_RADIUS). */
    private static float dataToRadius(float y) {
        return y / SCREEN_RESOLUTION * RADAR_RADIUS;
    }

    /** x (0..1000) -> aci (radyan, 0..2π). */
    private static float dataToAngle(float x) {
        return x / SCREEN_RESOLUTION * (float) (2.0 * Math.PI);
    }

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

    // ================================================================

    private static GLCapabilities caps() {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(MSAA_SAMPLES);
        return capabilities;
    }

    public yedek(int frequency, int count) {
        super(caps());
        this.model = new Simulator(count);
        this.shipCount = count;
        this.maxTargetVerts = Math.max(1, count) * TARGET_VERTS;
        setPreferredSize(new Dimension(SCREEN_RESOLUTION, SCREEN_RESOLUTION));
        addGLEventListener(this);
        installInputHandlers();
        executeGraph(frequency);
    }

    private void installInputHandlers() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                if (event.getButton() != MouseEvent.BUTTON1) return;

                if (isInsideMinimap(event.getX(), event.getY())) {
                    minimapDragging = true;
                    navigateToMinimapPoint(event.getX(), event.getY());
                    return;
                }

                pressX = event.getX();
                pressY = event.getY();
                lastDragX = pressX;
                lastDragY = pressY;
                dragging = false;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) return;

                if (minimapDragging) {
                    minimapDragging = false;
                    return;
                }

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
                applyZoom(event.getX(), event.getY(), rotation < 0.0);
            }
        };

        MouseMotionAdapter motionHandler = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (minimapDragging) {
                    navigateToMinimapPoint(event.getX(), event.getY());
                    return;
                }

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
                } else if (event.getKeyCode() == KeyEvent.VK_TAB) {
                    minimapVisible = !minimapVisible;
                } else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
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

    // ---------------- ZOOM ----------------

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

    // ---------------- MINIMAP ETKILESIMI ----------------

    private boolean isInsideMinimap(int screenX, int screenY) {
        if (!minimapVisible) return false;

        int minimapWidth = Math.round(getWidth() * MINIMAP_FRACTION);
        int minimapHeight = Math.round(getHeight() * MINIMAP_FRACTION);

        return screenX >= 0 && screenX < minimapWidth
            && screenY >= 0 && screenY < minimapHeight;
    }

    private void navigateToMinimapPoint(int screenX, int screenY) {
        int minimapWidth = Math.round(getWidth() * MINIMAP_FRACTION);
        int minimapHeight = Math.round(getHeight() * MINIMAP_FRACTION);

        float fractionX = (float) screenX / minimapWidth;
        float fractionY = 1f - (float) screenY / minimapHeight;

        centerViewOn(fractionX * SCREEN_RESOLUTION, fractionY * SCREEN_RESOLUTION);
    }

    private void centerViewOn(float targetX, float targetY) {
        float rangeX = viewMaxX - viewMinX;
        float rangeY = viewMaxY - viewMinY;

        float[] centeredX = shiftRange(viewMinX, viewMaxX,
                                       targetX - (viewMinX + rangeX / 2f));
        viewMinX = centeredX[0];
        viewMaxX = centeredX[1];

        float[] centeredY = shiftRange(viewMinY, viewMaxY,
                                       targetY - (viewMinY + rangeY / 2f));
        viewMinY = centeredY[0];
        viewMaxY = centeredY[1];
    }

    // ---------------- KOORDINAT DONUSUMU ----------------

    private float screenToWorldX(int screenX) {
        float fraction = (float) screenX / getWidth();
        return viewMinX + fraction * (viewMaxX - viewMinX);
    }

    private float screenToWorldY(int screenY) {
        float fraction = 1f - (float) screenY / getHeight();
        return viewMinY + fraction * (viewMaxY - viewMinY);
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

    public void executeGraph(int frequency) {
        if (frequency <= 0) return;

        scanY = 0f;
        lastTimeNs = 0L;
        detected.clear();

        if (animator == null) {
            animator = new FPSAnimator(this, frequency);
            animator.start();
            return;
        }
        if (animator.isStarted()) animator.stop();
        animator.setFPS(frequency);
        animator.start();
    }

    public void stopGraph() {
        if (animator != null && animator.isStarted()) animator.stop();
    }

    // ---------------- INIT ----------------

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

        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);
        gl.glUseProgram(shaderProgram);

        gl.glGenBuffers(vbo.length, vbo, 0);

        uploadBuffer(gl, vbo[0], unitCircleOutline());
        uploadBuffer(gl, vbo[1], unitDisc());
        uploadBuffer(gl, vbo[2], radialGridPositions());
        uploadBuffer(gl, vbo[3], RECT_VERTICES);

        uploadBuffer(gl, vbo[4], solidColor(CIRCLE_SEG, GRID_COLOR));
        uploadBuffer(gl, vbo[5], solidColor(CIRCLE_SEG, SCAN_COLOR));
        uploadBuffer(gl, vbo[6], solidColor(CIRCLE_SEG, MARK_COLOR));
        uploadBuffer(gl, vbo[7], solidColor(RECT_VERTEX_COUNT, RECT_COLOR));
        uploadBuffer(gl, vbo[8], solidColor(DISC_SEG + 2, TARGET_COLOR));

        // vbo[9] hedef pozisyonlari her karede yuklenecek (dinamik).
        // vbo[10] hedef renkleri sabit amber; bir kez yukle, alt kumesini cizeriz.
        uploadBuffer(gl, vbo[10], solidColor(maxTargetVerts, TARGET_COLOR));

        attribPosition = gl.glGetAttribLocation(shaderProgram, "inPosition");
        gl.glEnableVertexAttribArray(attribPosition);
        bindPositionBuffer(gl, vbo[0]);

        attribColor = gl.glGetAttribLocation(shaderProgram, "inColor");
        gl.glEnableVertexAttribArray(attribColor);
        bindColorBuffer(gl, vbo[4]);

        uniformMatrix = gl.glGetUniformLocation(shaderProgram, "matrix");

        gl.glClearColor(BG_R, BG_G, BG_B, 1f);

        gl.glEnable(GL.GL_MULTISAMPLE);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glLineWidth(GL_LINE_WIDTH);
    }

    private static void uploadBuffer(GL2 gl, int bufferId, float[] data) {
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        (long) data.length * Float.BYTES, buffer, GL.GL_STATIC_DRAW);
    }

    private static void uploadDynamic(GL2 gl, int bufferId, float[] data) {
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        (long) data.length * Float.BYTES, buffer, GL.GL_DYNAMIC_DRAW);
    }

    private void bindPositionBuffer(GL2 gl, int bufferId) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribPosition, 3, GL.GL_FLOAT, false, 0, 0L);
    }

    private void bindColorBuffer(GL2 gl, int bufferId) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glVertexAttribPointer(attribColor, 3, GL.GL_FLOAT, false, 0, 0L);
    }

    // ---------------- MATRIS ----------------

    /**
     * Birim geometriyi (-1..1) verilen aralik icinde (centerX,centerY) konumuna,
     * (widthWorld x heightWorld) dunya boyutunda yerlestirir. Birim cember/disk/kare icin.
     */
    private static void setRangeMatrix(float[] matrix, float centerX, float centerY,
                                       float widthWorld, float heightWorld,
                                       float minX, float maxX, float minY, float maxY) {
        float rangeX = maxX - minX;
        float rangeY = maxY - minY;

        float translateX = 2f * (centerX - minX) / rangeX - 1f;
        float translateY = 2f * (centerY - minY) / rangeY - 1f;
        float scaleX = widthWorld / rangeX;
        float scaleY = heightWorld / rangeY;

        Arrays.fill(matrix, 0f);
        matrix[0]  = scaleX;
        matrix[5]  = scaleY;
        matrix[10] = 1f;
        matrix[12] = translateX;
        matrix[13] = translateY;
        matrix[15] = 1f;
    }

    /** Ana ekran: gorunur aralik (zoom/pan burada). Birim geometri icin. */
    private void setModelMatrix(float[] matrix, float centerX, float centerY,
                                float widthWorld, float heightWorld) {
        setRangeMatrix(matrix, centerX, centerY, widthWorld, heightWorld,
                       viewMinX, viewMaxX, viewMinY, viewMaxY);
    }

    /** Minimap: dunyanin tamami. Birim geometri icin. */
    private static void setWorldMatrix(float[] matrix, float centerX, float centerY,
                                       float widthWorld, float heightWorld) {
        setRangeMatrix(matrix, centerX, centerY, widthWorld, heightWorld,
                       0f, SCREEN_RESOLUTION, 0f, SCREEN_RESOLUTION);
    }

    /** Ham dunya koordinatlarini gorunur araliga gore NDC'ye tasir (hedef yaylari, radyal cizgiler). */
    private void setViewMatrix(float[] matrix) {
        setRangeToNdc(matrix, viewMinX, viewMaxX, viewMinY, viewMaxY);
    }

    private static void setRangeToNdc(float[] matrix, float minX, float maxX, float minY, float maxY) {
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

    // ---------------- TARAMA ----------------

    /** Cemberi (yaricapi) gecen zamana gore buyutur; gectigi bandda kalan hedefleri tespit eder. */
    private void advanceScan() {
        final float resolution = SCREEN_RESOLUTION;

        long now = System.nanoTime();
        if (lastTimeNs == 0L) {
            lastTimeNs = now;
            return;
        }
        float deltaSec = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;

        float bandStart = scanY;
        float bandEnd   = scanY + (resolution * deltaSec) / SCAN_PERIOD_SEC;

        if (bandEnd >= resolution) {
            detected.clear();
            bandEnd = bandEnd % resolution;
            bandStart = 0f;
        }

        int[][] snapshot = model.getSnapshot();
        for (int shipIndex = 0; shipIndex < snapshot.length; shipIndex++) {
            float shipY = snapshot[shipIndex][1];
            if (shipY >= bandStart && shipY < bandEnd) {
                detected.add(new int[] { snapshot[shipIndex][0], snapshot[shipIndex][1] });
            }
        }

        scanY = bandEnd;
    }

    // ---------------- HEDEF YAYLARI ----------------

    /** Tespit edilen her hedefi bir halka dilimi (annular sector) olarak dunya koordinatlarinda uretir. */
    private float[] buildTargetSectorPositions() {
        int n = detected.size();
        float[] v = new float[n * TARGET_VERTS * 3];
        int c = 0;

        float halfAngle = TARGET_ANGULAR_SPAN / 2f;
        float halfThick = TARGET_RADIAL_THICKNESS / 2f;

        for (int t = 0; t < n; t++) {
            int[] tg = detected.get(t);
            float theta = dataToAngle(tg[0]);
            float rho   = dataToRadius(tg[1]);

            float inner = rho - halfThick;
            if (inner < 0f) inner = 0f;
            float outer = rho + halfThick;

            for (int s = 0; s < TARGET_ARC_SEGMENTS; s++) {
                float a0 = theta - halfAngle + TARGET_ANGULAR_SPAN * s / TARGET_ARC_SEGMENTS;
                float a1 = theta - halfAngle + TARGET_ANGULAR_SPAN * (s + 1) / TARGET_ARC_SEGMENTS;

                float cos0 = (float) Math.cos(a0), sin0 = (float) Math.sin(a0);
                float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);

                float ix0 = RADAR_CENTER + inner * cos0, iy0 = RADAR_CENTER + inner * sin0;
                float ox0 = RADAR_CENTER + outer * cos0, oy0 = RADAR_CENTER + outer * sin0;
                float ix1 = RADAR_CENTER + inner * cos1, iy1 = RADAR_CENTER + inner * sin1;
                float ox1 = RADAR_CENTER + outer * cos1, oy1 = RADAR_CENTER + outer * sin1;

                c = putVert(v, c, ix0, iy0);
                c = putVert(v, c, ox0, oy0);
                c = putVert(v, c, ox1, oy1);

                c = putVert(v, c, ix0, iy0);
                c = putVert(v, c, ox1, oy1);
                c = putVert(v, c, ix1, iy1);
            }
        }
        return v;
    }

    private static int putVert(float[] v, int c, float x, float y) {
        v[c++] = x;
        v[c++] = y;
        v[c++] = 0f;
        return c;
    }

    // ---------------- MINIMAP ----------------

    private void drawMinimap(GL2 gl, int surfaceWidth, int surfaceHeight) {
        int minimapWidth = Math.round(surfaceWidth * MINIMAP_FRACTION);
        int minimapHeight = Math.round(surfaceHeight * MINIMAP_FRACTION);
        int minimapX = 0;
        int minimapY = surfaceHeight - minimapHeight;

        gl.glViewport(minimapX, minimapY, minimapWidth, minimapHeight);

        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(minimapX, minimapY, minimapWidth, minimapHeight);
        gl.glClearColor(MINIMAP_BG_R, MINIMAP_BG_G, MINIMAP_BG_B, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glClearColor(BG_R, BG_G, BG_B, 1f);

        // Mesafe halkalari (dim)
        gl.glLineWidth(1f);
        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[4]);
        for (float ring : GRID_RING_RADII) {
            setWorldMatrix(modelMatrix, RADAR_CENTER, RADAR_CENTER, 2f * ring, 2f * ring);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_SEG);
        }

        // Hedef blipleri (dolu disk)
        bindPositionBuffer(gl, vbo[1]);
        bindColorBuffer(gl, vbo[8]);
        for (int t = 0; t < detected.size(); t++) {
            int[] tg = detected.get(t);
            float th = dataToAngle(tg[0]);
            float r  = dataToRadius(tg[1]);
            float px = RADAR_CENTER + r * (float) Math.cos(th);
            float py = RADAR_CENTER + r * (float) Math.sin(th);
            setWorldMatrix(modelMatrix, px, py, MINIMAP_BLIP_SIZE, MINIMAP_BLIP_SIZE);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, DISC_SEG + 2);
        }

        // Tarama cemberi
        float scanRadius = dataToRadius(scanY);
        if (scanRadius > 0.5f) {
            gl.glLineWidth(2f);
            bindPositionBuffer(gl, vbo[0]);
            bindColorBuffer(gl, vbo[5]);
            setWorldMatrix(modelMatrix, RADAR_CENTER, RADAR_CENTER, 2f * scanRadius, 2f * scanRadius);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_SEG);
        }

        // Gorunur alan dortgeni
        gl.glLineWidth(1.5f);
        bindPositionBuffer(gl, vbo[3]);
        bindColorBuffer(gl, vbo[7]);

        float viewCenterX = (viewMinX + viewMaxX) / 2f;
        float viewCenterY = (viewMinY + viewMaxY) / 2f;
        float viewWidth  = (viewMaxX - viewMinX) - 2f * MINIMAP_RECT_INSET;
        float viewHeight = (viewMaxY - viewMinY) - 2f * MINIMAP_RECT_INSET;
        if (viewWidth < 2f) viewWidth = 2f;
        if (viewHeight < 2f) viewHeight = 2f;

        setWorldMatrix(modelMatrix, viewCenterX, viewCenterY, viewWidth, viewHeight);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, RECT_VERTEX_COUNT);

        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
        gl.glLineWidth(GL_LINE_WIDTH);
    }

    // ---------------- DISPLAY ----------------

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        advanceScan();

        // ---- GRID: mesafe halkalari ----
        gl.glLineWidth(GRID_LINE_WIDTH);
        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[4]);
        for (float ring : GRID_RING_RADII) {
            setModelMatrix(modelMatrix, RADAR_CENTER, RADAR_CENTER, 2f * ring, 2f * ring);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_SEG);
        }

        // ---- GRID: radyal cizgiler (her 36 derece) ----
        bindPositionBuffer(gl, vbo[2]);
        bindColorBuffer(gl, vbo[4]);
        setViewMatrix(modelMatrix);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_LINES, 0, RADIAL_LINES * 2);

        // ---- HEDEFLER (halka dilimleri) ----
        if (!detected.isEmpty()) {
            float[] pos = buildTargetSectorPositions();
            uploadDynamic(gl, vbo[9], pos);
            bindPositionBuffer(gl, vbo[9]);
            bindColorBuffer(gl, vbo[10]);
            setViewMatrix(modelMatrix);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, detected.size() * TARGET_VERTS);
        }

        // ---- TARAMA CEMBERI (merkezden buyur) ----
        float scanRadius = dataToRadius(scanY);
        if (scanRadius > 0.5f) {
            gl.glLineWidth(SCAN_LINE_WIDTH);
            bindPositionBuffer(gl, vbo[0]);
            bindColorBuffer(gl, vbo[5]);
            setModelMatrix(modelMatrix, RADAR_CENTER, RADAR_CENTER, 2f * scanRadius, 2f * scanRadius);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_SEG);
        }

        // ---- ISARETLER ----
        gl.glLineWidth(GL_LINE_WIDTH);
        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[6]);
        for (int markIndex = 0; markIndex < marks.size(); markIndex++) {
            float[] mark = marks.get(markIndex);
            float radius = (markIndex == selectedMark) ? MARK_RADIUS * 1.15f : MARK_RADIUS;
            setModelMatrix(modelMatrix, mark[0], mark[1], radius, radius);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, CIRCLE_SEG);
        }

        if (minimapVisible) {
            drawMinimap(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glDisableVertexAttribArray(attribPosition);
        gl.glDisableVertexAttribArray(attribColor);
        gl.glDeleteBuffers(vbo.length, vbo, 0);
        gl.glDeleteProgram(shaderProgram);
        stopGraph();
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
