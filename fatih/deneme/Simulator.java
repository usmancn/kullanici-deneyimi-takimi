package deneme;

import java.util.Random;

public class Simulator {
	private int count;
	private volatile int[][] snapshot;
	
	
	private static final int SCREEN_RESOLUTION = 1000;
	private static final int ESCAPE_DISTANCE = 5;
	
	
	public Simulator(int count) {
		this.count = count;
		this.snapshot = simulate();
	}
	
	public int[][] getSnapshot() { return snapshot; }
    public int getResolution()   { return 1000; }
    
    
	public int[][] simulate() {

	    int min = ESCAPE_DISTANCE;
	    int max = SCREEN_RESOLUTION - ESCAPE_DISTANCE;

	    int[][] points = new int[count][2];
	    Random random = new Random();

	    int placed = 0;

	    while (placed < count) {

	        int x = min + random.nextInt(max - min);
	        int y = min + random.nextInt(max - min);

	        boolean overlaps = false;
	        for (int i = 0; i < placed; i++) {
	            if (Math.abs(points[i][0] - x) < ESCAPE_DISTANCE * 2 && Math.abs(points[i][1] - y) < ESCAPE_DISTANCE * 2) {
	                overlaps = true;
	                break;
	            }
	        }

	        if (!overlaps) {
	            points[placed][0] = x;
	            points[placed][1] = y;
	            placed++;
	        }
	    }

	    return points;
	}

}
