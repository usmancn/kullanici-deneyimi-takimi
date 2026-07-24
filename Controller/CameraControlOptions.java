package deneme.Controller;

/**
 * CameraController'in hangi kontrolleri kuracagini secer.
 *
 * <p>{@link #defaults()}: zoom + pan + minimap + TAB (square/circular).
 * {@link #withoutMinimap()}: sadece zoom + pan (line/waterfall).
 */
public final class CameraControlOptions {

    public final boolean zoomEnabled;
    public final boolean panEnabled;
    public final boolean minimapEnabled;
    public final boolean keyboardEnabled;

    public CameraControlOptions(
            boolean zoomEnabled,
            boolean panEnabled,
            boolean minimapEnabled,
            boolean keyboardEnabled
    ) {
        this.zoomEnabled = zoomEnabled;
        this.panEnabled = panEnabled;
        this.minimapEnabled = minimapEnabled;
        this.keyboardEnabled = keyboardEnabled;
    }

    public static CameraControlOptions defaults() {
        return new CameraControlOptions(true, true, true, true);
    }

    public static CameraControlOptions withoutMinimap() {
        return new CameraControlOptions(true, true, false, false);
    }
}
