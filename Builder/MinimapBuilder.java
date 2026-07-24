package deneme.Builder;

import java.awt.Color;

import deneme.GLCore.Minimap;
import deneme.Interfaces.IMinimapBuilder;

public class MinimapBuilder implements IMinimapBuilder {

	private final Minimap minimap = new Minimap();

	@Override
	public IMinimapBuilder CanClosable(boolean canClosable) {
		minimap.setClosable(canClosable);
		return this;
	}

	@Override
	public IMinimapBuilder Fraction(double fraction) {
		minimap.setFraction(fraction);
		return this;
	}

	@Override
	public IMinimapBuilder borderColor(Color color) {
		minimap.setBorderColor(color);
		return this;
	}

	@Override
	public IMinimapBuilder squareColor(Color color) {
		minimap.setSquareColor(color);
		return this;
	}

	@Override
	public Minimap build() {
		return minimap;
	}
}
