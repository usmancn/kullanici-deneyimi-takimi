package deneme.Interfaces;

import deneme.App.RadarApp;
import deneme.App.SliderPosition;

/**
 * Uygulamanin builder'i: hangi canvas'larin olacagini tutar ve pencereyi ona
 * gore kurar (grafik menusunde sadece eklenen canvas'lar gorunur).
 *
 * <p>Zorunlu alanlar: en az bir canvas + simulasyon. Simulasyon baglantisi da
 * zorunludur ama verilmezse default'u uretilir. Gain slider opsiyoneldir;
 * eklenirse boyutu ve ekranin ustunde mi altinda mi duracagi secilebilir.
 */
public interface IAppBuilder {
	IAppBuilder title(String title);

	/** ZORUNLU: simulasyon. */
	IAppBuilder simulation(ISimulationBuilder builder);

	/** Simulasyon-canvas hatti; verilmezse default uretilir (uretim zorunlu). */
	IAppBuilder simulationConnection(ISimulationConnectionBuilder builder);

	/** Canvas-detection hatti; verilmezse default uretilir. */
	IAppBuilder detectionConnection(IDetectionConnectionBuilder builder);

	/** Detection algoritmasi calissin mi (default: acik). */
	IAppBuilder hasDetection(boolean hasDetection);

	// ---- canvas'lar: en az biri ZORUNLU ----
	IAppBuilder addSquareCanvas(ISquareCanvasBuilder builder);
	IAppBuilder addCircularCanvas(ICircularCanvasBuilder builder);
	IAppBuilder addLineCanvas(ILineCanvasBuilder builder);
	IAppBuilder addWaterfallCanvas(IWaterfallCanvasBuilder builder);

	// ---- gain slider: opsiyonel ----
	IAppBuilder gainSlider(IGainSliderBuilder builder);
	IAppBuilder gainSliderSize(int width, int height);
	IAppBuilder gainSliderPosition(SliderPosition position);

	/** Canvas cizim dongusu FPS'i (default 40). */
	IAppBuilder animatorFps(int fps);

	RadarApp build();
}
