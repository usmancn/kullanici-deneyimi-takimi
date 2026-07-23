package deneme.Graph.Circular;

import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.TextRenderer;

import deneme.App.GainFilterSlider;
import deneme.App.MarkMenu;
import deneme.GLCore.Camera;
import deneme.GLCore.Mark;
import deneme.GLCore.Minimap;
import deneme.GLCore.Viewport;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;

/**
 * Kutupsal (PPI) radar ekrani: RadarCanvas ile ayni gain -> renk mantigi,
 * ama kare yerine daire. Satir (row) = merkeze uzaklik (menzil), sutun (col) = aci.
 * Scanline merkezden buyuyen bir halka; dis cember beyaz.
 */
public class CircularCanvas extends GLCanvas implements GLEventListener {

    private static final int SCREEN_RESOLUTION = 1000;
    private static final int CELL_COUNT = SCREEN_RESOLUTION * SCREEN_RESOLUTION;
    private static final float CENTER = SCREEN_RESOLUTION / 2f;        // 500
    private static final float MAX_RADIUS = SCREEN_RESOLUTION * 0.5f;  // 500 -> ic teget cember
    private static final int RING_SEGMENTS = 360;                      // scan halkasi / dis cember cozunurlugu

    // consumer thread'inin yazdigi, GL thread'inin okudugu paylasilan veri
    private final double[][] image = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];
    private volatile int scanRowIndex = 0;

    private final ShaderProgram shader = new ShaderProgram();
    private final GridLayer grid = new GridLayer();
    private TextRenderer text;
    private final RowConsumer consumer;

    private final Camera camera = new Camera();
    private final Viewport viewport = new Viewport();
    private final Minimap minimap = new Minimap();
    private volatile boolean minimapDragging = false;

    // GPU nesneleri
    private int quadVBO;   // ekrani kaplayan dortgen (x,y,u,v)
    private int gainTex;   // gain grid'i (SIZE x SIZE texture)
    private int scanVBO;   // merkezden buyuyen scan halkasi
    private int rimVBO;    // sabit beyaz dis cember

    // her karede yeniden doldurulan CPU tarafi diziler
    private final float[] gainData = new float[CELL_COUNT];
    private final float[] scanData = new float[RING_SEGMENTS * 2];
    private final float[] matrix   = new float[16];
    private final float[] miniMatrix = new float[16];   // minimap: tum dunya

    private int viewWidth = SCREEN_RESOLUTION;
    private int viewHeight = SCREEN_RESOLUTION;

    public CircularCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
        super(caps);
        this.consumer = new RowConsumer(queue);
        addGLEventListener(this);
        installCameraControls();
    }

    private void installCameraControls() {
        // fare konumlari kare cizim alanina gore hesaplanir (pencere daha buyuk olabilir)
        addMouseWheelListener(e -> {
            boolean zoomIn = e.getWheelRotation() < 0;   // teker yukari -> yakinlas
            int side = Viewport.side(getWidth(), getHeight());
            camera.zoom(Viewport.mouseX(e.getX(), getWidth(), getHeight()),
                        Viewport.mouseY(e.getY(), getWidth(), getHeight()),
                        side, side, zoomIn);
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();          // TAB tuslarini alabilmek icin
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;   // sag tik: menu
                if (minimap.contains(e.getX(), e.getY(), getWidth(), getHeight())) {
                    minimapDragging = true;
                    minimap.navigate(camera, e.getX(), e.getY(), getWidth(), getHeight());
                    return;
                }
                camera.panPress(e.getX(), e.getY());
            }
            @Override public void mouseReleased(MouseEvent e) {
                minimapDragging = false;
                camera.panRelease();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (minimapDragging) {           // minimap uzerinde surukleyerek de gezilir
                    minimap.navigate(camera, e.getX(), e.getY(), getWidth(), getHeight());
                    return;
                }
                int side = Viewport.side(getWidth(), getHeight());
                camera.panDrag(e.getX(), e.getY(), side, side);
            }
        });

        // TAB: minimap ac/kapa (odak gezinme tusu olmaktan cikarilir)
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) minimap.toggle();
            }
        });
    }

    /** Sag tik menusu (mark / change mark / unmark). Main tarafindan baglanir. */
    public void installMarkMenu(deneme.Simulation.Simulation simulation) {
        MarkMenu.install(this, simulation, (eventX, eventY) -> {
            int side = Viewport.side(getWidth(), getHeight());
            int localX = eventX - Viewport.offsetX(getWidth(), getHeight());
            int localY = eventY - Viewport.offsetY(getWidth(), getHeight());
            if (localX < 0 || localY < 0 || localX >= side || localY >= side) return null;

            // ekran -> kare dunya
            float worldX = camera.screenToWorldX(localX, side);
            float worldY = camera.screenToWorldY(localY, side);

            // kare dunya -> polar (shader'daki eslesmenin tersi)
            float dx = worldX - CENTER;
            float dy = worldY - CENTER;
            float radius = (float) Math.sqrt(dx * dx + dy * dy) / MAX_RADIUS;
            if (radius > 1f) return null;                    // cember disi

            double bearing = Math.atan2(dx, dy);             // kuzeyden saat yonunde
            if (bearing < 0) bearing += 2.0 * Math.PI;

            int column = (int) Math.round(bearing / (2.0 * Math.PI) * SCREEN_RESOLUTION);
            int row    = Math.round(radius * SCREEN_RESOLUTION);
            if (column >= SCREEN_RESOLUTION) column -= SCREEN_RESOLUTION;   // 360 -> 0
            if (row >= SCREEN_RESOLUTION) row = SCREEN_RESOLUTION - 1;

            return new int[] { column, row };
        });
    }

    public void startConsuming() { consumer.start(); }
    public void stopConsuming()  { consumer.stop(); }

    private class RowConsumer extends MessageConsumer {
        RowConsumer(BlockingQueue<QueueMessage> queue) { super(queue); }

        @Override
        public void processMessage(QueueMessage message) {
            int row = message.getRow();
            if (row >= 0 && row < SCREEN_RESOLUTION) {
                image[row] = message.getData();
                scanRowIndex = row;
            }
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0f, 0.18f, 0f, 1f);
        shader.init(gl);
        grid.init(gl);
        text = new TextRenderer(new Font("SansSerif", Font.BOLD, 16), true, true);

        int[] ids = new int[3];
        gl.glGenBuffers(3, ids, 0);
        quadVBO = ids[0];
        scanVBO = ids[1];
        rimVBO  = ids[2];

        // ekrani kaplayan dortgen: dunya [0,SIZE] x [0,SIZE], UV [0,1] x [0,1]
        float S = SCREEN_RESOLUTION;
        float[] quad = {
            0f, 0f, 0f, 0f,
            S,  0f, 1f, 0f,
            0f, S,  0f, 1f,
            S,  S,  1f, 1f,
        };
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) quad.length * Float.BYTES,
                FloatBuffer.wrap(quad), GL.GL_STATIC_DRAW);

        // scan halkasi (dinamik, yaricapi her karede degisir)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, scanVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) scanData.length * Float.BYTES,
                null, GL.GL_DYNAMIC_DRAW);

        // sabit beyaz dis cember (radius = MAX_RADIUS)
        float[] rim = new float[RING_SEGMENTS * 2];
        buildRing(rim, MAX_RADIUS);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, rimVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) rim.length * Float.BYTES,
                FloatBuffer.wrap(rim), GL.GL_STATIC_DRAW);

        // gain texture
        int[] tex = new int[1];
        gl.glGenTextures(1, tex, 0);
        gainTex = tex[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, gainTex);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);          // aci -> sarilir
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);   // menzil -> kenara kis
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL2.GL_R32F,
                SCREEN_RESOLUTION, SCREEN_RESOLUTION, 0,
                GL2.GL_RED, GL.GL_FLOAT, null);
    }

    // merkez etrafinda verilen yaricapta cember noktalari uretir (LINE_LOOP icin)
    private void buildRing(float[] out, float radius) {
        for (int k = 0; k < RING_SEGMENTS; k++) {
            double theta = 2.0 * Math.PI * k / RING_SEGMENTS;
            out[2 * k]     = CENTER + radius * (float) Math.cos(theta);
            out[2 * k + 1] = CENTER + radius * (float) Math.sin(theta);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // viewport kare oldugu icin ayrica aspect duzeltmesi gerekmiyor (cember yuvarlak kalir)
        camera.modelMatrix(matrix, 0f, 0f, 2f, 2f);

        // ---- gain grid'ini texture'a yukle ----
        int i = 0;
        for (int row = 0; row < SCREEN_RESOLUTION; row++) {
            double[] r = image[row];
            for (int col = 0; col < SCREEN_RESOLUTION; col++) {
                gainData[i++] = (float) r[col];
            }
        }
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, gainTex);
        gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0,
                SCREEN_RESOLUTION, SCREEN_RESOLUTION,
                GL2.GL_LUMINANCE, GL.GL_FLOAT, FloatBuffer.wrap(gainData));

        // ---- kutupsal gain gorseli ----
        shader.use(gl);
        shader.setMatrix(gl, matrix);
        shader.setGainFilter(gl, GainFilterSlider.filterMin(), GainFilterSlider.filterMax());
        shader.bindVertices(gl, quadVBO);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

        // ---- scan halkasi: merkezden buyuyen cember ----
        float scanRadius = scanRowIndex * (MAX_RADIUS / SCREEN_RESOLUTION);
        buildRing(scanData, scanRadius);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, scanVBO);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                (long) scanData.length * Float.BYTES, FloatBuffer.wrap(scanData));

        shader.useScan(gl);
        shader.setScanMatrix(gl, matrix);
        shader.setScanColor(gl, 0.3f, 1.0f, 0.4f);
        shader.bindScanPosition(gl, scanVBO);
        gl.glLineWidth(2f);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, RING_SEGMENTS);

        // ---- dis cember: beyaz ----
        shader.setScanColor(gl, 1.0f, 1.0f, 1.0f);
        shader.bindScanPosition(gl, rimVBO);
        gl.glLineWidth(2f);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, RING_SEGMENTS);
        gl.glLineWidth(1f);

        // ---- grid: halkalar + aci cizgileri + etiketler ----
        grid.draw(gl, matrix, viewWidth, viewHeight);

        // ---- tanimli hedefler: cember + ID (polar konum) ----
        Mark.draw(gl, text, matrix, viewWidth, viewHeight, 18f,
                  GainFilterSlider.filterMin(), GainFilterSlider.filterMax(), m -> {
            // shader ile ayni eslesme: a = bearing/2pi  =>  bearing = x/SIZE * 2pi
            // bearing kuzeyden (yukari) saat yonunde olculur: x = sin, y = cos
            double bearing = 2.0 * Math.PI * (m.getCenterX() / (double) SCREEN_RESOLUTION);
            float rad = (float) (m.getCenterY() / (double) SCREEN_RESOLUTION * MAX_RADIUS);
            return new float[] { CENTER + rad * (float) Math.sin(bearing),
                                 CENTER + rad * (float) Math.cos(bearing) };
        });

        drawMinimap(gl);
    }

    /** Sol ustte, haritanin 1/4 olcekli aynisi: ayni polar gorsel + halkalar, gridsiz. */
    private void drawMinimap(GL2 gl) {
        if (!minimap.begin(gl, viewport)) return;

        // kameradan bagimsiz: dunyanin tamami
        Camera.worldMatrix(miniMatrix, 0f, 0f, 2f, 2f);

        shader.use(gl);
        shader.setMatrix(gl, miniMatrix);
        shader.setGainFilter(gl, GainFilterSlider.filterMin(), GainFilterSlider.filterMax());
        shader.bindVertices(gl, quadVBO);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

        // scan halkasi + dis cember (scanVBO bu karede zaten guncellendi)
        shader.useScan(gl);
        shader.setScanMatrix(gl, miniMatrix);
        shader.setScanColor(gl, 0.3f, 1.0f, 0.4f);
        shader.bindScanPosition(gl, scanVBO);
        gl.glLineWidth(1f);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, RING_SEGMENTS);

        shader.setScanColor(gl, 1.0f, 1.0f, 1.0f);
        shader.bindScanPosition(gl, rimVBO);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, RING_SEGMENTS);

        minimap.end(gl, viewport, camera);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        viewport.apply(gl, width, height);          // ortalanmis kare, en fazla 1000x1000
        this.viewWidth = viewport.side();
        this.viewHeight = viewport.side();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glDeleteBuffers(3, new int[] { quadVBO, scanVBO, rimVBO }, 0);
        gl.glDeleteTextures(1, new int[] { gainTex }, 0);
        if (text != null) text.dispose();
        grid.dispose(gl);
        shader.dispose(gl);
    }
}
