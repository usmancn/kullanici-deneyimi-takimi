package deneme.MessageProcess;

public class QueueMessage {
	private int row;
	private double [] data;
	public QueueMessage(int row, double[] data) {
		this.row = row;
		this.data = data;
	}
	public double[] getData() {
		return this.data;
	}
	public int getRow() {
		return this.row;
	}
}
