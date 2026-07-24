package deneme.GLCore;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Tespit edilen hedeflerin ID etiketlerini cizen sinif. Eskiden {@link Mark}
 * icindeydi; ID mark'tan ayri bir ozellik oldugu icin (mark kapaliyken de ID
 * gorunebilir) kendi sinifina ayrildi. IDBuilder uretir.
 *
 * <p>Metin ekran-uzayinda sabit boyutta cizilir (zoom ile buyumez).
 * TextRenderer bir GL context'e bagli oldugundan her canvas kendi IDLabel
 * ornegini kullanir ve {@link #dispose} etmekle yukumludur.
 */
public class IDLabel {

	public static final int DEFAULT_SIZE = 16;
	public static final Color DEFAULT_TEXT_COLOR = Color.WHITE;

	private static final int TEXT_MARGIN_PX = 4;   // etiket ile sekil arasi bosluk (piksel)

	private int size = DEFAULT_SIZE;               // font boyutu (piksel)
	private Color textColor = DEFAULT_TEXT_COLOR;

	private TextRenderer text;

	public int getSize()        { return size; }
	public Color getTextColor() { return textColor; }

	public void setSize(int px)            { if (px > 0) this.size = px; }
	public void setTextColor(Color color)  { if (color != null) this.textColor = color; }

	/**
	 * Gain filtresi araligindaki tum kayitli hedeflerin ID'sini yazar.
	 *
	 * @param anchorRadius hedefin ustundeki sekil yaricapi (dunya birimi);
	 *                     etiket seklin hemen ustune konur, sekil yoksa 0 verilir
	 */
	public void draw(GL2 gl, float[] matrix, int width, int height,
	                 float anchorRadius, float filterMin, float filterMax,
	                 Mark.Mapper mapper) {
		if (width <= 0 || height <= 0) return;

		Collection<Mark> marks = new ArrayList<>();
		for (Mark mark : Mark.all()) {
			if (mark.getGain() >= filterMin && mark.getGain() <= filterMax) {
				marks.add(mark);
			}
		}
		if (marks.isEmpty()) return;

		if (text == null) {
			text = new TextRenderer(new Font("SansSerif", Font.BOLD, size), true, true);
		}

		text.beginRendering(width, height);
		text.setColor(textColor.getRed() / 255f, textColor.getGreen() / 255f,
		              textColor.getBlue() / 255f, 1f);
		for (Mark mark : marks) {
			float[] p = mapper.world(mark);
			float clipX = matrix[0] * p[0] + matrix[4] * p[1] + matrix[12];
			float clipY = matrix[1] * p[0] + matrix[5] * p[1] + matrix[13];
			float clipW = matrix[3] * p[0] + matrix[7] * p[1] + matrix[15];
			if (clipW == 0f) continue;
			int pixelX = Math.round((clipX / clipW * 0.5f + 0.5f) * width);
			int pixelY = Math.round((clipY / clipW * 0.5f + 0.5f) * height);

			// seklin ust kenarini piksel olarak bul -> etiketi hemen ustune, yatayda ortali koy
			int radiusPx = Math.round(anchorRadius * Math.abs(matrix[5]) * height * 0.5f);
			int textWidth = Math.round((float) text.getBounds(mark.getId()).getWidth());
			text.draw(mark.getId(), pixelX - textWidth / 2, pixelY + radiusPx + TEXT_MARGIN_PX);
		}
		text.endRendering();
	}

	public void dispose() {
		if (text != null) {
			text.dispose();
			text = null;
		}
	}
}
