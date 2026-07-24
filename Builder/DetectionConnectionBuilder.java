package deneme.Builder;

import deneme.Buffers.DetectionConnection;
import deneme.Interfaces.IDetectionConnectionBuilder;

public class DetectionConnectionBuilder implements IDetectionConnectionBuilder {

	private int queueCapacity = 0;   // sinirsiz

	@Override
	public IDetectionConnectionBuilder queueCapacity(int capacity) {
		this.queueCapacity = capacity;
		return this;
	}

	@Override
	public DetectionConnection build() {
		return new DetectionConnection(queueCapacity);
	}
}
