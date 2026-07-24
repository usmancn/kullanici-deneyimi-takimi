package deneme.Graph.Circular;

import java.awt.Color;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

/**
 * Dairesel (PPI) grafikteki scanline: merkezden buyuyen halka.
 * CircularScanlineBuilder uretir; renk, kalinlik ve halkanin kac parcadan
 * (segment) olusacagi ayarlanabilir, hepsinin default'u vardir.
 *
 * <p>Cizim GridLayer ile ayni sabit-fonksiyon (immediate mode) yolunu kullanir.
 */
public class ScanLine {

	public static final Color DEFAULT_COLOR = new Color(77, 255, 102);   // (0.3, 1.0, 0.4)
	public static final float DEFAULT_THICKNESS = 2f;
	public static final int DEFAULT_STEP_COUNT = 360;

	private Color lineColor = DEFAULT_COLOR;
	private float thickness = DEFAULT_THICKNESS;
	private int stepCount = DEFAULT_STEP_COUNT;

	public Color getLineColor() { return lineColor; }
	public float getThickness() { return thickness; }
	public int getStepCount()   { return stepCount; }

	public void setLineColor(Color color)     { if (color != null) this.lineColor = color; }
	public void setThickness(float thickness) { if (thickness > 0) this.thickness = thickness; }
	public void setStepCount(int step)        { if (step >= 3) this.stepCount = step; }

	/** (centerX, centerY) merkezli, verilen yaricapta halka cizer. */
	public void draw(GL2 gl, float[] matrix, float centerX, float centerY, float radius) {
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
		gl.glBegin(GL.GL_LINE_LOOP);
		for (int k = 0; k < stepCount; k++) {
			double theta = 2.0 * Math.PI * k / stepCount;
			gl.glVertex2f(centerX + radius * (float) Math.cos(theta),
			              centerY + radius * (float) Math.sin(theta));
		}
		gl.glEnd();
		gl.glLineWidth(1f);
	}
}
