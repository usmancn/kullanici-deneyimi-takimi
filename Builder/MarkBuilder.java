package deneme.Builder;

import java.awt.Color;

import deneme.GLCore.MarkStyle;
import deneme.GLCore.MarkType;
import deneme.Interfaces.IMarkBuilder;

public class MarkBuilder implements IMarkBuilder {

	private final MarkStyle style = new MarkStyle();

	@Override
	public IMarkBuilder markColor(Color color) {
		style.setColor(color);
		return this;
	}

	@Override
	public IMarkBuilder size(int size) {
		style.setSize(size);
		return this;
	}

	@Override
	public IMarkBuilder type(MarkType type) {
		style.setType(type);
		return this;
	}

	@Override
	public MarkStyle build() {
		return style;
	}
}
