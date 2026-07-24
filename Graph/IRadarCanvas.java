package deneme.Graph;

/**
 * Tum radar canvas'larinin ortak kontratı: simulasyondan gelen kuyrugu
 * tuketmeye baslama/durdurma. RadarApp acilis/kapanista bunlari cagirir.
 */
public interface IRadarCanvas {
	void startConsuming();
	void stopConsuming();
}
