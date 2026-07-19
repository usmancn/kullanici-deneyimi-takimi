package deneme;

import java.awt.Dimension;
import java.awt.Font;
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
import com.jogamp.opengl.util.awt.TextRenderer;

public class FirstGraph extends GLCanvas implements GLEventListener {

    private FPSAnimator animator;
    private static final int SCREEN_RESOLUTION = 1000;

    private static final float SQUARE_SIZE = 10f;

    /** Bir tam taramanin suresi, saniye.*/
    private static final float SCAN_PERIOD_SEC = 10f;

    /** Tarama cizgisinin kalinligi, piksel. */
    private static final float LINE_THICKNESS = 3f;

    /** Cizgi kalinligi */
    private static final float GL_LINE_WIDTH = 2f;

    /** MSAA ornek sayisi. */
    private static final int MSAA_SAMPLES = 8;

    /** Isaret cemberinin yarıcapi, dunya birimi. */
    private static final float MARK_RADIUS = 20f;
    private static final int MARK_SEGMENTS = 64;

    /** Bir tiklamanin isarete sayilmasi icin gereken tolerans, dunya birimi. */
    private static final float PICK_TOLERANCE = 15f;

    /** Tiklama ile suruklemeyi ayiran esik, piksel. */
    private static final int DRAG_THRESHOLD = 5;

    /** Grid cizgilerinin ekrandaki oransal konumlari. Zoom'dan bagimsiz, sabit. */
    private static final float[] GRID_FRACTIONS = { 0f, 0.25f, 0.5f, 0.75f, 1f };

    /** Etiketlerin cizgiden uzakligi, piksel. */
    private static final int LABEL_PADDING = 6;

    private static final int LABEL_FONT_SIZE = 14;

    /** X ekseni etiketleri en altta, Y ekseni etiketleri onlarin ustunde ayri seritte. */
    private static final int X_LABEL_BASELINE = 6;
    private static final int Y_LABEL_LEFT = 6;
    private static final int Y_LABEL_MIN_BASELINE = 26;

    /** Minimap ekranin bu oraninda; sol ustte durur, TAB ile acilip kapanir. */
    private static final float MINIMAP_FRACTION = 0.25f;

    private static final float MINIMAP_SQUARE_SIZE = 10f;

    /** Gorunur alan dortgeni kenara dayaninca kirpilmasin diye iceri cekilir. */
    private static final float MINIMAP_RECT_INSET = 6f;

    /** Minimap zemini: ana zeminden biraz koyu. */
    private static final float MINIMAP_BG_R = 0.01f;
    private static final float MINIMAP_BG_G = 0.09f;
    private static final float MINIMAP_BG_B = 0.05f;

    /** Zoom bolge sinirlari: bu oranin altinda alt/sol, ustunde ust/sag kenar sabitlenir. */
    private static final float ZONE_LOW = 0.4f;
    private static final float ZONE_HIGH = 0.6f;

    /** Tekerlegin bir tiklamasinda araligin carpani. */
    private static final float ZOOM_STEP = 1.15f;

    /** En fazla yakinlasma: gorunur aralik bu degerin altina inemez. */
    private static final float MIN_VIEW_RANGE = 40f;

    private static final int ANCHOR_LOW = -1;
    private static final int ANCHOR_CENTER = 0;
    private static final int ANCHOR_HIGH = 1;

    /** Koyu yesil arka plan */
    private static final float BG_R = 0.02f;
    private static final float BG_G = 0.15f;
    private static final float BG_B = 0.08f;

    private final Simulator model;

    private int shaderProgram;
    private final int[] vbo = new int[9];   // 0=pozisyon, 1=renk, 2=yesil, 3=cember pozisyon, 4=cember renk, 5=grid pozisyon, 6=grid renk, 7=dortgen pozisyon, 8=dortgen renk
    private int attribPosition;
    private int attribColor;
    private int uniformMatrix;

    private TextRenderer textRenderer;

    private final float[] modelMatrix = new float[16];

    /** Gorunur dunya araligi. Zoom ve pan sadece bunlari degistirir. */
    private volatile float viewMinX = 0f;
    private volatile float viewMaxX = SCREEN_RESOLUTION;
    private volatile float viewMinY = 0f;
    private volatile float viewMaxY = SCREEN_RESOLUTION;

    /** Minimap acik mi. */
    private volatile boolean minimapVisible = true;

    /** Suruklemenin baslangic noktasi ve son fare konumu. */
    private int pressX = 0;
    private int pressY = 0;
    private int lastDragX = 0;
    private int lastDragY = 0;
    private boolean dragging = false;

    /** Minimap uzerinde tiklama/surukleme yapiliyor mu. */
    private boolean minimapDragging = false;

    /** Tarama cizgisinin anlik y konumu ve zaman referansi. */
    private float scanY = 0f;
    private long lastTimeNs = 0L;

    /** Cizginin bu turda gordugu hedefler. */
    private final List<int[]> detected = new ArrayList<>();

    /** Kullanicinin koydugu isaretler: {x, y}. */
    private final List<float[]> marks = new CopyOnWriteArrayList<>();

    /** Secili isaretin indeksi; -1 ise secim yok. */
    private volatile int selectedMark = -1;

    /** M tusu basili mi. */
    private volatile boolean markKeyDown = false;

    // 8 ucgen = 24 vertex
    private static final float[] VERTICES = {
         0f,  0f, 0f,   -1f, -1f, 0f,    0f, -1f, 0f,
         0f,  0f, 0f,    0f, -1f, 0f,    1f, -1f, 0f,
         0f,  0f, 0f,    1f, -1f, 0f,    1f,  0f, 0f,
         0f,  0f, 0f,    1f,  0f, 0f,    1f,  1f, 0f,
         0f,  0f, 0f,    1f,  1f, 0f,    0f,  1f, 0f,
         0f,  0f, 0f,    0f,  1f, 0f,   -1f,  1f, 0f,
         0f,  0f, 0f,   -1f,  1f, 0f,   -1f,  0f, 0f,
         0f,  0f, 0f,   -1f,  0f, 0f,   -1f, -1f, 0f
    };

    private static final int VERTEX_COUNT = 24;

    /** Hedefler: merkez parlak, kenarlar koyu zemine kaynayan yesil. */
    private static final float[] COLORS = targetColors();

    private static float[] targetColors() {
        float[] colors = new float[VERTEX_COUNT * 3];
        for (int vertexIndex = 0; vertexIndex < VERTEX_COUNT; vertexIndex++) {
            boolean isCenter = (vertexIndex % 3 == 0);
            colors[vertexIndex * 3]     = isCenter ? 1.00f : 0.30f;
            colors[vertexIndex * 3 + 1] = isCenter ? 0.35f : 0.02f;
            colors[vertexIndex * 3 + 2] = isCenter ? 0.25f : 0.02f;
        }
        return colors;
    }

    /** Tarama cizgisi icin: 24 vertex, hepsi parlak yesil. */
    private static final float[] GREEN_COLORS = greenColors();

    private static float[] greenColors() {
        float[] colors = new float[VERTEX_COUNT * 3];
        for (int vertexIndex = 0; vertexIndex < VERTEX_COUNT; vertexIndex++) {
            colors[vertexIndex * 3]     = 0.3f;
            colors[vertexIndex * 3 + 1] = 1.0f;
            colors[vertexIndex * 3 + 2] = 0.4f;
        }
        return colors;
    }

    /** Isaret cemberi: birim cember, LINE_LOOP ile cizilir. */
    private static final float[] CIRCLE_VERTICES = circleVertices();

    private static float[] circleVertices() {
        float[] vertices = new float[MARK_SEGMENTS * 3];
        for (int segment = 0; segment < MARK_SEGMENTS; segment++) {
            double angle = 2.0 * Math.PI * segment / MARK_SEGMENTS;
            vertices[segment * 3]     = (float) Math.cos(angle);
            vertices[segment * 3 + 1] = (float) Math.sin(angle);
            vertices[segment * 3 + 2] = 0f;
        }
        return vertices;
    }

    /** Isaret rengi: sari. */
    private static final float[] CIRCLE_COLORS = circleColors();

    private static float[] circleColors() {
        float[] colors = new float[MARK_SEGMENTS * 3];
        for (int segment = 0; segment < MARK_SEGMENTS; segment++) {
            colors[segment * 3]     = 1.0f;
            colors[segment * 3 + 1] = 0.9f;
            colors[segment * 3 + 2] = 0.2f;
        }
        return colors;
    }

    /** Birim kare cercevesi (-1..1): minimap'teki gorunur alan dortgeni icin. */
    private static final int RECT_VERTEX_COUNT = 4;

    private static final float[] RECT_VERTICES = {
        -1f, -1f, 0f,
         1f, -1f, 0f,
         1f,  1f, 0f,
        -1f,  1f, 0f
    };

    /** Dortgen rengi: acik mavi, digerlerinden ayrilsin. */
    private static final float[] RECT_COLORS = {
        0.4f, 0.8f, 1f,
        0.4f, 0.8f, 1f,
        0.4f, 0.8f, 1f,
        0.4f, 0.8f, 1f
    };

    private static final int GRID_VERTEX_COUNT = GRID_FRACTIONS.length * 4;

    /**
     * Grid cizgileri dogrudan NDC'de (-1..1) uretiliyor; birim matrisle cizilecek.
     * Ekrana sabit civili: zoom yapilsa da yerleri degismez, sadece etiketleri degisir.
     */
    private static final float[] GRID_VERTICES = gridVertices();

    private static float[] gridVertices() {
        float[] vertices = new float[GRID_VERTEX_COUNT * 3];
        int cursor = 0;

        for (int index = 0; index < GRID_FRACTIONS.length; index++) {
            float ndc = 2f * GRID_FRACTIONS[index] - 1f;

            vertices[cursor++] = ndc;
            vertices[cursor++] = -1f;
            vertices[cursor++] = 0f;

            vertices[cursor++] = ndc;
            vertices[cursor++] = 1f;
            vertices[cursor++] = 0f;

            vertices[cursor++] = -1f;
            vertices[cursor++] = ndc;
            vertices[cursor++] = 0f;

            vertices[cursor++] = 1f;
            vertices[cursor++] = ndc;
            vertices[cursor++] = 0f;
        }
        return vertices;
    }

    /** Grid rengi: beyaz. */
    private static final float[] GRID_COLORS = gridColors();

    private static float[] gridColors() {
        float[] colors = new float[GRID_VERTEX_COUNT * 3];
        for (int vertexIndex = 0; vertexIndex < GRID_VERTEX_COUNT; vertexIndex++) {
            colors[vertexIndex * 3]     = 1f;
            colors[vertexIndex * 3 + 1] = 1f;
            colors[vertexIndex * 3 + 2] = 1f;
        }
        return colors;
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

    public FirstGraph(int frequency, int count) {
        super(caps());
        this.model = new Simulator(count);
        setPreferredSize(new Dimension(SCREEN_RESOLUTION, SCREEN_RESOLUTION));
        addGLEventListener(this);
        installInputHandlers();
        executeGraph(frequency);
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

                boolean zoomIn = rotation < 0.0;
                applyZoom(event.getX(), event.getY(), zoomIn);
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

    /** Fareyi takip edecek sekilde gorunur araligi kaydirir. */
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

    /** Araligi kaydirir, dunya sinirlarinin disina tasmasini engeller. */
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

    /** Tiklama minimap'in piksel dikdortgeninin icinde mi. */
    private boolean isInsideMinimap(int screenX, int screenY) {
        if (!minimapVisible) return false;

        int minimapWidth = Math.round(getWidth() * MINIMAP_FRACTION);
        int minimapHeight = Math.round(getHeight() * MINIMAP_FRACTION);

        return screenX >= 0 && screenX < minimapWidth
            && screenY >= 0 && screenY < minimapHeight;
    }

    /** Minimap pikselinden dunya koordinatina; minimap dunyanin tamamini gosterir. */
    private void navigateToMinimapPoint(int screenX, int screenY) {
        int minimapWidth = Math.round(getWidth() * MINIMAP_FRACTION);
        int minimapHeight = Math.round(getHeight() * MINIMAP_FRACTION);

        float fractionX = (float) screenX / minimapWidth;
        float fractionY = 1f - (float) screenY / minimapHeight;

        centerViewOn(fractionX * SCREEN_RESOLUTION, fractionY * SCREEN_RESOLUTION);
    }

    /** Gorunur araligi, genisligini koruyarak verilen noktaya ortalar. */
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

    /** Ekran pikselinden dunya koordinatina; gorunur araligi hesaba katar. */
    private float screenToWorldX(int screenX) {
        float fraction = (float) screenX / getWidth();
        return viewMinX + fraction * (viewMaxX - viewMinX);
    }

    /** Y ekseni ters: AWT yukaridan, OpenGL asagidan sayar. */
    private float screenToWorldY(int screenY) {
        float fraction = 1f - (float) screenY / getHeight();
        return viewMinY + fraction * (viewMaxY - viewMinY);
    }

    /** Cemberin kenarina yeterince yakin tiklama varsa o isaretin indeksi; yoksa -1. */
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

    //init
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

        gl.glGenBuffers(9, vbo, 0);

        uploadBuffer(gl, vbo[0], VERTICES);
        uploadBuffer(gl, vbo[1], COLORS);
        uploadBuffer(gl, vbo[2], GREEN_COLORS);
        uploadBuffer(gl, vbo[3], CIRCLE_VERTICES);
        uploadBuffer(gl, vbo[4], CIRCLE_COLORS);
        uploadBuffer(gl, vbo[5], GRID_VERTICES);
        uploadBuffer(gl, vbo[6], GRID_COLORS);
        uploadBuffer(gl, vbo[7], RECT_VERTICES);
        uploadBuffer(gl, vbo[8], RECT_COLORS);

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

        textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, LABEL_FONT_SIZE), true, true);
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

    /**
     * Dunya koordinatindan NDC'ye, verilen aralik icinde.
     * Geometriler -1..1 arasinda oldugu icin widthWorld/heightWorld tam boyuttur.
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
        matrix[0]  = scaleX;      // 1. sutun: x olcegi
        matrix[5]  = scaleY;      // 2. sutun: y olcegi
        matrix[10] = 1f;          // 3. sutun
        matrix[12] = translateX;  // 4. sutun: konum
        matrix[13] = translateY;
        matrix[15] = 1f;
    }

    /** Ana ekran: gorunur aralik, yani zoom ve pan burada uygulanir. */
    private void setModelMatrix(float[] matrix, float centerX, float centerY,
                                float widthWorld, float heightWorld) {
        setRangeMatrix(matrix, centerX, centerY, widthWorld, heightWorld,
                       viewMinX, viewMaxX, viewMinY, viewMaxY);
    }

    /** Minimap: dunyanin tamami, zoom'dan bagimsiz. */
    private static void setWorldMatrix(float[] matrix, float centerX, float centerY,
                                       float widthWorld, float heightWorld) {
        setRangeMatrix(matrix, centerX, centerY, widthWorld, heightWorld,
                       0f, SCREEN_RESOLUTION, 0f, SCREEN_RESOLUTION);
    }

    private static void setIdentityMatrix(float[] matrix) {
        Arrays.fill(matrix, 0f);
        matrix[0]  = 1f;
        matrix[5]  = 1f;
        matrix[10] = 1f;
        matrix[15] = 1f;
    }

    /** Cizgiyi gecen zamana gore ilerletir, gectigi bandda kalan hedefleri tespit listesine alir. */
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

    /** Tam sayiya yakinsa ondalik gosterme; zoom'da ara degerler icin bir basamak. */
    private static String formatLabel(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }

    /** Etiketleri fixed-function pipeline ile cizer; shader gecici olarak birakilir. */
    private void drawLabels(GL2 gl) {
        gl.glDisableVertexAttribArray(attribPosition);
        gl.glDisableVertexAttribArray(attribColor);
        gl.glUseProgram(0);

        int canvasWidth = getWidth();
        int canvasHeight = getHeight();

        float minX = viewMinX;
        float maxX = viewMaxX;
        float minY = viewMinY;
        float maxY = viewMaxY;

        textRenderer.beginRendering(canvasWidth, canvasHeight);
        textRenderer.setColor(1f, 1f, 1f, 1f);

        for (int index = 0; index < GRID_FRACTIONS.length; index++) {
            float fraction = GRID_FRACTIONS[index];

            float worldX = minX + fraction * (maxX - minX);
            float angleValue = worldX * 360f / SCREEN_RESOLUTION;
            String angleText = formatLabel(angleValue);

            int pixelX = Math.round(fraction * canvasWidth);
            int textWidth = Math.round((float) textRenderer.getBounds(angleText).getWidth());

            int labelX;
            if (fraction >= 1f) {
                labelX = pixelX - textWidth - LABEL_PADDING;
            } else {
                labelX = pixelX + LABEL_PADDING;
            }
            textRenderer.draw(angleText, labelX, X_LABEL_BASELINE);
        }

        for (int index = 0; index < GRID_FRACTIONS.length; index++) {
            float fraction = GRID_FRACTIONS[index];

            float worldY = minY + fraction * (maxY - minY);
            String yText = formatLabel(worldY);

            int pixelY = Math.round(fraction * canvasHeight);

            int labelY = pixelY + LABEL_PADDING;
            if (labelY < Y_LABEL_MIN_BASELINE) labelY = Y_LABEL_MIN_BASELINE;

            int maxBaseline = canvasHeight - LABEL_FONT_SIZE - 2;
            if (labelY > maxBaseline) labelY = maxBaseline;

            textRenderer.draw(yText, Y_LABEL_LEFT, labelY);
        }

        textRenderer.endRendering();

        gl.glUseProgram(shaderProgram);
        gl.glEnableVertexAttribArray(attribPosition);
        gl.glEnableVertexAttribArray(attribColor);
    }

    /**
     * Minimap: sol ustte, ayri viewport. Dunyanin tamamini gosterir,
     * uzerine gorunur alanin dortgenini cizer.
     */
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

        bindPositionBuffer(gl, vbo[0]);
        bindColorBuffer(gl, vbo[1]);
        for (int targetIndex = 0; targetIndex < detected.size(); targetIndex++) {
            int[] target = detected.get(targetIndex);
            setWorldMatrix(modelMatrix, target[0], target[1], MINIMAP_SQUARE_SIZE, MINIMAP_SQUARE_SIZE);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, VERTEX_COUNT);
        }

        bindColorBuffer(gl, vbo[2]);
        setWorldMatrix(modelMatrix, SCREEN_RESOLUTION / 2f, scanY,
                       SCREEN_RESOLUTION, LINE_THICKNESS * 2f);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, VERTEX_COUNT);

        bindPositionBuffer(gl, vbo[7]);
        bindColorBuffer(gl, vbo[8]);

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
    }

    //  display: cizgi asagidan yukari ilerler, gordugunu cizer.
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        advanceScan();

        bindPositionBuffer(gl, vbo[5]);
        bindColorBuffer(gl, vbo[6]);
        setIdentityMatrix(modelMatrix);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_LINES, 0, GRID_VERTEX_COUNT);

        bindPositionBuffer(gl, vbo[0]);

        bindColorBuffer(gl, vbo[1]);
        for (int targetIndex = 0; targetIndex < detected.size(); targetIndex++) {
            int[] target = detected.get(targetIndex);
            setModelMatrix(modelMatrix, target[0], target[1], SQUARE_SIZE, SQUARE_SIZE);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, VERTEX_COUNT);
        }

        float viewCenterX = (viewMinX + viewMaxX) / 2f;
        float viewWidth = viewMaxX - viewMinX;
        float scanThickness = LINE_THICKNESS * (viewMaxY - viewMinY) / SCREEN_RESOLUTION;

        bindColorBuffer(gl, vbo[2]);
        setModelMatrix(modelMatrix, viewCenterX, scanY, viewWidth, scanThickness);
        gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, VERTEX_COUNT);

        bindPositionBuffer(gl, vbo[3]);
        bindColorBuffer(gl, vbo[4]);
        for (int markIndex = 0; markIndex < marks.size(); markIndex++) {
            float[] mark = marks.get(markIndex);
            float radius = (markIndex == selectedMark) ? MARK_RADIUS * 1.15f : MARK_RADIUS;
            setModelMatrix(modelMatrix, mark[0], mark[1], radius, radius);
            gl.glUniformMatrix4fv(uniformMatrix, 1, false, modelMatrix, 0);
            gl.glDrawArrays(GL.GL_LINE_LOOP, 0, MARK_SEGMENTS);
        }

        drawLabels(gl);

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
        gl.glDeleteBuffers(9, vbo, 0);
        gl.glDeleteProgram(shaderProgram);
        if (textRenderer != null) textRenderer.dispose();
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