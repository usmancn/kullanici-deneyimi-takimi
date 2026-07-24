package deneme.Interfaces;

import java.awt.Color;

import deneme.Graph.Square.GridLayer;

public interface IGridSquareBuilder {
	IGridSquareBuilder xLineCount(int count);
	IGridSquareBuilder yLineCount(int count);
	IGridSquareBuilder lineColor(Color color);
	
	GridLayer build();
}
