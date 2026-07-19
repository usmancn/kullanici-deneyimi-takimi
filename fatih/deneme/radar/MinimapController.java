package deneme.radar;

/**
 * Minimap uzerinde tik/surukleme ile ana gorunumu tasima.
 * Minimap dunyanin tamamini gosterir; tiklanan nokta ana gorunumun merkezi olur.
 */
public class MinimapController {

    private final Camera camera;
    private boolean dragging = false;

    public MinimapController(Camera camera) { this.camera = camera; }

    public boolean isDragging()          { return dragging; }
    public void setDragging(boolean d)   { dragging = d; }

    /** Tik, minimap'in piksel dikdortgeni icinde mi. */
    public boolean isInside(int screenX, int screenY, int width, int height, boolean visible) {
        if (!visible) return false;
        int minimapWidth  = Math.round(width  * Minimap.FRACTION);
        int minimapHeight = Math.round(height * Minimap.FRACTION);
        return screenX >= 0 && screenX < minimapWidth
            && screenY >= 0 && screenY < minimapHeight;
    }

    public void navigate(int screenX, int screenY, int width, int height) {
        int minimapWidth  = Math.round(width  * Minimap.FRACTION);
        int minimapHeight = Math.round(height * Minimap.FRACTION);
        float fractionX = (float) screenX / minimapWidth;
        float fractionY = 1f - (float) screenY / minimapHeight;
        camera.centerOn(fractionX * Camera.WORLD_SIZE, fractionY * Camera.WORLD_SIZE);
    }
}
