package deneme.Builder;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Waterfall.WaterfallCanvas;
import deneme.Interfaces.IWaterfallCanvasBuilder;
import deneme.MessageProcess.QueueMessage;

/**
 * Waterfall builder'i. Waterfall grid / scanline / mark / ID ozelliklerinin
 * hicbirine sahip olamaz; sadece zemin rengi ve cozunurluk ayarlanir.
 */
public class WaterfallCanvasBuilder implements IWaterfallCanvasBuilder {

	private Color backgroundColor;
	private int resolution = WaterfallCanvas.DEFAULT_RESOLUTION;

	private GLCapabilities caps;
	private BlockingQueue<QueueMessage> queue;

	@Override public IWaterfallCanvasBuilder backgroundColor(Color color) { this.backgroundColor = color; return this; }
	@Override public IWaterfallCanvasBuilder Resolution(int resolution)   { if (resolution > 0) this.resolution = resolution; return this; }

	@Override
	public IWaterfallCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue) {
		this.caps = caps;
		this.queue = queue;
		return this;
	}

	@Override
	public WaterfallCanvas build() {
		if (caps == null || queue == null) {
			throw new IllegalStateException("source(caps, queue) verilmeden canvas kurulamaz (AppBuilder baglar)");
		}
		WaterfallCanvas canvas = new WaterfallCanvas(caps, queue, resolution);
		if (backgroundColor != null) canvas.setBackgroundColor(backgroundColor);
		return canvas;
	}
}
