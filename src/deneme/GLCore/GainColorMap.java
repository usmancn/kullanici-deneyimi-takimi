package deneme.GLCore;

public final class GainColorMap {

    public final float lowRed;
    public final float lowGreen;
    public final float lowBlue;
    public final float highRed;
    public final float highGreen;
    public final float highBlue;
    public final float backgroundRed;
    public final float backgroundGreen;
    public final float backgroundBlue;

    public GainColorMap(
            float lowRed,
            float lowGreen,
            float lowBlue,
            float highRed,
            float highGreen,
            float highBlue,
            float backgroundRed,
            float backgroundGreen,
            float backgroundBlue
    ) {
        this.lowRed = lowRed;
        this.lowGreen = lowGreen;
        this.lowBlue = lowBlue;
        this.highRed = highRed;
        this.highGreen = highGreen;
        this.highBlue = highBlue;
        this.backgroundRed = backgroundRed;
        this.backgroundGreen = backgroundGreen;
        this.backgroundBlue = backgroundBlue;
    }

    public static GainColorMap green() {
        return new GainColorMap(
                0f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 0f
        );
    }
}
