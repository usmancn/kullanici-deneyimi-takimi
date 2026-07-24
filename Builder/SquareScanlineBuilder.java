package deneme.Builder;

import java.awt.Color;

import deneme.Graph.Square.ScanLine;
import deneme.Interfaces.IDrawSquareScanlineBuilder;

public class SquareScanlineBuilder implements IDrawSquareScanlineBuilder {

	private final ScanLine scanLine = new ScanLine();

	@Override
	public IDrawSquareScanlineBuilder LineColor(Color color) {
		scanLine.setLineColor(color);
		return this;
	}

	@Override
	public IDrawSquareScanlineBuilder LineTickness(float tickness) {
		scanLine.setThickness(tickness);
		return this;
	}

	@Override
	public ScanLine build() {
		return scanLine;
	}
}
