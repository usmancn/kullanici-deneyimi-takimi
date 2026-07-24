package deneme.Builder;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Line.LineCanvas;
import deneme.Interfaces.IGridLineBuilder;
import deneme.Interfaces.ILineCanvasBuilder;
import deneme.MessageProcess.QueueMessage;

/** Cizgi grafigi builder'i. Tek opsiyonel ozelligi grid'dir (default: acik). */
public class LineCanvasBuilder implements ILineCanvasBuilder {

	private Color backgroundColor;
	private Color lineColor;
	private Color averageLineColor;
	private int resolution = LineCanvas.DEFAULT_RESOLUTION;

	private GLCapabilities caps;
	private BlockingQueue<QueueMessage> queue;

	private boolean hasGrid = true;
	private IGridLineBuilder gridBuilder;

	@Override public ILineCanvasBuilder backgroundColor(Color color)  { this.backgroundColor = color; return this; }
	@Override public ILineCanvasBuilder lineColor(Color color)        { this.lineColor = color; return this; }
	@Override public ILineCanvasBuilder averageLineColor(Color color) { this.averageLineColor = color; return this; }
	@Override public ILineCanvasBuilder Resolution(int resolution)    { if (resolution > 0) this.resolution = resolution; return this; }

	@Override
	public ILineCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
		this.caps = caps;
		this.queue = queue;
		return this;
	}

	@Override public ILineCanvasBuilder hasGrid(boolean hasGrid)       { this.hasGrid = hasGrid; return this; }
	@Override public ILineCanvasBuilder grid(IGridLineBuilder builder) { this.gridBuilder = builder; return this; }

	@Override
	public LineCanvas build() {
		if (caps == null || queue == null) {
			throw new IllegalStateException("source(caps, queue) verilmeden canvas kurulamaz (AppBuilder baglar)");
		}
		LineCanvas canvas = new LineCanvas(caps, queue, resolution);
		if (backgroundColor != null) canvas.setBackgroundColor(backgroundColor);
		if (lineColor != null) canvas.setLineColor(lineColor);
		if (averageLineColor != null) canvas.setAverageLineColor(averageLineColor);

		if (!hasGrid) canvas.setGrid(null);
		else if (gridBuilder != null) canvas.setGrid(gridBuilder.build());

		return canvas;
	}
}
