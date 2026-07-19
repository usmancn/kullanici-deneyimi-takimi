package deneme.sim;

public class GainFilter {

    private volatile float minGain = 0.0f;
    private volatile float maxGain = 1.0f;
    public void setMinGain(float minGain) {
        this.minGain = minGain;
    }

    public void setMaxGain(float maxGain) {
        this.maxGain = maxGain;
    }

    public float getMinGain() {
        return minGain;
    }

    public float getMaxGain() {
        return maxGain;
    }

    public boolean accepts(float gain) {
        return gain >= minGain && gain <= maxGain;
    }
}