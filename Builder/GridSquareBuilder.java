package deneme.Builder;

import java.awt.Color;

import deneme.Graph.Square.GridLayer;
import deneme.Interfaces.IGridSquareBuilder;

public class GridSquareBuilder implements IGridSquareBuilder {

	private final GridLayer grid = new GridLayer();

	@Override
	public IGridSquareBuilder xLineCount(int count) {
		grid.setXLineCount(count);
		return this;
	}

	@Override
	public IGridSquareBuilder yLineCount(int count) {
		grid.setYLineCount(count);
		return this;
	}

	@Override
	public IGridSquareBuilder lineColor(Color color) {
		grid.setLineColor(color);
		return this;
	}

	@Override
	public GridLayer build() {
		return grid;
	}
}
