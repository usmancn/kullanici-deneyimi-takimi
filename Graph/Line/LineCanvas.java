package deneme.Graph.Line;

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
import deneme.Controller.CameraController;
import deneme.GLCore.Camera;
import deneme.GLCore.Viewport;
import deneme.Graph.IRadarCanvas;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;

public class LineCanvas extends GLCanvas implements GLEventListener, IRadarCanvas {

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_RESOLUTION = 1000;

	/** Dunya boyu sabittir; resolution sadece cizgi nokta sayisini etkiler. */
	private static final float WORLD_SIZE = Camera.WORLD_SIZE;

	private final int resolution;

    // consumer thread'inin yazdigi, GL thread'inin okudugu paylasilan veri
    private final double[][] image;                            // kalici goruntu
    private volatile int scanRowIndex = 0;                     // cizilecek Satir

    private final ShaderProgram shader = new ShaderProgram();
    private final RowConsumer consumer;

    private final Camera camera = new Camera();
    private final Viewport viewport = new Viewport();

    private final float[] matrix = new float[16];

    // ---- LineCanvasBuilder ile ayarlanan ozellikler (hepsinin default'u var) ----
    private Color background = Color.BLACK;
    private Color lineColor = null;           // null: shader default'u (yesil)
    private Color averageLineColor = null;    // null: shader default'u (kirmizi)
    private GridLayer grid = new GridLayer(); // null: grid yok (line'in tek opsiyonel ozelligi)

    // GPU nesneleri (texture yok, sadece cizgi vertexleri)
    private int lineVBO;      // son gelen satirlarin cizgileri
    private int averageVBO;   // son 30 satirin ortalama cizgisi

    // her karede yeniden doldurulan CPU tarafi diziler (nokta basina 2 float: x, y)
    private final float[] lineData;
    private final float[] averageData;

    private int viewWidth = DEFAULT_RESOLUTION;
    private int viewHeight = DEFAULT_RESOLUTION;

    public LineCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
        this(caps, queue, DEFAULT_RESOLUTION);
    }

    public LineCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue, int resolution) {
        super(caps);
        this.resolution = resolution;
        this.image = new double[resolution][resolution];
        this.lineData = new float[resolution * 2];
        this.averageData = new float[resolution * 2];
        this.consumer = new RowConsumer(queue);
        addGLEventListener(this);
        installCameraControls();
    }

    // ---- builder erisimi ----
    // (setBackground adi Component.setBackground ile cakisir, o yuzden ...Color)
    public void setBackgroundColor(Color color)    { if (color != null) this.background = color; }
    public void setLineColor(Color color)          { this.lineColor = color; }
    public void setAverageLineColor(Color color)   { this.averageLineColor = color; }
    public void setGrid(GridLayer grid)            { this.grid = grid; }

    @Override public void startConsuming() { consumer.start(); }
    @Override public void stopConsuming()  { consumer.stop(); }

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
                image[row] = data;   // her satir kendi yerine -> kaybolmaz
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

    private void installCameraControls() {
        // sadece zoom + pan; minimap/TAB yok (ozellik matrisi geregi)
        new CameraController(this, camera).install();
    }

	@Override
	public void display(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // kameranin gorunur araligi -> NDC [-1,1] zoom/pan buradan gelir
        camera.modelMatrix(matrix, 0f, 0f, 2f, 2f);

        // ---- sabit grid (veri cizgilerinin altinda kalsin diye once) ----
        if (grid != null) grid.draw(gl, matrix, viewWidth, viewHeight);

        float xScale = WORLD_SIZE / resolution;
        int k = 0;
        int t = 0;
        int count = Math.min(30, scanRowIndex + 1);   // eldeki satir sayisi
        for (int i = 0; i < resolution; i++) {
            // ana cizgi: son satir (gain 0..1 -> dunya 0..1000)
            lineData[k++] = i * xScale;
            lineData[k++] = (float) (image[scanRowIndex][i] * WORLD_SIZE);

            // ortalama cizgi: son 30 satirin ortalamasi
            double sum = 0;
            for (int j = scanRowIndex - count + 1; j <= scanRowIndex; j++) {
                sum += image[j][i];
            }
            averageData[t++] = i * xScale;
            averageData[t++] = (float) (sum / count * WORLD_SIZE);
        }

        // ---- ana cizgi ----
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, lineVBO);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                (long) lineData.length * Float.BYTES, FloatBuffer.wrap(lineData));

        shader.use(gl);
        if (lineColor != null) {
            shader.setLineColor(gl, lineColor.getRed() / 255f,
                    lineColor.getGreen() / 255f, lineColor.getBlue() / 255f);
        }
        shader.setMatrix(gl, matrix);
        shader.setGainFilter(gl, GainFilterSlider.filterMin(), GainFilterSlider.filterMax());
        shader.bindVertices(gl, lineVBO);
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, resolution);

        // ---- ortalama cizgi (soluk, blend ile) ----
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, averageVBO);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                (long) averageData.length * Float.BYTES, FloatBuffer.wrap(averageData));

        shader.useAverage(gl);
        if (averageLineColor != null) {
            shader.setAverageLineColor(gl, averageLineColor.getRed() / 255f,
                    averageLineColor.getGreen() / 255f, averageLineColor.getBlue() / 255f);
        }
        shader.setAverageMatrix(gl, matrix);
        shader.setAverageGainFilter(gl, GainFilterSlider.filterMin(), GainFilterSlider.filterMax());
        shader.bindAverageVertices(gl, averageVBO);
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, resolution);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(background.getRed() / 255f, background.getGreen() / 255f,
                        background.getBlue() / 255f, 1f);

        // soluk ortalama cizgisi (alpha 0.3) icin blend acik olmali
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        shader.init(gl);
        if (grid != null) grid.init(gl);

        int[] ids = new int[2];
        gl.glGenBuffers(2, ids, 0);
        lineVBO = ids[0];
        averageVBO = ids[1];

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, lineVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) lineData.length * Float.BYTES,
                null, GL.GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, averageVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) averageData.length * Float.BYTES,
                null, GL.GL_DYNAMIC_DRAW);
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
        gl.glDeleteBuffers(2, new int[] { lineVBO, averageVBO }, 0);
        if (grid != null) grid.dispose(gl);
        shader.dispose(gl);
    }
}
