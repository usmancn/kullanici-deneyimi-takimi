package deneme.Buffers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import deneme.MessageProcess.MessagePublisher;
import deneme.MessageProcess.QueueMessage;

/**
 * Simulasyon ile canvas'lar arasindaki haberlesme hatti.
 *
 * <p>Simulasyon uretilen her satiri {@link #getPublisher() publisher}'a basar;
 * her canvas {@link #register()} ile kendi kuyrugunu acar ve publisher'a abone
 * olur. Boylece her tuketici ayni satirin kendi kopyasini alir.
 * SimulationConnectionBuilder uretir; app icin uretilmesi zorunludur.
 */
public class SimulationConnection {

	private final MessagePublisher publisher = new MessagePublisher();
	private final int queueCapacity;   // <= 0: sinirsiz

	public SimulationConnection() {
		this(0);
	}

	public SimulationConnection(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/** Yeni bir tuketici kuyrugu acar ve publisher'a abone eder. */
	public BlockingQueue<QueueMessage> register() {
		BlockingQueue<QueueMessage> queue = (queueCapacity > 0)
				? new LinkedBlockingQueue<>(queueCapacity)
				: new LinkedBlockingQueue<>();
		publisher.subscribe(queue);
		return queue;
	}

	public MessagePublisher getPublisher() {
		return publisher;
	}
}
