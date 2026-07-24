package deneme.Graph.Square;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import deneme.App.GainFilterSlider;
import deneme.App.MarkMenuStyle;
import deneme.Controller.CameraController;
import deneme.Controller.TargetMarkController;
import deneme.GLCore.Camera;
import deneme.GLCore.IDLabel;
import deneme.GLCore.Mark;
import deneme.GLCore.MarkStyle;
import deneme.GLCore.Minimap;
import deneme.GLCore.ShaderProgram;
import deneme.GLCore.Viewport;
import deneme.Graph.IRadarCanvas;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;

public class SquareCanvas extends GLCanvas implements GLEventListener, IRadarCanvas {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_RESOLUTION = 1000;

    /** Dunya boyu sabittir (kamera/simulasyon ile ortak); resolution sadece veri gridini etkiler. */
    private static final float WORLD_SIZE = Camera.WORLD_SIZE;

    private final int resolution;
    private final int cellCount;

    // consumer thread'inin yazdigi, GL thread'inin okudugu paylasilan veri
    private final double[][] image;                       // kalici goruntu
    private volatile int scanRowIndex = 0;                // scanline konumu

    private final ShaderProgram shader = new ShaderProgram();
    private final RowConsumer consumer;

    private final Camera camera = new Camera();
    private final Viewport viewport = new Viewport();

    // ---- SquareCanvasBuilder ile ayarlanan ozellikler (hepsinin default'u var) ----
    private Color background = Color.BLACK;
    private Color firstColor = null;    // null: shader default'u (koyu yesil taban)
    private Color lastColor = null;     // null: shader default'u (acik yesil)
    private GridLayer grid = new GridLayer();          // null: grid yok
    private ScanLine scanLine = new ScanLine();        // null: scanline yok
    private MarkStyle markStyle = defaultMarkStyle();  // null: mark (ve mark menusu) yok
    private IDLabel idLabel = new IDLabel();           // null: ID etiketi yok
    private Minimap minimap = new Minimap();           // null: minimap yok
    private MarkMenuStyle markMenuStyle = new MarkMenuStyle();

    // GPU nesneleri
    private int quadVBO;  // ekrani kaplayan tek dortgen (4 vertex: x,y,u,v)
    private int gainTex;  // gain grid'i (resolution x resolution texture)

    // her karede yeniden doldurulan CPU tarafi diziler
    private final float[] gainData;
    private final float[] matrix = new float[16];
    private final float[] miniMatrix = new float[16];   // minimap: tum dunya

    private int viewWidth = DEFAULT_RESOLUTION;
    private int viewHeight = DEFAULT_RESOLUTION;

    public SquareCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
        this(caps, queue, DEFAULT_RESOLUTION);
    }

    public SquareCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue, int resolution) {
        super(caps);
        this.resolution = resolution;
        this.cellCount = resolution * resolution;
        this.image = new double[resolution][resolution];
        this.gainData = new float[cellCount];
        this.consumer = new RowConsumer(queue);
        addGLEventListener(this);
    }

    private static MarkStyle defaultMarkStyle() {
        MarkStyle style = new MarkStyle();
        style.setSize(22);
        return style;
    }

    // ---- builder erisimi: null vermek ozelligi kapatir ----
    // (setBackground adi Component.setBackground ile cakisir, o yuzden ...Color)
    public void setBackgroundColor(Color color) { if (color != null) this.background = color; }
    public void setFirstColor(Color color)      { this.firstColor = color; }
    public void setLastColor(Color color)       { this.lastColor = color; }
    public void setGrid(GridLayer grid)         { this.grid = grid; }
    public void setScanLine(ScanLine scanLine)  { this.scanLine = scanLine; }
    public void setMarkStyle(MarkStyle style)   { this.markStyle = style; }
    public void setIdLabel(IDLabel idLabel)     { this.idLabel = idLabel; }
    public void setMinimap(Minimap minimap)     { this.minimap = minimap; }
    public void setMarkMenuStyle(MarkMenuStyle style) { if (style != null) this.markMenuStyle = style; }

    /** Mark ozelligi acik mi (mark menusu buna gore kurulur). */
    public boolean hasMark() { return markStyle != null; }

    /**
     * Kamera kontrolleri (zoom / pan / minimap / TAB) CameraController ile
     * baglanir. Builder, minimap gibi ozellikler ayarlandiktan SONRA cagirir;
     * boylece hasMinimap(false) ile kapatilan minimap'e kontrol de kurulmaz.
     */
    public void installCameraController() {
        new CameraController(this, camera, minimap).install();
    }

    /** Sag tik menusu (mark / unmark). Mark ozelligi acik canvas'lara AppBuilder baglar. */
    public void installMarkMenu(deneme.Simulation.Simulation simulation) {
        TargetMarkController.install(this, simulation, markMenuStyle, (eventX, eventY) -> {
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

    @Override public void startConsuming() { consumer.start(); }
    @Override public void stopConsuming()  { consumer.stop(); }

    // ---- consumer: veriyi image'a koy, GL cagirma (baska thread) ----
    private class RowConsumer extends MessageConsumer {
        RowConsumer(BlockingQueue<QueueMessage> queue) { super(queue); }

        @Override
        public void processMessage(QueueMessage message) {
            double[] data = message.getData();
            if (data == null) return;

            // kaynak (simulasyon) cozunurlugu ile canvas cozunurlugu farkliysa ornekle
            int sourceResolution = data.length;
            int row = message.getRow() * resolution / sourceResolution;
            if (row < 0 || row >= resolution) return;

            if (sourceResolution == resolution) {
                image[row] = data;                       // her satir kendi yerine -> kaybolmaz
            } else {
                double[] sampled = new double[resolution];
                for (int col = 0; col < resolution; col++) {
                    sampled[col] = data[col * sourceResolution / resolution];
                }
                image[row] = sampled;
            }
            scanRowIndex = row;
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(background.getRed() / 255f, background.getGreen() / 255f,
                        background.getBlue() / 255f, 1f);

        shader.init(gl);
        if (grid != null) grid.init(gl);

        // ekrani kaplayan dortgen: dunya [0,WORLD] x [0,WORLD], UV [0,1] x [0,1]
        // interleaved: x, y, u, v  (TRIANGLE_STRIP sirasi)
        float S = WORLD_SIZE;
        float[] quad = {
            0f, 0f, 0f, 0f,
            S,  0f, 1f, 0f,
            0f, S,  0f, 1f,
            S,  S,  1f, 1f,
        };

        int[] ids = new int[1];
        gl.glGenBuffers(1, ids, 0);
        quadVBO = ids[0];

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) quad.length * Float.BYTES,
                FloatBuffer.wrap(quad), GL.GL_STATIC_DRAW);

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
        	    resolution,
        	    resolution,
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
        for (int row = 0; row < resolution; row++) {
            double[] r = image[row];
            for (int col = 0; col < resolution; col++) {
                gainData[i++] = (float) r[col];
            }
        }
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, gainTex);
        gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0,
                resolution, resolution,
                GL2.GL_LUMINANCE, GL.GL_FLOAT, FloatBuffer.wrap(gainData));

        drawGain(gl, matrix);

        // scanline: son gelen satirda yatay cizgi
        if (scanLine != null) {
            float y = (scanRowIndex + 0.5f) * (WORLD_SIZE / resolution);
            scanLine.draw(gl, matrix, y, WORLD_SIZE);
        }

        // ---- grid cizgileri + eksen etiketleri ----
        if (grid != null) grid.draw(gl, matrix, camera, viewWidth, viewHeight);

        // ---- tanimli hedefler: mark sekli + ID (kare konumda dunya = veri uzayi) ----
        Mark.Mapper mapper = m -> new float[] { m.getCenterX(), m.getCenterY() };
        if (markStyle != null) {
            Mark.draw(gl, matrix, markStyle,
                    GainFilterSlider.filterMin(), GainFilterSlider.filterMax(), mapper);
        }
        if (idLabel != null) {
            float anchor = (markStyle != null) ? markStyle.getSize() : 0f;
            idLabel.draw(gl, matrix, viewWidth, viewHeight, anchor,
                    GainFilterSlider.filterMin(), GainFilterSlider.filterMax(), mapper);
        }

        drawMinimap(gl);
    }

    /** Gain dortgenini secili gradyan renkleriyle cizer. */
    private void drawGain(GL2 gl, float[] m) {
        shader.use(gl);
        if (firstColor != null) {
            shader.setDarkGreen(gl, firstColor.getRed() / 255f,
                    firstColor.getGreen() / 255f, firstColor.getBlue() / 255f);
        }
        if (lastColor != null) {
            shader.setLightGreen(gl, lastColor.getRed() / 255f,
                    lastColor.getGreen() / 255f, lastColor.getBlue() / 255f);
        }
        shader.setMatrix(gl, m);
        shader.setGainFilter(gl, GainFilterSlider.filterMin(), GainFilterSlider.filterMax());
        shader.bindVertices(gl, quadVBO);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    /** Sol ustte, haritanin kucuk olcekli aynisi: ayni gain quad'i + scanline, gridsiz. */
    private void drawMinimap(GL2 gl) {
        if (minimap == null || !minimap.begin(gl, viewport)) return;

        // kameradan bagimsiz: dunyanin tamami
        Camera.worldMatrix(miniMatrix, 0f, 0f, 2f, 2f);

        drawGain(gl, miniMatrix);
        if (scanLine != null) {
            float y = (scanRowIndex + 0.5f) * (WORLD_SIZE / resolution);
            scanLine.draw(gl, miniMatrix, y, WORLD_SIZE);
        }

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
        gl.glDeleteBuffers(1, new int[] { quadVBO }, 0);
        gl.glDeleteTextures(1, new int[] { gainTex }, 0);
        if (idLabel != null) idLabel.dispose();
        if (grid != null) grid.dispose(gl);
        shader.dispose(gl);
    }
}
