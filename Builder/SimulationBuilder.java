package deneme.Builder;

import deneme.Buffers.SimulationConnection;
import deneme.Interfaces.ISimulationBuilder;
import deneme.Simulation.Simulation;

public class SimulationBuilder implements ISimulationBuilder {

	public static final int DEFAULT_ENEMY_COUNT = 15;

	private int fps = Simulation.DEFAULT_FPS;
	private int enemyCount = DEFAULT_ENEMY_COUNT;
	private SimulationConnection connection;

	@Override
	public ISimulationBuilder FPS(int fps) {
		if (fps > 0) this.fps = fps;
		return this;
	}

	@Override
	public ISimulationBuilder EnemyCount(int enemyCount) {
		if (enemyCount >= 0) this.enemyCount = enemyCount;
		return this;
	}

	@Override
	public ISimulationBuilder connection(SimulationConnection connection) {
		this.connection = connection;
		return this;
	}

	@Override
	public Simulation build() {
		if (connection == null) {
			throw new IllegalStateException("connection(...) verilmeden simulasyon kurulamaz (AppBuilder baglar)");
		}
		return new Simulation(enemyCount, connection.getPublisher(), fps);
	}
}
