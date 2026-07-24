package deneme.Builder;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Square.SquareCanvas;
import deneme.Interfaces.IDrawSquareScanlineBuilder;
import deneme.Interfaces.IGridSquareBuilder;
import deneme.Interfaces.IIDBuilder;
import deneme.Interfaces.IMarkBuilder;
import deneme.Interfaces.IMarkMenuRenderer;
import deneme.Interfaces.IMinimapBuilder;
import deneme.Interfaces.ISquareCanvasBuilder;
import deneme.MessageProcess.QueueMessage;

/**
 * Kare canvas builder'i. Ozellikler (grid / scanline / mark / ID / minimap)
 * default olarak ACIK gelir; has...(false) kapatir, alt builder verilirse
 * onun ayarlari kullanilir.
 */
public class SquareCanvasBuilder implements ISquareCanvasBuilder {

	private Color backgroundColor;
	private Color firstColor;
	private Color lastColor;
	private int resolution = SquareCanvas.DEFAULT_RESOLUTION;

	private GLCapabilities caps;
	private BlockingQueue<QueueMessage> queue;

	private boolean hasGrid = true;
	private IGridSquareBuilder gridBuilder;
	private boolean hasScanline = true;
	private IDrawSquareScanlineBuilder scanlineBuilder;
	private boolean hasMark = true;
	private IMarkBuilder markBuilder;
	private IMarkMenuRenderer markMenuBuilder;
	private boolean hasID = true;
	private IIDBuilder idBuilder;
	private boolean hasMinimap = true;
	private IMinimapBuilder minimapBuilder;

	@Override public ISquareCanvasBuilder backgroundColor(Color color) { this.backgroundColor = color; return this; }
	@Override public ISquareCanvasBuilder Resolution(int resolution)   { if (resolution > 0) this.resolution = resolution; return this; }
	@Override public ISquareCanvasBuilder firstColor(Color color)      { this.firstColor = color; return this; }
	@Override public ISquareCanvasBuilder lastColor(Color color)       { this.lastColor = color; return this; }

	@Override
	public ISquareCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
		this.caps = caps;
		this.queue = queue;
		return this;
	}

	@Override public ISquareCanvasBuilder hasGrid(boolean hasGrid)           { this.hasGrid = hasGrid; return this; }
	@Override public ISquareCanvasBuilder grid(IGridSquareBuilder builder)   { this.gridBuilder = builder; return this; }
	@Override public ISquareCanvasBuilder hasScanline(boolean hasScanline)   { this.hasScanline = hasScanline; return this; }
	@Override public ISquareCanvasBuilder scanline(IDrawSquareScanlineBuilder builder) { this.scanlineBuilder = builder; return this; }
	@Override public ISquareCanvasBuilder hasMark(boolean hasMark)           { this.hasMark = hasMark; return this; }
	@Override public ISquareCanvasBuilder mark(IMarkBuilder builder)         { this.markBuilder = builder; return this; }
	@Override public ISquareCanvasBuilder markMenu(IMarkMenuRenderer builder) { this.markMenuBuilder = builder; return this; }
	@Override public ISquareCanvasBuilder hasID(boolean hasID)               { this.hasID = hasID; return this; }
	@Override public ISquareCanvasBuilder id(IIDBuilder builder)             { this.idBuilder = builder; return this; }
	@Override public ISquareCanvasBuilder hasMinimap(boolean hasMinimap)     { this.hasMinimap = hasMinimap; return this; }
	@Override public ISquareCanvasBuilder minimap(IMinimapBuilder builder)   { this.minimapBuilder = builder; return this; }

	@Override
	public SquareCanvas build() {
		if (caps == null || queue == null) {
			throw new IllegalStateException("source(caps, queue) verilmeden canvas kurulamaz (AppBuilder baglar)");
		}
		SquareCanvas canvas = new SquareCanvas(caps, queue, resolution);
		if (backgroundColor != null) canvas.setBackgroundColor(backgroundColor);
		if (firstColor != null) canvas.setFirstColor(firstColor);
		if (lastColor != null) canvas.setLastColor(lastColor);

		if (!hasGrid) canvas.setGrid(null);
		else if (gridBuilder != null) canvas.setGrid(gridBuilder.build());

		if (!hasScanline) canvas.setScanLine(null);
		else if (scanlineBuilder != null) canvas.setScanLine(scanlineBuilder.build());

		if (!hasMark) canvas.setMarkStyle(null);   // mark menusu de kurulmaz
		else if (markBuilder != null) canvas.setMarkStyle(markBuilder.build());
		if (markMenuBuilder != null) canvas.setMarkMenuStyle(markMenuBuilder.build());

		if (!hasID) canvas.setIdLabel(null);
		else if (idBuilder != null) canvas.setIdLabel(idBuilder.build());

		if (!hasMinimap) canvas.setMinimap(null);
		else if (minimapBuilder != null) canvas.setMinimap(minimapBuilder.build());

		// kamera kontrolleri en son kurulur: minimap acik/kapali kararini gorsun
		canvas.installCameraController();

		return canvas;
	}
}
