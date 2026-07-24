package deneme.Interfaces;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Line.LineCanvas;
import deneme.MessageProcess.QueueMessage;

/** Cizgi grafigi: opsiyonel ozellik olarak SADECE grid'e sahip olabilir. */
public interface ILineCanvasBuilder {
	ILineCanvasBuilder backgroundColor(Color color);
	ILineCanvasBuilder lineColor(Color color);
	ILineCanvasBuilder averageLineColor(Color color);
	ILineCanvasBuilder Resolution(int resolution);

	/** AppBuilder baglar: GL ayarlari + simulasyondan gelen kuyruk. */
	ILineCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue);

	ILineCanvasBuilder hasGrid(boolean hasGrid);
	ILineCanvasBuilder grid(IGridLineBuilder builder);

	LineCanvas build();
}
