package deneme.Simulation;

public class GainFilterModel {

    private volatile float filterMin = 0f;
    private volatile float filterMax = 1f;

    public float filterMin() {
        return filterMin;
    }

    public float filterMax() {
        return filterMax;
    }

    public void setRange(float min, float max) {
        if (min < 0f) {
            min = 0f;
        }
        if (max > 1f) {
            max = 1f;
        }
        if (min > max) {
            min = max;
        }

        this.filterMin = min;
        this.filterMax = max;
    }
}