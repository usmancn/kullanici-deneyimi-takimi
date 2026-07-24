package deneme.Builder;

import java.awt.Color;

import deneme.Graph.Circular.GridLayer;
import deneme.Interfaces.IGridCircularBuilder;

public class GridCircularBuilder implements IGridCircularBuilder {

	private final GridLayer grid = new GridLayer();

	@Override
	public IGridCircularBuilder xLineCount(int count) {
		grid.setXLineCount(count);   // aci (radyal) cizgi sayisi
		return this;
	}

	@Override
	public IGridCircularBuilder yLineCount(int count) {
		grid.setYLineCount(count);   // halka sayisi
		return this;
	}

	@Override
	public IGridCircularBuilder lineColor(Color color) {
		grid.setLineColor(color);
		return this;
	}

	@Override
	public GridLayer build() {
		return grid;
	}
}
