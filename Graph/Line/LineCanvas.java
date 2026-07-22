package deneme.Graph.Line;

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

import deneme.GLCore.Camera;
import deneme.Graph.Line.ShaderProgram;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;

public class LineCanvas extends GLCanvas implements GLEventListener {
	
	private static final int SCREEN_RESOLUTION = 1000;

    // consumer thread'inin yazdigi, GL thread'inin okudugu paylasilan veri
    private final double[][] image = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];   // kalici goruntu
    private volatile int scanRowIndex = 0;                     // cizilecek Satir

    private final ShaderProgram shader = new ShaderProgram();
    private final RowConsumer consumer;

    private final Camera camera = new Camera();

    private final float[] matrix = new float[16];

    // GPU nesneleri (texture yok, sadece cizgi vertexleri)
    private int lineVBO;      // son gelen satirlarin cizgileri
    private int averageVBO;   // son 30 satirin ortalama cizgisi

    // her karede yeniden doldurulan CPU tarafi diziler (nokta basina 2 float: x, y)
    private final float[] lineData    = new float[SCREEN_RESOLUTION * 2];
    private final float[] averageData = new float[SCREEN_RESOLUTION * 2];

    private int viewWidth = SCREEN_RESOLUTION;
    private int viewHeight = SCREEN_RESOLUTION;
    
    public void startConsuming() { consumer.start(); }
    public void stopConsuming()  { consumer.stop(); }

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
    
    public LineCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
        super(caps);
        this.consumer = new RowConsumer(queue);
        addGLEventListener(this);
        installCameraControls();
    }
    
    private void installCameraControls() {
        addMouseWheelListener(e -> {
            boolean zoomIn = e.getWheelRotation() < 0;   // teker yukari -> yakinlas
            camera.zoom(e.getX(), e.getY(), getWidth(), getHeight(), zoomIn);
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { camera.panPress(e.getX(), e.getY()); }
            @Override public void mouseReleased(MouseEvent e) { camera.panRelease(); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                camera.panDrag(e.getX(), e.getY(), getWidth(), getHeight());
            }
        });
    }
	@Override
	public void display(GLAutoDrawable drawable) {
		
		GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // kameranin gorunur araligi -> NDC [-1,1] zoom/pan buradan gelir
        camera.modelMatrix(matrix, 0f, 0f, 2f, 2f);

        int k = 0;
        int t = 0;
        int count = Math.min(30, scanRowIndex + 1);   // eldeki satir sayisi
        for (int i = 0; i < SCREEN_RESOLUTION; i++) {
            // ana cizgi: son satir (gain 0..1 -> dunya 0..1000)
            lineData[k++] = i;
            lineData[k++] = (float) (image[scanRowIndex][i] * 1000);

            // ortalama cizgi: son 30 satirin ortalamasi
            double sum = 0;
            for (int j = scanRowIndex - count + 1; j <= scanRowIndex; j++) {
                sum += image[j][i];
            }
            averageData[t++] = i;
            averageData[t++] = (float) (sum / count * 1000);
        }

        // ---- ana cizgi ----
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, lineVBO);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                (long) lineData.length * Float.BYTES, FloatBuffer.wrap(lineData));

        shader.use(gl);
        shader.setMatrix(gl, matrix);
        shader.bindVertices(gl, lineVBO);
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, SCREEN_RESOLUTION);

        // ---- ortalama cizgi (soluk, blend ile) ----
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, averageVBO);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                (long) averageData.length * Float.BYTES, FloatBuffer.wrap(averageData));

        shader.useAverage(gl);
        shader.setAverageMatrix(gl, matrix);
        shader.bindAverageVertices(gl, averageVBO);
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, SCREEN_RESOLUTION);
	}

	

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(1f, 1f, 1f, 1f);

        // soluk ortalama cizgisi (alpha 0.3) icin blend acik olmali
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        shader.init(gl);

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
        gl.glViewport(0, 0, width, height);
        this.viewWidth = width;
        this.viewHeight = height;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glDeleteBuffers(2, new int[] { lineVBO, averageVBO }, 0);
        shader.dispose(gl);
    }
}
	
	

