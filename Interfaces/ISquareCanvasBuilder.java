package deneme.Interfaces;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;

import com.jogamp.opengl.GLCapabilities;

import deneme.Graph.Square.SquareCanvas;
import deneme.MessageProcess.QueueMessage;

/**
 * Kare radar canvas'i: grid'e, scanline'a, mark'a (mark menusu otomatik gelir)
 * ve ID'ye sahip olabilir. has...(false) ozelligi tamamen kapatir; ...builder
 * verilmezse ozellik default ayarlariyla acik gelir.
 */
public interface ISquareCanvasBuilder {
	ISquareCanvasBuilder backgroundColor(Color color);
	ISquareCanvasBuilder Resolution(int resolution);
	ISquareCanvasBuilder firstColor(Color color);
	ISquareCanvasBuilder lastColor(Color color);

	/** AppBuilder baglar: GL ayarlari + simulasyondan gelen kuyruk. */
	ISquareCanvasBuilder source(GLCapabilities caps, BlockingQueue<QueueMessage> queue);

	ISquareCanvasBuilder hasGrid(boolean hasGrid);
	ISquareCanvasBuilder grid(IGridSquareBuilder builder);

	ISquareCanvasBuilder hasScanline(boolean hasScanline);
	ISquareCanvasBuilder scanline(IDrawSquareScanlineBuilder builder);

	/** Mark ve mark menusu birlikte acilir/kapanir. */
	ISquareCanvasBuilder hasMark(boolean hasMark);
	ISquareCanvasBuilder mark(IMarkBuilder builder);
	ISquareCanvasBuilder markMenu(IMarkMenuRenderer builder);

	ISquareCanvasBuilder hasID(boolean hasID);
	ISquareCanvasBuilder id(IIDBuilder builder);

	ISquareCanvasBuilder hasMinimap(boolean hasMinimap);
	ISquareCanvasBuilder minimap(IMinimapBuilder builder);

	SquareCanvas build();
}
