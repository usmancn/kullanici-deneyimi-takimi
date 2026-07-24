package deneme.Interfaces;

import java.awt.Color;

import deneme.Graph.Line.GridLayer;

public interface IGridLineBuilder {
	IGridLineBuilder xLineCount(int count);
	IGridLineBuilder yLineCount(int count);
	IGridLineBuilder lineColor(Color color);
	IGridLineBuilder labelColor(Color color);

	GridLayer build();
}
