package deneme.GLCore;

public final class TargetStyle {

    public final float markRed;
    public final float markGreen;
    public final float markBlue;
    public final float labelRed;
    public final float labelGreen;
    public final float labelBlue;
    public final float markLineWidth;
    public final int labelMarginPx;

    public TargetStyle(
            float markRed,
            float markGreen,
            float markBlue,
            float labelRed,
            float labelGreen,
            float labelBlue,
            float markLineWidth,
            int labelMarginPx
    ) {
        this.markRed = markRed;
        this.markGreen = markGreen;
        this.markBlue = markBlue;
        this.labelRed = labelRed;
        this.labelGreen = labelGreen;
        this.labelBlue = labelBlue;
        this.markLineWidth = markLineWidth;
        this.labelMarginPx = labelMarginPx;
    }

    public static TargetStyle defaults() {
        return new TargetStyle(
                1f, 1f, 0f,
                1f, 1f, 1f,
                2f,
                4
        );
    }
}
