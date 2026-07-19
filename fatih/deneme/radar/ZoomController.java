package deneme.radar;

/**
 * Fare tekerlegi ile yaklasma/uzaklasma.
 * Farenin bulundugu bolgeye gore hangi kenarin sabit kalacagini secer
 * ve kamerayi gunceller. (Zoom, kendi basina ayri bir bilesen.)
 */
public class ZoomController {

    /** Bu oranin altinda alt/sol, ustunde ust/sag kenar sabitlenir. */
    private static final float ZONE_LOW  = 0.4f;
    private static final float ZONE_HIGH = 0.6f;

    /** Tekerlegin bir tiklamasinda araligin carpani. */
    private static final float ZOOM_STEP = 1.15f;

    private static final int ANCHOR_LOW = -1, ANCHOR_CENTER = 0, ANCHOR_HIGH = 1;

    private final Camera camera;

    public ZoomController(Camera camera) { this.camera = camera; }

    public void zoom(int screenX, int screenY, int width, int height, boolean zoomIn) {
        float fractionX = (float) screenX / width;
        float fractionY = 1f - (float) screenY / height;

        float factor = zoomIn ? (1f / ZOOM_STEP) : ZOOM_STEP;

        float[] rangeX = { camera.minX(), camera.maxX() };
        zoomRange(rangeX, factor, anchorFor(fractionX));
        camera.setRangeX(rangeX[0], rangeX[1]);

        float[] rangeY = { camera.minY(), camera.maxY() };
        zoomRange(rangeY, factor, anchorFor(fractionY));
        camera.setRangeY(rangeY[0], rangeY[1]);
    }

    private static int anchorFor(float fraction) {
        if (fraction < ZONE_LOW)  return ANCHOR_LOW;
        if (fraction > ZONE_HIGH) return ANCHOR_HIGH;
        return ANCHOR_CENTER;
    }

    private static void zoomRange(float[] minMax, float factor, int anchor) {
        float min = minMax[0];
        float max = minMax[1];

        float newRange = (max - min) * factor;
        if (newRange < Camera.MIN_VIEW_RANGE) newRange = Camera.MIN_VIEW_RANGE;
        if (newRange > Camera.WORLD_SIZE)     newRange = Camera.WORLD_SIZE;

        if (anchor == ANCHOR_LOW) {
            max = min + newRange;
        } else if (anchor == ANCHOR_HIGH) {
            min = max - newRange;
        } else {
            float center = (min + max) / 2f;
            min = center - newRange / 2f;
            max = center + newRange / 2f;
        }

        if (min < 0f) { max -= min; min = 0f; }
        if (max > Camera.WORLD_SIZE) { min -= (max - Camera.WORLD_SIZE); max = Camera.WORLD_SIZE; }
        if (min < 0f) min = 0f;

        minMax[0] = min;
        minMax[1] = max;
    }
}
