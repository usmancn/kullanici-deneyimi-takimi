package deneme.Simulation;
import java.util.Random;
public class Target {
	private TargetType type;
	private String ID;
	private static int idCounter = 0;
	private boolean hasID;
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
		ID = type.getTypeName() + Integer.toString(idCounter);
		setIdCounter();
	}
	public static int getIdCounter() {
		return idCounter;
	}
	private void setIdCounter() {
		idCounter++;
	}
	public boolean isHasID() {
		return hasID;
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
		couldHaveID();
		if(this.hasID)
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
	
	public void couldHaveID() {
		Random random = new Random();
		this.hasID = random.nextBoolean();
	}

	

}
