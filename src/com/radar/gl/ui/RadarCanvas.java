package com.radar.gl.ui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.radar.sim.engine.EntityManager;
import com.radar.gl.core.Camera;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.gl.layers.GridLayer;
import com.radar.gl.layers.LabelLayer;
import com.radar.gl.layers.MarkLayer;
import com.radar.gl.layers.ScanLine;
import com.radar.gl.layers.TargetLayer;
import com.radar.graphs.IGraph;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

/**
 * Fatih'in VBO/GLCanvas tabanli radar ekrani.
 * Osman'in {@link EntityManager}'i ile konusarak varlik verilerini alir.
 *
 * GLEventListener yasam dongusu: init → display (her kare) → reshape → dispose.
 */
@SuppressWarnings("serial")
public class RadarCanvas extends GLCanvas implements GLEventListener, IGraph {

    private static final int   MSAA_SAMPLES  = 8;
    private static final float GL_LINE_WIDTH  = 2f;

    private static final float BG_R = 0.02f, BG_G = 0.15f, BG_B = 0.08f;

    private final ShaderProgram  shader         = new ShaderProgram();
    private final Camera         camera         = new Camera();
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
    private final MarkController    markController    = new MarkController();
    private final MinimapController minimapController;

    private volatile boolean minimapVisible = true;
    private FPSAnimator animator;

    private static GLCapabilities caps() {
        GLCapabilities c = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        c.setSampleBuffers(true);
        c.setNumSamples(MSAA_SAMPLES);
        return c;
    }

    /**
     * @param frequency    Hedef kare hizi (FPS).
     * @param entityManager Osman'in varlik yoneticisi; null olamaz.
     */
    public RadarCanvas(int frequency, EntityManager entityManager) {
        super(caps());
        if (entityManager == null) throw new IllegalArgumentException("EntityManager null olamaz.");

        this.grid     = new GridLayer();
        this.targets  = new TargetLayer(targetGeometry);
        this.scan     = new ScanLine(targetGeometry, entityManager);
        this.markLayer = new MarkLayer(markController);
        this.minimap  = new Minimap(targetGeometry);

        this.zoom              = new ZoomController(camera);
        this.pan               = new PanController(camera);
        this.minimapController = new MinimapController(camera);

        setPreferredSize(new Dimension((int) Camera.WORLD_SIZE, (int) Camera.WORLD_SIZE));
        addGLEventListener(this);
        installInputHandlers();
        start(frequency);
    }

    // ------------------------------------------------------------------ //
    // GLEventListener
    // ------------------------------------------------------------------ //

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
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
        targets.draw(gl, shader, camera, scan.detected());
        scan.draw(gl, shader, camera);
        markLayer.draw(gl, shader, camera);
        labels.draw(gl, shader, camera, getWidth(), getHeight());

        if (minimapVisible) {
            minimap.draw(gl, shader, camera, scan.detected(), scan.scanY(),
                         drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        drawable.getGL().glViewport(0, 0, w, h);
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

    // ------------------------------------------------------------------ //
    // IGraph
    // ------------------------------------------------------------------ //

    @Override
    public void startGraph() { start(60); }

    @Override
    public void stopGraph()  { stop(); }

    // ------------------------------------------------------------------ //
    // Animasyon yaşam döngüsü
    // ------------------------------------------------------------------ //

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

    public void pause() {
        if (animator != null && animator.isAnimating()) animator.pause();
    }

    public void resume() {
        if (animator != null && animator.isPaused()) animator.resume();
    }

    // ------------------------------------------------------------------ //
    // Girdi yönetimi
    // ------------------------------------------------------------------ //

    private void installInputHandlers() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (e.getButton() != MouseEvent.BUTTON1) return;
                if (minimapController.isInside(e.getX(), e.getY(), getWidth(), getHeight(), minimapVisible)) {
                    minimapController.setDragging(true);
                    minimapController.navigate(e.getX(), e.getY(), getWidth(), getHeight());
                    return;
                }
                pan.press(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                if (minimapController.isDragging()) { minimapController.setDragging(false); return; }
                if (pan.isDragging())               { pan.release(); return; }
                float wx = camera.screenToWorldX(e.getX(), getWidth());
                float wy = camera.screenToWorldY(e.getY(), getHeight());
                if (markController.isMarkKeyDown()) markController.add(wx, wy);
                else                                markController.selectAt(wx, wy);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                requestFocusInWindow();
                double r = e.getPreciseWheelRotation();
                if (r == 0.0) return;
                zoom.zoom(e.getX(), e.getY(), getWidth(), getHeight(), r < 0.0);
            }
        };

        addMouseListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (minimapController.isDragging()) {
                    minimapController.navigate(e.getX(), e.getY(), getWidth(), getHeight());
                    return;
                }
                if (markController.isMarkKeyDown()) return;
                pan.drag(e.getX(), e.getY(), getWidth(), getHeight());
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if      (e.getKeyCode() == KeyEvent.VK_M)         markController.setMarkKeyDown(true);
                else if (e.getKeyCode() == KeyEvent.VK_TAB)       minimapVisible = !minimapVisible;
                else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) markController.removeSelected();
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_M) markController.setMarkKeyDown(false);
            }
        });
    }
}
