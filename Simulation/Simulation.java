package deneme.Simulation;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import deneme.Detection.TargetIdentifier;
import deneme.MessageProcess.MessagePublisher;
import deneme.MessageProcess.QueueMessage;

public class Simulation implements TargetIdentifier {
	private double[][] data;
	private final MessagePublisher publisher;
	private int targetCount;
	private static final int SCREEN_RESOLUTION = 1000;
	public static final int DEFAULT_FPS = 40;

	private final int fps;

	private Target[] targets;

	private int currentRowIndex = 0;
	private ScheduledExecutorService scheduler;
	public Simulation(int targetCount, MessagePublisher publisher) {
		this(targetCount, publisher, DEFAULT_FPS);
	}
	public Simulation(int targetCount, MessagePublisher publisher, int fps) {
		this.publisher = publisher;
		this.fps = (fps > 0) ? fps : DEFAULT_FPS;
		initializeData();
		placeTarget(targetCount);

	}
	public void start() {
		sendLoop();
	}
	public void sendLoop() {
		int period = 1000 / fps;

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
        if(currentRowIndex == 0) {
        	replaceTargets();
        }
	}
	public void send(QueueMessage message) {
		publisher.publish(message);
	}
	
	/** Bir hedefe yeni yer ararken denenecek en fazla rastgele konum (sonsuz donguyu onler). */
	private static final int MAX_PLACE_ATTEMPTS = 30;

	/** Iki hedef merkezi arasindaki en kucuk mesafe. */
	private static final int MIN_TARGET_DISTANCE = 20;

	/**
	 * Her tam tarama sonunda (currentRowIndex == 0) hedefleri 0.9 - 1.1 kati
	 * araliginda yeni konuma tasir. Konumlar dunya sinirlari icinde kalir ve
	 * hedefler ust uste binmez.
	 *
	 * <p>Sonunda veri gridi <b>bastan uretilir</b> (yeni satir dizileri). Canvas'lar
	 * satir dizilerine referans tuttugu icin bu sart: eski satirlar, scanline o
	 * satira tekrar gelene kadar ekranda eski konumlariyla kalir.
	 */
	public void replaceTargets() {
		Random random = new Random();
		for(int i = 0; i < targetCount; i++) {
			Target target = targets[i];
			if(target == null) continue;

			for(int attempt = 0; attempt < MAX_PLACE_ATTEMPTS; attempt++) {
				int x = (int)((0.9 + 0.2 * random.nextDouble()) * target.getCenterX());
				int y = (int)((0.9 + 0.2 * random.nextDouble()) * target.getTopY());

				x = clampToWorld(x, target.getType().getWidth());
				y = clampToWorld(y, target.getType().getHeight());

				if(collides(i, x, y)) continue;

				target.setCenterX(x);
				target.setTopY(y);
				break;   // yer bulunamazsa hedef eski konumunda kalir
			}
		}
		this.data = buildData();
	}

	/** i. hedef disindaki bir hedefle cakisiyor mu. */
	private boolean collides(int index, int x, int y) {
		for(int j = 0; j < targets.length; j++) {
			if(j == index || targets[j] == null) continue;
			if(Math.abs(x - targets[j].getCenterX()) < MIN_TARGET_DISTANCE
					&& Math.abs(y - targets[j].getTopY()) < MIN_TARGET_DISTANCE) {
				return true;
			}
		}
		return false;
	}

	/** Hedef merkezini, kapladigi alan dunya disina tasmayacak sekilde sinirlar. */
	private int clampToWorld(int value, int size) {
		int half = size / 2;
		if(value < half) return half;
		if(value > SCREEN_RESOLUTION - 1 - half) return SCREEN_RESOLUTION - 1 - half;
		return value;
	}

	/** Gurultu zemini + tum hedeflerin basildigi yeni bir veri gridi uretir. */
	private double[][] buildData() {
		double[][] grid = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];
		Random random = new Random();
		for(int i = 0; i < SCREEN_RESOLUTION; i++) {
			for(int j = 0; j < SCREEN_RESOLUTION; j++) {
				grid[i][j] = 0.2 * random.nextDouble();
			}
		}
		for(Target target : targets) {
			if(target == null) continue;
			stamp(grid, target);
		}
		return grid;
	}

	/** Hedefin dikdortgenini grid'e gain factor'u ile basar. */
	private void stamp(double[][] grid, Target target) {
		int x = target.getCenterX();
		int y = target.getTopY();
		int width = target.getType().getWidth();
		int height = target.getType().getHeight();

		for(int k = x - width / 2; k < x + width / 2; k++) {
			for(int t = y - height / 2; t < y + height / 2; t++) {
				if(t >= 0 && k >= 0 && t < SCREEN_RESOLUTION && k < SCREEN_RESOLUTION) {
					grid[t][k] = target.getGainFactor();
				}
			}
		}
	}
	public void placeTarget(int targetCount) {
		this.targetCount = targetCount;
		this.targets = new Target[targetCount];
		Random random = new Random();

		for(int i = 0; i < targetCount; i++) {
			Target target = new Target();

			for(int attempt = 0; attempt < MAX_PLACE_ATTEMPTS; attempt++) {
				int x = clampToWorld(random.nextInt(SCREEN_RESOLUTION), target.getType().getWidth());
				int y = clampToWorld(random.nextInt(SCREEN_RESOLUTION), target.getType().getHeight());

				// yer bulunamazsa son denenen konum kullanilir (ilk yerlesimde eski konum yok)
				target.setCenterX(x);
				target.setTopY(y);
				if(!collides(i, x, y)) break;
			}
			this.targets[i] = target;
		}

		this.data = buildData();
	}
	
	/**
	 * Dedektorun tespit ettigi (x,y) merkezine denk gelen hedefin ID'sini doner.
	 * Nokta bir hedefin kapladigi bolgeye dusuyorsa: ID varsa ID, yoksa null (false).
	 * Hicbir hedefe denk gelmiyorsa null (false).
	 */
	@Override
	public String identify(int x, int y) {
		Target t = targetAt(x, y, 0);
		if (t == null) return null;
		return t.getID();
	}

	/** Sag tik menusu icin fare toleransi (dunya birimi): kucuk hedefler de tutulabilsin. */
	public static final int PICK_MARGIN = 6;

	/** Verilen noktadaki hedef; yoksa null. */
	public Target targetAt(int x, int y) {
		return targetAt(x, y, PICK_MARGIN);
	}

	private Target targetAt(int x, int y, int margin) {
		for (Target t : targets) {
			if (t == null) continue;
			int w = t.getType().getWidth();
			int h = t.getType().getHeight();
			if (Math.abs(x - t.getCenterX()) <= w / 2 + margin
					&& Math.abs(y - t.getTopY()) <= h / 2 + margin) {
				return t;
			}
		}
		return null;
	}

	public Target findTarget(int centerX, int topY) {
		for(Target t : targets) {
			if(t.getCenterX() == centerX && t.getTopY() == topY) {
				return t;
			}
		}
		return null;
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
