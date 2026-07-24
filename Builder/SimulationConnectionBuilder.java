package deneme.Builder;

import deneme.Buffers.SimulationConnection;
import deneme.Interfaces.ISimulationConnectionBuilder;

public class SimulationConnectionBuilder implements ISimulationConnectionBuilder {

	private int queueCapacity = 0;   // sinirsiz

	@Override
	public ISimulationConnectionBuilder queueCapacity(int capacity) {
		this.queueCapacity = capacity;
		return this;
	}

	@Override
	public SimulationConnection build() {
		return new SimulationConnection(queueCapacity);
	}
}
