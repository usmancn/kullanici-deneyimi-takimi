package deneme.Builder;

import java.awt.Color;

import deneme.App.MarkMenuStyle;
import deneme.Interfaces.IMarkMenuRenderer;

public class MarkMenuBuilder implements IMarkMenuRenderer {

	private final MarkMenuStyle style = new MarkMenuStyle();

	@Override
	public IMarkMenuRenderer labelColor(Color color) {
		style.setLabelColor(color);
		return this;
	}

	@Override
	public IMarkMenuRenderer backgroundColor(Color color) {
		style.setBackgroundColor(color);
		return this;
	}

	@Override
	public MarkMenuStyle build() {
		return style;
	}
}
