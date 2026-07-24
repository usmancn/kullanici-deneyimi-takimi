package deneme.Interfaces;

import java.awt.Color;

import deneme.Graph.Square.ScanLine;

public interface IDrawSquareScanlineBuilder {
	IDrawSquareScanlineBuilder LineColor(Color color);
	IDrawSquareScanlineBuilder LineTickness(float tickness);

	ScanLine build();
}
