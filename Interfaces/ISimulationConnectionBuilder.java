package deneme.Interfaces;

import deneme.Buffers.SimulationConnection;

/**
 * Simulasyon ile canvas'lar arasindaki haberlesme hattinin builder'i.
 * App icin uretilmesi ZORUNLUDUR; kullanici vermezse AppBuilder default
 * ayarlarla kendisi uretir.
 */
public interface ISimulationConnectionBuilder {
	/** Canvas basina acilan kuyrugun kapasitesi; 0 veya alti sinirsiz demektir. */
	ISimulationConnectionBuilder queueCapacity(int capacity);

	SimulationConnection build();
}
