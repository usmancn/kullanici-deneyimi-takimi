package deneme.Interfaces;

import java.awt.Color;

import deneme.Graph.Circular.ScanLine;

public interface IDrawCircularScanlineBuilder {
	IDrawCircularScanlineBuilder LineColor(Color color);
	IDrawCircularScanlineBuilder LineTickness(float tickness);
	IDrawCircularScanlineBuilder StepCount(int step);   // scan halkasinin segment sayisi

	ScanLine build();
}
