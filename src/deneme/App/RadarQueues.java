package deneme.App;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import deneme.MessageProcess.MessagePublisher;
import deneme.MessageProcess.QueueMessage;

public class RadarQueues {

    public final BlockingQueue<QueueMessage> square = new LinkedBlockingQueue<>();
    public final BlockingQueue<QueueMessage> waterfall = new LinkedBlockingQueue<>();
    public final BlockingQueue<QueueMessage> line = new LinkedBlockingQueue<>();
    public final BlockingQueue<QueueMessage> circular = new LinkedBlockingQueue<>();
    public final BlockingQueue<QueueMessage> detection = new LinkedBlockingQueue<>();

    public void subscribeAll(MessagePublisher publisher) {
        subscribeGraphs(publisher);
        subscribeDetection(publisher);
    }

    public void subscribeGraphs(MessagePublisher publisher) {
        publisher.subscribe(square);
        publisher.subscribe(waterfall);
        publisher.subscribe(line);
        publisher.subscribe(circular);
    }

    public void subscribeDetection(MessagePublisher publisher) {
        publisher.subscribe(detection);
    }
}
