package deneme.MessageProcess;

import java.util.concurrent.BlockingQueue;

public abstract class MessageConsumer {

    private final BlockingQueue<QueueMessage> queue;
    private volatile boolean running = true;
    private Thread consumerThread;

    public MessageConsumer(BlockingQueue<QueueMessage> queue) {
        this.queue = queue;
    }

    public void start() {
        consumerThread = new Thread(this::consumeLoop);
        consumerThread.start();
    }

    private void consumeLoop() {
        while (running) {
            try {
                QueueMessage message = queue.take();

                processMessage(message);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public abstract void processMessage(QueueMessage message);

    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }
}