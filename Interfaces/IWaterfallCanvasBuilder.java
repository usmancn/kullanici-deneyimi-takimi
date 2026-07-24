package deneme.Interfaces;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Waterfall.WaterfallCanvas;
import deneme.MessageProcess.QueueMessage;

/** Waterfall grid / scanline / mark / ID ozelliklerinin hicbirine sahip olamaz. */
public interface IWaterfallCanvasBuilder {
	IWaterfallCanvasBuilder backgroundColor(Color color);
	IWaterfallCanvasBuilder Resolution(int resolution);

	/** AppBuilder baglar: GL ayarlari + simulasyondan gelen kuyruk. */
	IWaterfallCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue);

	WaterfallCanvas build();
}
