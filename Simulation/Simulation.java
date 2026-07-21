package deneme.Simulation;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import deneme.MessageProcess.MessagePublisher;
import deneme.MessageProcess.QueueMessage;

public class Simulation {
	private double[][] data;
	private final MessagePublisher publisher;
	private int targetCount;
	private static final int SCREEN_RESOLUTION = 1000;
	private static final int FPS = 40;
	
	private int currentRowIndex = 0;
	private ScheduledExecutorService scheduler;
	public Simulation(int targetCount, MessagePublisher publisher) {
		this.publisher = publisher;
		initializeData();
		placeTarget(targetCount);
		
	}
	public void start() {
		sendLoop();
	}
	public void sendLoop() {
		int period = SCREEN_RESOLUTION / FPS;

		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(
                this::sendRow,
                0,
                period,
                TimeUnit.MILLISECONDS
        );

	}
	public void stop() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}
	public void sendRow() {
		double[] row = data[currentRowIndex];
		QueueMessage message = new QueueMessage(currentRowIndex, row);
        send(message);
        currentRowIndex = (currentRowIndex + 1) % SCREEN_RESOLUTION;
	}
	public void send(QueueMessage message) {
		publisher.publish(message);
	}
	public void placeTarget(int targetCount) {
		Target [] targets = new Target[targetCount];
		Random random = new Random();
		
		for(int i = 0; i < targetCount; i++) {
			
			int x = random.nextInt(SCREEN_RESOLUTION - 20);
			int y = random.nextInt(SCREEN_RESOLUTION - 20);
			
			if(x < 20) {x = 20;}
			if(y < 20) {y = 20;}
			
			boolean constraint = false;
			
			for(int j = 0; j < i; j++) {
				if(Math.abs(x - targets[j].getCenterX()) < 20 && Math.abs(y - targets[j].getTopY()) < 20)
					constraint = true;
			}
			if(constraint) {
				i--;
			}
			else {
				Target target = new Target();
				target.setCenterX(x);
				target.setTopY(y);
				targets[i] = target;
				for(int k = x - target.getType().getWidth() / 2; k < x + target.getType().getWidth() / 2; k++) {
					for(int t = y - target.getType().getHeight() / 2; t < y + target.getType().getHeight() / 2; t++) {
						this.data[t][k] = target.getGainFactor(); 
					}
				}
			}
		}
		
	}
	public void initializeData() {
		this.data = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];
		Random random = new Random();
		for(int i = 0; i < SCREEN_RESOLUTION; i++) {
			for(int j = 0; j < SCREEN_RESOLUTION; j++) {
				this.data[i][j] = 0.2 * random.nextDouble();
			}
		}
		
	}

}
