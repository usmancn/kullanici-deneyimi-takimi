package deneme.Interfaces;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Circular.CircularCanvas;
import deneme.MessageProcess.QueueMessage;

/**
 * Dairesel (PPI) radar canvas'i: grid'e, scanline'a, mark'a (mark menusu
 * otomatik gelir) ve ID'ye sahip olabilir. has...(false) ozelligi tamamen
 * kapatir; ...builder verilmezse ozellik default ayarlariyla acik gelir.
 */
public interface ICircularCanvasBuilder {
	ICircularCanvasBuilder backgroundColor(Color color);
	ICircularCanvasBuilder Resolution(int resolution);
	ICircularCanvasBuilder firstColor(Color color);
	ICircularCanvasBuilder lastColor(Color color);

	/** AppBuilder baglar: GL ayarlari + simulasyondan gelen kuyruk. */
	ICircularCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue);

	ICircularCanvasBuilder hasGrid(boolean hasGrid);
	ICircularCanvasBuilder grid(IGridCircularBuilder builder);

	ICircularCanvasBuilder hasScanline(boolean hasScanline);
	ICircularCanvasBuilder scanline(IDrawCircularScanlineBuilder builder);

	/** Mark ve mark menusu birlikte acilir/kapanir. */
	ICircularCanvasBuilder hasMark(boolean hasMark);
	ICircularCanvasBuilder mark(IMarkBuilder builder);
	ICircularCanvasBuilder markMenu(IMarkMenuRenderer builder);

	ICircularCanvasBuilder hasID(boolean hasID);
	ICircularCanvasBuilder id(IIDBuilder builder);

	ICircularCanvasBuilder hasMinimap(boolean hasMinimap);
	ICircularCanvasBuilder minimap(IMinimapBuilder builder);

	CircularCanvas build();
}
