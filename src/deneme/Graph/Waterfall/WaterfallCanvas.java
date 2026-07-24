package deneme.Graph.Waterfall;


import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import deneme.GLCore.Camera;
import deneme.GLCore.ShaderProgram;
import deneme.GLCore.Viewport;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;
import deneme.Interfaces.RadarGraph;
import deneme.Controller.CameraController;

public class WaterfallCanvas extends GLCanvas implements GLEventListener, RadarGraph {

    private static final int SCREEN_RESOLUTION = 1000;
    private static final int CELL_COUNT = SCREEN_RESOLUTION * SCREEN_RESOLUTION;
    // ARB_texture_float internal format (JOGL'de isimle acik degil, degeri 0x8818)
    private static final int GL_LUMINANCE32F_ARB = 0x8818;

    // consumer thread'inin yazdigi, GL thread'inin okudugu paylasilan veri
    // image[SIZE-1] = en yeni satir (ust), image[0] = en eski (alt)
    private final double[][] image = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];

    private final ShaderProgram shader = new ShaderProgram();
    private final RowConsumer consumer;

    private final Camera camera = new Camera();
    private final Viewport viewport = new Viewport();

    // GPU nesneleri
    private int quadVBO;   // ekrani kaplayan tek dortgen (4 vertex: x,y,u,v)
    private int gainTex;   // gain grid'i (SIZE x SIZE texture)

    // her karede yeniden doldurulan CPU tarafi diziler
    private final float[] gainData = new float[CELL_COUNT];
    private final float[] matrix = new float[16];

    public WaterfallCanvas(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
        super(caps);
        this.consumer = new RowConsumer(queue);
        addGLEventListener(this);
        installCameraControls();

        // baslangicta ekran bos (0 -> koyu yesil zemin)
        for (int r = 0; r < SCREEN_RESOLUTION; r++) {
            image[r] = new double[SCREEN_RESOLUTION];
        }
    }

    @Override
    public GLCanvas canvas() {
        return this;
    }

    private void installCameraControls() {
        // fare konumlari kare cizim alanina gore hesaplanir (pencere daha buyuk olabilir)
        new CameraController(this, camera).install();
    }

    @Override
    public void startConsuming() { consumer.start(); }
    @Override
    public void stopConsuming()  { consumer.stop(); }

    // consumer: satirlari asagi kaydir, en yeniyi uste koy (GL cagirma)
    private class RowConsumer extends MessageConsumer {
        RowConsumer(BlockingQueue<QueueMessage> queue) { super(queue); }

        @Override
        public void processMessage(QueueMessage message) {
            double[] newRow = message.getData();
            // hepsini bir asagi kaydir 
            for (int r = 0; r < SCREEN_RESOLUTION - 1; r++) {
                image[r] = image[r + 1];
            }
            image[SCREEN_RESOLUTION - 1] = newRow;   // en yeni satir en uste
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0f, 0f, 0f, 1f);

        shader.init(gl);

        // ekrani kaplayan dortgen: dunya [0,SIZE] x [0,SIZE], UV [0,1] x [0,1]
        // interleaved: x, y, u, v  (TRIANGLE_STRIP sirasi)
        float S = SCREEN_RESOLUTION;
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

        
        int[] tex = new int[1];
        gl.glGenTextures(1, tex, 0);
        gainTex = tex[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, gainTex);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL_LUMINANCE32F_ARB,
                SCREEN_RESOLUTION, SCREEN_RESOLUTION, 0,
                GL2.GL_LUMINANCE, GL.GL_FLOAT, null);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

       
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

        // gain dortgenini ciz (texture'dan renklenir)
        shader.use(gl);
        shader.setMatrix(gl, matrix);
        shader.bindVertices(gl, quadVBO);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        viewport.apply(gl, width, height);          // ortalanmis kare, en fazla 1000x1000
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glDeleteBuffers(1, new int[] { quadVBO }, 0);
        gl.glDeleteTextures(1, new int[] { gainTex }, 0);
        shader.dispose(gl);
    }
}
