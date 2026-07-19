package com.radar.graphs;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.radar.config.SimulationConfig;
import com.radar.sim.engine.EntityManager;
import com.radar.gl.core.Camera;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.gl.layers.CircularGridLayer;
import com.radar.gl.layers.CircularScanLine;
import com.radar.gl.layers.CircularTargetLayer;
import com.radar.gl.layers.CircularLabelLayer;
import com.radar.gl.layers.MarkLayer;
import com.radar.gl.ui.MarkController;
import com.radar.gl.ui.CircularMinimap;
import com.radar.gl.ui.MinimapController;
import com.radar.gl.ui.PanController;
import com.radar.gl.ui.ZoomController;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

/**
 * Dairesel (Sonar Ripple) grafik.
 * Fatih'in disari dogru buyuyen dalga mantigini ve VBO optimizasyonlarini kullanir.
 */
@SuppressWarnings("serial")
public class CircularGraph extends GLCanvas implements GLEventListener, IGraph {

    private static final int MSAA_SAMPLES = 8;
    private static final float BG_R = 0.02f, BG_G = 0.15f, BG_B = 0.08f;

    private final ShaderProgram shader = new ShaderProgram();
    private final Camera camera = new Camera();
    private final TargetGeometry targetGeometry = new TargetGeometry();

    private final CircularGridLayer grid;
    private final CircularScanLine scan;
    private final CircularTargetLayer targets;
    private final CircularLabelLayer labels;
    
    private final MarkLayer markLayer;
    private final CircularMinimap minimap;

    private final ZoomController zoom;
    private final PanController pan;
    private final MarkController markController = new MarkController();
    private final MinimapController minimapController;

    private volatile boolean minimapVisible = true;
    private FPSAnimator animator;
    private final int targetFps;

    public CircularGraph(SimulationConfig config, EntityManager entityManager) {
        super(caps());
        this.targetFps = 60;

        this.grid = new CircularGridLayer();
        this.scan = new CircularScanLine(entityManager, grid.getCircleBuffer());
        this.targets = new CircularTargetLayer(targetGeometry);
        this.labels = new CircularLabelLayer();
        
        this.markLayer = new MarkLayer(markController);
        this.minimap = new CircularMinimap(targetGeometry);
        
        this.zoom = new ZoomController(camera);
        this.pan = new PanController(camera);
        this.minimapController = new MinimapController(camera);

        addGLEventListener(this);
        installInputHandlers();
    }

    private static GLCapabilities caps() {
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities c = new GLCapabilities(profile);
        c.setDoubleBuffered(true);
        c.setHardwareAccelerated(true);
        c.setSampleBuffers(true);
        c.setNumSamples(MSAA_SAMPLES);
        return c;
    }

    @Override
    public void startGraph() {
        if (animator == null) {
            animator = new FPSAnimator(this, targetFps, true);
        }
        if (!animator.isStarted()) {
            scan.reset();
            animator.start();
        }
    }

    @Override
    public void stopGraph() {
        if (animator != null && animator.isStarted()) {
            animator.stop();
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(BG_R, BG_G, BG_B, 1.0f);
        
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_MULTISAMPLE);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);

        shader.init(gl);
        targetGeometry.init(gl);
        minimap.init(gl);
        markLayer.init(gl);
        labels.init(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        shader.dispose(gl);
        targetGeometry.dispose(gl);
        markLayer.dispose(gl);
        minimap.dispose(gl);
        labels.dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        
        // Aspekt oranini her frame'de zorla (ZoomController'in en-boy oranini bozmasini engeller)
        int width = drawable.getSurfaceWidth();
        int height = drawable.getSurfaceHeight();
        if (height == 0) height = 1;
        float aspect = (float) width / height;
        float rangeY = camera.rangeY();
        float expectedRangeX = rangeY * aspect;
        // Eger rangeX beklenenden %1 bile farkliysa duzelt
        if (Math.abs(camera.rangeX() - expectedRangeX) > 0.01f) {
            float currentCenterX = camera.centerX();
            camera.setRangeX(currentCenterX - expectedRangeX / 2f, currentCenterX + expectedRangeX / 2f);
        }
        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        scan.advance();

        shader.use(gl);
        
        grid.draw(gl, shader, camera);
        targets.draw(gl, shader, camera, scan.detected(), scan.scanRadius());
        scan.draw(gl, shader, camera);
        markLayer.draw(gl, shader, camera);
        labels.draw(gl, shader, camera, getWidth(), getHeight());
        
        if (minimapVisible) {
            minimap.draw(gl, shader, camera, targets.getMemory(), scan.scanRadius(), getWidth(), getHeight(), grid.getCircleBuffer());
        }

        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);

        if (height == 0) height = 1;
        float aspect = (float) width / height;
        float currentCenterY = camera.centerY();
        float currentCenterX = camera.centerX();
        float rangeY = camera.rangeY();
        float newRangeX = rangeY * aspect;
        camera.setRangeX(currentCenterX - newRangeX / 2f, currentCenterX + newRangeX / 2f);
    }
    
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
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (minimapController.isDragging()) { minimapController.setDragging(false); return; }
                    if (pan.isDragging())               { pan.release(); return; }
                }

                float wx = camera.screenToWorldX(e.getX(), getWidth());
                float wy = camera.screenToWorldY(e.getY(), getHeight());
                
                if (e.getButton() == MouseEvent.BUTTON3) {
                    markController.add(wx, wy);
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    if (markController.isMarkKeyDown()) markController.add(wx, wy);
                    else                                markController.selectAt(wx, wy);
                }
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
                if      (e.getKeyCode() == KeyEvent.VK_M)          markController.setMarkKeyDown(true);
                else if (e.getKeyCode() == KeyEvent.VK_TAB)        minimapVisible = !minimapVisible;
                else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) markController.removeSelected();
                else if (e.getKeyCode() == KeyEvent.VK_C)          markController.clearAll();
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_M) markController.setMarkKeyDown(false);
            }
        });
    }
}
