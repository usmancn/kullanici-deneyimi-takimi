package deneme.radar;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import deneme.radar.gl.ShaderProgram;
import deneme.sim.GainFilter;
import deneme.sim.Simulation;

/**
 * Parcalara ayrilmis radarin birlestirildigi ana bilesen.
 * Shader, kamera, tum katmanlar (grid/hedef/tarama/isaret/minimap/etiket),
 * kontrolculer (zoom/pan/isaret/minimap) ve simulasyon modeli burada bir araya gelir.
 *
 * GLEventListener yasam dongusu: init -> display (her kare) -> reshape -> dispose.
 */
public class RadarCanvas extends GLCanvas implements GLEventListener {

    private static final int MSAA_SAMPLES = 8;
    private static final float GL_LINE_WIDTH = 2f;

    /** Koyu yesil arka plan. */
    private static final float BG_R = 0.02f, BG_G = 0.15f, BG_B = 0.08f;

    private final ShaderProgram shader = new ShaderProgram();
    private final Camera camera = new Camera();

    private final Simulation model;
    private final GainFilter filter;

    // paylasilan geometri
    private final TargetGeometry targetGeometry = new TargetGeometry();

    // katmanlar
    private final GridLayer   grid;
    private final TargetLayer targets;
    private final ScanLine    scan;
    private final MarkLayer   markLayer;
    private final Minimap     minimap;
    private final LabelLayer  labels = new LabelLayer();

    // kontrolculer
    private final ZoomController    zoom;
    private final PanController     pan;
    private final MarkController    markController = new MarkController();
    private final MinimapController minimapController;

    private volatile boolean minimapVisible = true;

    private FPSAnimator animator;

    private static GLCapabilities caps() {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(MSAA_SAMPLES);
        return capabilities;
    }

    public RadarCanvas(int frequency, Simulation model, GainFilter filter) {
        super(caps());
        this.model = model;
        this.filter = filter;

        this.grid      = new GridLayer();
        this.targets   = new TargetLayer(targetGeometry);
        this.scan      = new ScanLine(targetGeometry, model, filter);
        this.markLayer = new MarkLayer(markController);
        this.minimap   = new Minimap(targetGeometry);

        this.zoom = new ZoomController(camera);
        this.pan  = new PanController(camera);
        this.minimapController = new MinimapController(camera);

        setPreferredSize(new Dimension((int) Camera.WORLD_SIZE, (int) Camera.WORLD_SIZE));
        addGLEventListener(this);
        installInputHandlers();
        start(frequency);
    }

    // ---------------- GLEventListener ----------------

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        System.out.println("GL_VERSION : " + gl.glGetString(GL.GL_VERSION));

        shader.init(gl);

        targetGeometry.init(gl);
        grid.init(gl);
        markLayer.init(gl);
        minimap.init(gl);
        labels.init(gl);

        gl.glClearColor(BG_R, BG_G, BG_B, 1f);
        gl.glEnable(GL.GL_MULTISAMPLE);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glLineWidth(GL_LINE_WIDTH);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        scan.advance();

        grid.draw(gl, shader);
        targets.draw(gl, shader, camera, scan.detected(), filter);
        scan.draw(gl, shader, camera);
        markLayer.draw(gl, shader, camera);
        labels.draw(gl, shader, camera, getWidth(), getHeight());

        if (minimapVisible) {
            minimap.draw(gl, shader, camera, scan.detected(), filter,
                         scan.scanY(), drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        grid.dispose(gl);
        markLayer.dispose(gl);
        minimap.dispose(gl);
        targetGeometry.dispose(gl);
        labels.dispose(gl);
        shader.dispose(gl);
        stop();
    }

    // ---------------- animasyon ----------------

    public void start(int frequency) {
        if (frequency <= 0) return;
        scan.reset();
        if (animator == null) {
            animator = new FPSAnimator(this, frequency);
            animator.start();
            return;
        }
        if (animator.isStarted()) animator.stop();
        animator.setFPS(frequency);
        animator.start();
    }

    public void stop() {
        if (animator != null && animator.isStarted()) animator.stop();
    }

    // ---------------- girdi ----------------

    private void installInputHandlers() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);   // TAB'i minimap icin serbest birak

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                if (event.getButton() != MouseEvent.BUTTON1) return;

                if (minimapController.isInside(event.getX(), event.getY(),
                        getWidth(), getHeight(), minimapVisible)) {
                    minimapController.setDragging(true);
                    minimapController.navigate(event.getX(), event.getY(), getWidth(), getHeight());
                    return;
                }
                pan.press(event.getX(), event.getY());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) return;

                if (minimapController.isDragging()) { minimapController.setDragging(false); return; }
                if (pan.isDragging())               { pan.release(); return; }

                float worldX = camera.screenToWorldX(event.getX(), getWidth());
                float worldY = camera.screenToWorldY(event.getY(), getHeight());
                if (markController.isMarkKeyDown()) markController.add(worldX, worldY);
                else                                markController.selectAt(worldX, worldY);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                requestFocusInWindow();
                double rotation = event.getPreciseWheelRotation();
                if (rotation == 0.0) return;
                zoom.zoom(event.getX(), event.getY(), getWidth(), getHeight(), rotation < 0.0);
            }
        };

        MouseMotionAdapter motionHandler = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (minimapController.isDragging()) {
                    minimapController.navigate(event.getX(), event.getY(), getWidth(), getHeight());
                    return;
                }
                if (markController.isMarkKeyDown()) return;
                pan.drag(event.getX(), event.getY(), getWidth(), getHeight());
            }
        };

        addMouseListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        addMouseMotionListener(motionHandler);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_M) {
                    markController.setMarkKeyDown(true);
                } else if (event.getKeyCode() == KeyEvent.VK_TAB) {
                    minimapVisible = !minimapVisible;
                } else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    markController.removeSelected();
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_M) {
                    markController.setMarkKeyDown(false);
                }
            }
        });
    }
}
