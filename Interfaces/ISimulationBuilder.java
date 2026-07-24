package deneme.Interfaces;

import deneme.Buffers.SimulationConnection;
import deneme.Simulation.Simulation;

public interface ISimulationBuilder {
	ISimulationBuilder FPS(int fps);
	ISimulationBuilder EnemyCount(int enemyCount);

	/** AppBuilder baglar: simulasyonun satirlari basacagi hat. */
	ISimulationBuilder connection(SimulationConnection connection);

	Simulation build();
}
