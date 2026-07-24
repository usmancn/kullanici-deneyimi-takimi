package deneme.Builder;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Circular.CircularCanvas;
import deneme.Interfaces.ICircularCanvasBuilder;
import deneme.Interfaces.IDrawCircularScanlineBuilder;
import deneme.Interfaces.IGridCircularBuilder;
import deneme.Interfaces.IIDBuilder;
import deneme.Interfaces.IMarkBuilder;
import deneme.Interfaces.IMarkMenuRenderer;
import deneme.Interfaces.IMinimapBuilder;
import deneme.MessageProcess.QueueMessage;

/**
 * Dairesel (PPI) canvas builder'i. Ozellikler (grid / scanline / mark / ID /
 * minimap) default olarak ACIK gelir; has...(false) kapatir, alt builder
 * verilirse onun ayarlari kullanilir.
 */
public class CircularCanvasBuilder implements ICircularCanvasBuilder {

	private Color backgroundColor;
	private Color firstColor;
	private Color lastColor;
	private int resolution = CircularCanvas.DEFAULT_RESOLUTION;

	private GLCapabilities caps;
	private BlockingQueue<QueueMessage> queue;

	private boolean hasGrid = true;
	private IGridCircularBuilder gridBuilder;
	private boolean hasScanline = true;
	private IDrawCircularScanlineBuilder scanlineBuilder;
	private boolean hasMark = true;
	private IMarkBuilder markBuilder;
	private IMarkMenuRenderer markMenuBuilder;
	private boolean hasID = true;
	private IIDBuilder idBuilder;
	private boolean hasMinimap = true;
	private IMinimapBuilder minimapBuilder;

	@Override public ICircularCanvasBuilder backgroundColor(Color color) { this.backgroundColor = color; return this; }
	@Override public ICircularCanvasBuilder Resolution(int resolution)   { if (resolution > 0) this.resolution = resolution; return this; }
	@Override public ICircularCanvasBuilder firstColor(Color color)      { this.firstColor = color; return this; }
	@Override public ICircularCanvasBuilder lastColor(Color color)       { this.lastColor = color; return this; }

	@Override
	public ICircularCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
		this.caps = caps;
		this.queue = queue;
		return this;
	}

	@Override public ICircularCanvasBuilder hasGrid(boolean hasGrid)         { this.hasGrid = hasGrid; return this; }
	@Override public ICircularCanvasBuilder grid(IGridCircularBuilder builder) { this.gridBuilder = builder; return this; }
	@Override public ICircularCanvasBuilder hasScanline(boolean hasScanline) { this.hasScanline = hasScanline; return this; }
	@Override public ICircularCanvasBuilder scanline(IDrawCircularScanlineBuilder builder) { this.scanlineBuilder = builder; return this; }
	@Override public ICircularCanvasBuilder hasMark(boolean hasMark)         { this.hasMark = hasMark; return this; }
	@Override public ICircularCanvasBuilder mark(IMarkBuilder builder)       { this.markBuilder = builder; return this; }
	@Override public ICircularCanvasBuilder markMenu(IMarkMenuRenderer builder) { this.markMenuBuilder = builder; return this; }
	@Override public ICircularCanvasBuilder hasID(boolean hasID)             { this.hasID = hasID; return this; }
	@Override public ICircularCanvasBuilder id(IIDBuilder builder)           { this.idBuilder = builder; return this; }
	@Override public ICircularCanvasBuilder hasMinimap(boolean hasMinimap)   { this.hasMinimap = hasMinimap; return this; }
	@Override public ICircularCanvasBuilder minimap(IMinimapBuilder builder) { this.minimapBuilder = builder; return this; }

	@Override
	public CircularCanvas build() {
		if (caps == null || queue == null) {
			throw new IllegalStateException("source(caps, queue) verilmeden canvas kurulamaz (AppBuilder baglar)");
		}
		CircularCanvas canvas = new CircularCanvas(caps, queue, resolution);
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
