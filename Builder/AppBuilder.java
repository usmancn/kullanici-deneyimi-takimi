package deneme.Builder;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import deneme.App.GainFilterSlider;
import deneme.App.RadarApp;
import deneme.App.SliderPosition;
import deneme.Buffers.DetectionConnection;
import deneme.Buffers.SimulationConnection;
import deneme.Detection.ObjectDetector;
import deneme.Graph.Circular.CircularCanvas;
import deneme.Graph.Line.LineCanvas;
import deneme.Graph.Square.SquareCanvas;
import deneme.Graph.Waterfall.WaterfallCanvas;
import deneme.Interfaces.IAppBuilder;
import deneme.Interfaces.ICircularCanvasBuilder;
import deneme.Interfaces.IDetectionConnectionBuilder;
import deneme.Interfaces.IGainSliderBuilder;
import deneme.Interfaces.ILineCanvasBuilder;
import deneme.Interfaces.ISimulationBuilder;
import deneme.Interfaces.ISimulationConnectionBuilder;
import deneme.Interfaces.ISquareCanvasBuilder;
import deneme.Interfaces.IWaterfallCanvasBuilder;
import deneme.Simulation.Simulation;

/**
 * Uygulamanin builder'i: tum parcalari birbirine baglayip {@link RadarApp}
 * uretir.
 *
 * <p>Zorunlu: en az bir canvas + simulasyon. SimulationConnection uretimi
 * zorunludur; kullanici builder vermezse default'u uretilir. Mark ozelligi
 * acik olan canvas'lara mark menusu otomatik baglanir (ikisi ayri dusunulemez).
 * Line + Waterfall ikisi birden varsa mevcut duzendeki gibi alt alta tek
 * kartta gosterilir.
 */
public class AppBuilder implements IAppBuilder {

	public static final int DEFAULT_ANIMATOR_FPS = 40;

	private String title = "Radar";
	private ISimulationBuilder simulationBuilder;
	private ISimulationConnectionBuilder simulationConnectionBuilder;
	private IDetectionConnectionBuilder detectionConnectionBuilder;
	private boolean hasDetection = true;

	private ISquareCanvasBuilder squareBuilder;
	private ICircularCanvasBuilder circularBuilder;
	private ILineCanvasBuilder lineBuilder;
	private IWaterfallCanvasBuilder waterfallBuilder;

	private IGainSliderBuilder gainSliderBuilder;
	private int gainSliderWidth = -1;
	private int gainSliderHeight = -1;
	private SliderPosition gainSliderPosition = SliderPosition.BOTTOM;

	private int animatorFps = DEFAULT_ANIMATOR_FPS;

	@Override public IAppBuilder title(String title) { if (title != null) this.title = title; return this; }
	@Override public IAppBuilder simulation(ISimulationBuilder builder) { this.simulationBuilder = builder; return this; }
	@Override public IAppBuilder simulationConnection(ISimulationConnectionBuilder builder) { this.simulationConnectionBuilder = builder; return this; }
	@Override public IAppBuilder detectionConnection(IDetectionConnectionBuilder builder) { this.detectionConnectionBuilder = builder; return this; }
	@Override public IAppBuilder hasDetection(boolean hasDetection) { this.hasDetection = hasDetection; return this; }

	@Override public IAppBuilder addSquareCanvas(ISquareCanvasBuilder builder)       { this.squareBuilder = builder; return this; }
	@Override public IAppBuilder addCircularCanvas(ICircularCanvasBuilder builder)   { this.circularBuilder = builder; return this; }
	@Override public IAppBuilder addLineCanvas(ILineCanvasBuilder builder)           { this.lineBuilder = builder; return this; }
	@Override public IAppBuilder addWaterfallCanvas(IWaterfallCanvasBuilder builder) { this.waterfallBuilder = builder; return this; }

	@Override public IAppBuilder gainSlider(IGainSliderBuilder builder) { this.gainSliderBuilder = builder; return this; }
	@Override public IAppBuilder gainSliderSize(int width, int height)  { this.gainSliderWidth = width; this.gainSliderHeight = height; return this; }
	@Override public IAppBuilder gainSliderPosition(SliderPosition position) { if (position != null) this.gainSliderPosition = position; return this; }

	@Override public IAppBuilder animatorFps(int fps) { if (fps > 0) this.animatorFps = fps; return this; }

	@Override
	public RadarApp build() {
		// ---- zorunlu alan kontrolleri ----
		if (squareBuilder == null && circularBuilder == null
				&& lineBuilder == null && waterfallBuilder == null) {
			throw new IllegalStateException("En az bir canvas eklenmeli");
		}
		if (simulationBuilder == null) {
			throw new IllegalStateException("Simulasyon zorunlu: simulation(...) verilmeli");
		}

		// ---- simulasyon hatti: uretimi zorunlu, verilmezse default ----
		if (simulationConnectionBuilder == null) {
			simulationConnectionBuilder = new SimulationConnectionBuilder();
		}
		SimulationConnection connection = simulationConnectionBuilder.build();

		Simulation simulation = simulationBuilder.connection(connection).build();

		// heavyweight popup: GLCanvas uzerinde hafif popup'lar gorunmez
		javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);

		// ---- canvas'lar ----
		GLProfile profile = GLProfile.get(GLProfile.GL2);
		GLCapabilities caps = new GLCapabilities(profile);

		SquareCanvas squareCanvas = null;
		CircularCanvas circularCanvas = null;
		LineCanvas lineCanvas = null;
		WaterfallCanvas waterfallCanvas = null;

		if (squareBuilder != null) {
			squareCanvas = squareBuilder.source(caps, connection.register()).build();
			if (squareCanvas.hasMark()) squareCanvas.installMarkMenu(simulation);
		}
		if (circularBuilder != null) {
			circularCanvas = circularBuilder.source(caps, connection.register()).build();
			if (circularCanvas.hasMark()) circularCanvas.installMarkMenu(simulation);
		}
		if (lineBuilder != null) {
			lineCanvas = lineBuilder.source(caps, connection.register()).build();
		}
		if (waterfallBuilder != null) {
			waterfallCanvas = waterfallBuilder.source(caps, connection.register()).build();
		}

		// ---- kartlar: menude sadece eklenen canvas'lar gorunur ----
		List<RadarApp.Card> cards = new ArrayList<>();
		if (squareCanvas != null) {
			cards.add(new RadarApp.Card("Square", squareCanvas));
		}
		if (lineCanvas != null && waterfallCanvas != null) {
			// ikisi birden varsa alt alta tek kart: her biri yarim yukseklikte cizer
			JPanel lineWaterfall = new JPanel(new GridLayout(2, 1));
			lineWaterfall.add(lineCanvas);        // ustte line
			lineWaterfall.add(waterfallCanvas);   // altta waterfall
			cards.add(new RadarApp.Card("Line + Waterfall", lineWaterfall,
					Arrays.asList((GLCanvas) lineCanvas, waterfallCanvas)));
		} else if (lineCanvas != null) {
			cards.add(new RadarApp.Card("Line", lineCanvas));
		} else if (waterfallCanvas != null) {
			cards.add(new RadarApp.Card("Waterfall", waterfallCanvas));
		}
		if (circularCanvas != null) {
			cards.add(new RadarApp.Card("Circular", circularCanvas));
		}

		// ---- detection: canvas verisi ile dedektor arasindaki hat ----
		ObjectDetector detector = null;
		if (hasDetection) {
			if (detectionConnectionBuilder == null) {
				detectionConnectionBuilder = new DetectionConnectionBuilder();
			}
			DetectionConnection detectionConnection = detectionConnectionBuilder.build();
			detectionConnection.attach(connection);
			// obje bulununca ID'yi Simulation'a sorar
			detector = new ObjectDetector(detectionConnection.getQueue(), simulation);
		}

		// ---- gain slider: opsiyonel; boyut ve konum app seviyesinde secilir ----
		GainFilterSlider gainSlider = null;
		if (gainSliderBuilder != null) {
			gainSlider = gainSliderBuilder.build();
			if (gainSliderWidth > 0 && gainSliderHeight > 0) {
				gainSlider.setSliderSize(gainSliderWidth, gainSliderHeight);
			}
		}

		return new RadarApp(title, simulation, detector, cards,
				gainSlider, gainSliderPosition, animatorFps);
	}
}
