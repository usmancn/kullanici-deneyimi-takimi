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

/**
 * Dairesel (Circular) grafik.
 * GLCanvas tabanli, donen radar ekranini cizer.
 */
@SuppressWarnings("serial")
public class CircularGraph extends GLCanvas implements GLEventListener, IGraph {

    private static final int MSAA_SAMPLES = 8;
    private static final float GL_LINE_WIDTH = 2f;

    private static final float BG_R = 0.02f, BG_G = 0.15f, BG_B = 0.08f;

    private final ShaderProgram shader = new ShaderProgram();
    private final Camera camera = new Camera();
    private final TargetGeometry targetGeometry = new TargetGeometry();

    private final CircularGridLayer grid;
    private final CircularScanLine scan;
    private final CircularTargetLayer targets;

    private FPSAnimator animator;
    private final int targetFps;

    public CircularGraph(SimulationConfig config, EntityManager entityManager) {
        super(caps());
        this.targetFps = 60; // Default or from config

        this.grid = new CircularGridLayer();
        this.scan = new CircularScanLine(entityManager);
        this.targets = new CircularTargetLayer(targetGeometry);

        addGLEventListener(this);
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
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        shader.dispose(gl);
        targetGeometry.dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        scan.advance();

        shader.use(gl);
        
        grid.draw(gl, shader, camera);
        targets.draw(gl, shader, camera, scan.detected(), scan.scanAngle());
        scan.draw(gl, shader, camera);

        // shader.unuse(gl); yok, disableAttribs cagirabiliriz veya birakabiliriz
        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }
}
