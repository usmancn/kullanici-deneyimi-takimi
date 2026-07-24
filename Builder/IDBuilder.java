package deneme.Builder;

import java.awt.Color;

import deneme.GLCore.IDLabel;
import deneme.Interfaces.IIDBuilder;

public class IDBuilder implements IIDBuilder {

	private final IDLabel label = new IDLabel();

	@Override
	public IIDBuilder size(int px) {
		label.setSize(px);
		return this;
	}

	@Override
	public IIDBuilder textColor(Color color) {
		label.setTextColor(color);
		return this;
	}

	@Override
	public IDLabel build() {
		return label;
	}
}
