package deneme.Builder;

import java.awt.Color;

import deneme.Graph.Line.GridLayer;
import deneme.Interfaces.IGridLineBuilder;

public class GridLineBuilder implements IGridLineBuilder {

	private final GridLayer grid = new GridLayer();

	@Override
	public IGridLineBuilder xLineCount(int count) {
		grid.setXLineCount(count);
		return this;
	}

	@Override
	public IGridLineBuilder yLineCount(int count) {
		grid.setYLineCount(count);
		return this;
	}

	@Override
	public IGridLineBuilder lineColor(Color color) {
		grid.setLineColor(color);
		return this;
	}

	@Override
	public IGridLineBuilder labelColor(Color color) {
		grid.setLabelColor(color);
		return this;
	}

	@Override
	public GridLayer build() {
		return grid;
	}
}
