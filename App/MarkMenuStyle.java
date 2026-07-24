package deneme.App;

import java.awt.Color;

/**
 * Sag tik mark menusunun gorunum ayarlari. MarkMenuBuilder uretir.
 * null birakilan renkte Swing'in kendi temasi kullanilir.
 */
public class MarkMenuStyle {

	private Color labelColor = null;        // null: LAF default'u
	private Color backgroundColor = null;   // null: LAF default'u

	public Color getLabelColor()      { return labelColor; }
	public Color getBackgroundColor() { return backgroundColor; }

	public void setLabelColor(Color color)      { this.labelColor = color; }
	public void setBackgroundColor(Color color) { this.backgroundColor = color; }
}
