package deneme.MessageProcess;

import java.util.concurrent.BlockingQueue;

public class MessagePublisher {

    private final BlockingQueue<QueueMessage> queue;

    public MessagePublisher(BlockingQueue<QueueMessage> queue) {
        this.queue = queue;
    }

    public void publish(QueueMessage message) {
        queue.offer(message);
    }
}
