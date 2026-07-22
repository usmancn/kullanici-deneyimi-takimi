package deneme.MessageProcess;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessagePublisher {

    // birden fazla tuketici (square, waterfall...) kendi queue'suyla dinleyebilir
    private final List<BlockingQueue<QueueMessage>> queues = new CopyOnWriteArrayList<>();

    public MessagePublisher() {
    }

    public MessagePublisher(BlockingQueue<QueueMessage> queue) {
        subscribe(queue);
    }

    public void subscribe(BlockingQueue<QueueMessage> queue) {
        queues.add(queue);
    }

    public void publish(QueueMessage message) {
        for (BlockingQueue<QueueMessage> queue : queues) {
            queue.offer(message);   // her tuketici kendi kopyasini alir
        }
    }
}
