package deneme.Interfaces;

import java.awt.Color;

import deneme.Graph.Circular.GridLayer;

public interface IGridCircularBuilder {
	IGridCircularBuilder xLineCount(int count);
	IGridCircularBuilder yLineCount(int count);
	IGridCircularBuilder lineColor(Color color);
	
	GridLayer build();
}
