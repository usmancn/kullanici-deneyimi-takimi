package deneme.Graph.Square;

import java.awt.Color;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

/**
 * Kare grafikteki scanline: son gelen satirin uzerinde yatay cizgi.
 * SquareScanlineBuilder uretir; renk ve kalinlik ayarlanabilir, ikisinin de
 * default'u vardir.
 *
 * <p>Cizim GridLayer ile ayni sabit-fonksiyon (immediate mode) yolunu kullanir.
 */
public class ScanLine {

	public static final Color DEFAULT_COLOR = new Color(77, 255, 102);   // (0.3, 1.0, 0.4)
	public static final float DEFAULT_THICKNESS = 2f;

	private Color lineColor = DEFAULT_COLOR;
	private float thickness = DEFAULT_THICKNESS;

	public Color getLineColor() { return lineColor; }
	public float getThickness() { return thickness; }

	public void setLineColor(Color color)   { if (color != null) this.lineColor = color; }
	public void setThickness(float thickness) { if (thickness > 0) this.thickness = thickness; }

	/**
	 * @param y         cizginin dunya-uzayindaki yuksekligi (satir konumu)
	 * @param worldSize cizginin uzunlugu (dunya genisligi)
	 */
	public void draw(GL2 gl, float[] matrix, float y, float worldSize) {
		gl.glUseProgram(0);
		for (int i = 0; i < 8; i++) {
			gl.glDisableVertexAttribArray(i);
		}
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadMatrixf(matrix, 0);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glColor3f(lineColor.getRed() / 255f, lineColor.getGreen() / 255f,
		             lineColor.getBlue() / 255f);
		gl.glLineWidth(thickness);
		gl.glBegin(GL.GL_LINES);
		gl.glVertex2f(0f, y);
		gl.glVertex2f(worldSize, y);
		gl.glEnd();
		gl.glLineWidth(1f);
	}
}
