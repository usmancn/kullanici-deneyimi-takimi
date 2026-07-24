package deneme.Buffers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import deneme.MessageProcess.QueueMessage;

/**
 * Canvas'lara akan veri ile detection algoritmasi (ObjectDetector) arasindaki
 * haberlesme hatti: canvas'larin gordugu ayni satir akisina abone olan ayri
 * bir kuyruk tutar, dedektor bu kuyruktan okur.
 * DetectionConnectionBuilder uretir.
 */
public class DetectionConnection {

	private final BlockingQueue<QueueMessage> queue;

	public DetectionConnection() {
		this(0);
	}

	public DetectionConnection(int queueCapacity) {
		this.queue = (queueCapacity > 0)
				? new LinkedBlockingQueue<>(queueCapacity)
				: new LinkedBlockingQueue<>();
	}

	/** Dedektor kuyrugunu canvas'lari besleyen ayni yayina abone eder. */
	public void attach(SimulationConnection connection) {
		connection.getPublisher().subscribe(queue);
	}

	public BlockingQueue<QueueMessage> getQueue() {
		return queue;
	}
}
