package deneme.GLCore;

import java.awt.Color;

/**
 * Mark cizim stili: sekil tipi, boyutu ve rengi. MarkBuilder uretir,
 * canvas her karede {@link Mark#draw} cagrisina verir.
 *
 * <p>Butun alanlarin default'u vardir: sari, 20 dunya birimi, daire.
 */
public class MarkStyle {

	public static final Color DEFAULT_COLOR = Color.YELLOW;
	public static final int DEFAULT_SIZE = 20;
	public static final MarkType DEFAULT_TYPE = MarkType.CIRCLE;

	private Color color = DEFAULT_COLOR;
	private int size = DEFAULT_SIZE;
	private MarkType type = DEFAULT_TYPE;

	public Color getColor() { return color; }
	public int getSize()    { return size; }
	public MarkType getType() { return type; }

	public void setColor(Color color) { if (color != null) this.color = color; }
	public void setSize(int size)     { if (size > 0) this.size = size; }
	public void setType(MarkType type) { if (type != null) this.type = type; }
}
