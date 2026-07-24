package deneme.Interfaces;

import deneme.Buffers.DetectionConnection;

/**
 * Canvas'lara akan veri ile detection algoritmasi (ObjectDetector) arasindaki
 * haberlesme hattinin builder'i.
 */
public interface IDetectionConnectionBuilder {
	/** Dedektor kuyrugunun kapasitesi; 0 veya alti sinirsiz demektir. */
	IDetectionConnectionBuilder queueCapacity(int capacity);

	DetectionConnection build();
}
