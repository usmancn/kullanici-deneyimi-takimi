package deneme.Simulation;

public enum TargetType {
	BIG(20, 20),
	SMALL(10, 10),
	RECTANGLE(20, 10);

	private final int width;
	private final int height;

	TargetType(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public String getTypeName() {
		return this.name();
	}
}
