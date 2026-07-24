package deneme.Graph.Square;

import java.awt.Font;
import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.TextRenderer;

import deneme.Simulation.GainFilterModel;
import deneme.Controller.TargetMarkController;
import deneme.Controller.CameraController;
import deneme.GLCore.Camera;
import deneme.GLCore.Mark;
import deneme.GLCore.Minimap;
import deneme.GLCore.ShaderProgram;
import deneme.GLCore.Viewport;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;
import deneme.Interfaces.RadarGraph;

public class SquareCanvas extends GLCanvas implements GLEventListener, RadarGraph{

    private static final int SCREEN_RESOLUTION = 1000;
    private static final int CELL_COUNT = SCREEN_RESOLUTION * SCREEN_RESOLUTION;

    private final GainFilterModel gainFilter;
    // consumer thread'inin yazdigi, GL thread'inin okudugu paylasilan veri
    private final double[][] image = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];   // kalici gorunti
    private volatile int scanRowIndex = 0;                     // scanline konumu

    private final ShaderProgram shader = new ShaderProgram();
    private final GridLayer grid = new GridLayer();
    private TextRenderer text;
    private final RowConsumer consumer;

    private final Camera camera = new Camera();
    private final Viewport viewport = new Viewport();
    private final Minimap minimap = new Minimap();

    // GPU nesneleri
    private int quadVBO;  // ekrani kaplayan tek dortgen (4 vertex: x,y,u,v)
    private int gainTex;   // gain grid'i (SIZE x SIZE texture)
    private int scanVBO;   // scanline'in iki ucu

    // her karede yeniden doldurulan CPU tarafi diziler
    private final float[] gainData = new float[CELL_COUNT];   // texture'a yuklenen gain
    private final float[] scanData = new float[4];
    private final float[] matrix = new float[16];
    private final float[] miniMatrix = new float[16];   // minimap: tum dunya

    private int viewWidth = SCREEN_RESOLUTION;
    private int viewHeight = SCREEN_RESOLUTION;
  
    public SquareCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue, GainFilterModel gainFilter) {
        super(caps);
        this.consumer = new RowConsumer(queue);
        this.gainFilter = gainFilter;
        addGLEventListener(this);
        installCameraControls();
    }

    @Override
    public GLCanvas canvas() {
        return this;
    }

    private void installCameraControls() {
        new CameraController(this, camera, minimap).install();
    }

    /** Target mark controller'ini bu canvas'a baglar. Main tarafindan cagrilir. */
    public void installTargetMarkController(deneme.Simulation.Simulation simulation) {
        TargetMarkController.install(this, simulation, (eventX, eventY) -> {
            int side = Viewport.side(getWidth(), getHeight());
            int localX = eventX - Viewport.offsetX(getWidth(), getHeight());
            int localY = eventY - Viewport.offsetY(getWidth(), getHeight());
            if (localX < 0 || localY < 0 || localX >= side || localY >= side) return null;

            // kare grafikte dunya dogrudan ekranla ortusur
            return new int[] {
                Math.round(camera.screenToWorldX(localX, side)),
                Math.round(camera.screenToWorldY(localY, side))
            };
        });
    }

    @Override
    public void startConsuming() { consumer.start(); }
    @Override
    public void stopConsuming()  { consumer.stop(); }

    // ---- consumer: veriyi image'a koy, GL cagirma (baska thread) ----
    private class RowConsumer extends MessageConsumer {
        RowConsumer(BlockingQueue<QueueMessage> queue) { super(queue); }

        @Override
        public void processMessage(QueueMessage message) {
            int row = message.getRow();
            if (row >= 0 && row < SCREEN_RESOLUTION) {
                image[row] = message.getData();   // her satir kendi yerine -> kaybolmaz
                scanRowIndex = row;
            }
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0f, 0f, 0f, 1f);

        shader.init(gl);
        grid.init(gl);
        text = new TextRenderer(new Font("SansSerif", Font.BOLD, 16), true, true);

        // ekrani kaplayan dortgen: dunya [0,SIZE] x [0,SIZE], UV [0,1] x [0,1]
        // interleaved: x, y, u, v  (TRIANGLE_STRIP sirasi)
        float S = SCREEN_RESOLUTION;
        float[] quad = {
            0f, 0f, 0f, 0f,
            S,  0f, 1f, 0f,
            0f, S,  0f, 1f,
            S,  S,  1f, 1f,
        };

        int[] ids = new int[2];
        gl.glGenBuffers(2, ids, 0);
        quadVBO = ids[0];
        scanVBO = ids[1];

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) quad.length * Float.BYTES,
                FloatBuffer.wrap(quad), GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, scanVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) scanData.length * Float.BYTES,
                null, GL.GL_DYNAMIC_DRAW);

        //gain texture
        int[] tex = new int[1];
        gl.glGenTextures(1, tex, 0);
        gainTex = tex[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, gainTex);
        // NEAREST her hucre keskin kare, zoom'da bulaniklik/seam yok
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        // internal format 32-bit float -> gain yuvarlanmaz (8-bit banding/siyah nokta olmaz)
        gl.glTexImage2D(
        	    GL.GL_TEXTURE_2D,
        	    0,
        	    GL2.GL_R32F,
        	    SCREEN_RESOLUTION,
        	    SCREEN_RESOLUTION,
        	    0,
        	    GL2.GL_RED,
        	    GL.GL_FLOAT,
        	    null
        	);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

       
        // viewport kare oldugu icin ayrica aspect duzeltmesi gerekmiyor
        camera.modelMatrix(matrix, 0f, 0f, 2f, 2f);

        // gain grid'ini texture'a yukle
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

        shader.use(gl);
        shader.setMatrix(gl, matrix);
        shader.setGainFilter(gl, gainFilter.filterMin(), gainFilter.filterMax());
        shader.bindVertices(gl, quadVBO);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

        // scanline: son gelen satirda yatay cizgi
        float y = scanRowIndex + 0.5f;
        scanData[0] = 0f;                    scanData[1] = y;
        scanData[2] = SCREEN_RESOLUTION;     scanData[3] = y;
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, scanVBO);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                (long) scanData.length * Float.BYTES, FloatBuffer.wrap(scanData));

        shader.useScan(gl);
        shader.setScanMatrix(gl, matrix);
        shader.bindScanPosition(gl, scanVBO);
        gl.glLineWidth(2f);
        gl.glDrawArrays(GL.GL_LINES, 0, 2);

        // ---- grid cizgileri + eksen etiketleri ----
        grid.draw(gl, matrix, camera, viewWidth, viewHeight);

        // ---- tanimli hedefler: cember + ID (kare konum) ----
        Mark.draw(gl, text, matrix, viewWidth, viewHeight, 22f,
                gainFilter.filterMin(), gainFilter.filterMax(),
                m -> new float[] { m.getCenterX(), m.getCenterY() });

        drawMinimap(gl);
    }

    /** Sol ustte, haritanin 1/4 olcekli aynisi: ayni gain quad'i + scanline, gridsiz. */
    private void drawMinimap(GL2 gl) {
        if (!minimap.begin(gl, viewport)) return;

        // kameradan bagimsiz: dunyanin tamami
        Camera.worldMatrix(miniMatrix, 0f, 0f, 2f, 2f);

        shader.use(gl);
        shader.setMatrix(gl, miniMatrix);
        shader.setGainFilter(gl, gainFilter.filterMin(), gainFilter.filterMax());
        shader.bindVertices(gl, quadVBO);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        shader.useScan(gl);
        shader.setScanMatrix(gl, miniMatrix);
        shader.bindScanPosition(gl, scanVBO);
        gl.glLineWidth(1f);
        gl.glDrawArrays(GL.GL_LINES, 0, 2);

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
        gl.glDeleteBuffers(2, new int[] { quadVBO, scanVBO }, 0);
        gl.glDeleteTextures(1, new int[] { gainTex }, 0);
        if (text != null) text.dispose();
        grid.dispose(gl);
        shader.dispose(gl);
    }
}
