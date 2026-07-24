package deneme.Builder;

import java.awt.Color;

import deneme.Graph.Circular.ScanLine;
import deneme.Interfaces.IDrawCircularScanlineBuilder;

public class CircularScanlineBuilder implements IDrawCircularScanlineBuilder {

	private final ScanLine scanLine = new ScanLine();

	@Override
	public IDrawCircularScanlineBuilder LineColor(Color color) {
		scanLine.setLineColor(color);
		return this;
	}

	@Override
	public IDrawCircularScanlineBuilder LineTickness(float tickness) {
		scanLine.setThickness(tickness);
		return this;
	}

	@Override
	public IDrawCircularScanlineBuilder StepCount(int step) {
		scanLine.setStepCount(step);
		return this;
	}

	@Override
	public ScanLine build() {
		return scanLine;
	}
}
