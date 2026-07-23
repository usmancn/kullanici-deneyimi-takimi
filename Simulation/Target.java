package deneme.Simulation;
import java.util.Random;
public class Target {
	private TargetType type;
	private String ID;
	private static int idCounter = 0;
	private int centerX;
	private int topY;
	private double gainFactor;
	public TargetType getType() {
		return type;
	}
	public String getID() {
		return ID;
	}
	public void setID() {
		ID = Integer.toString(idCounter);
		idCounter++;
	}
	public static int getIdCounter() {
		return idCounter;
	}
	public void setType(TargetType type) {
		this.type = type;
	}
	public int getCenterX() {
		return centerX;
	}
	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}
	public int getTopY() {
		return topY;
	}
	public void setTopY(int topY) {
		this.topY = topY;
	}
	public double getGainFactor() {
		return gainFactor;
	}
	public Target() {
		this.type = selectTargetType();
		initGainFactor();
		setID();
	}
	public TargetType selectTargetType() {
		Random random = new Random();
		int i = random.nextInt(3);
		switch(i) {
		case 0:
	        return TargetType.BIG;
		case 1:
	    	return TargetType.SMALL;
		case 2:
	    	return TargetType.RECTANGLE;
		default:
	        return TargetType.BIG;
		}
	}
	
	public void initGainFactor() {
		Random random = new Random();
		this.gainFactor = 0.4 * random.nextDouble() + 0.6;
	}
}
