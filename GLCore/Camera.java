package deneme.GLCore;
public class Camera {

    public static final float WORLD_SIZE = 1000f;
    public static final float MIN_VIEW_RANGE = 40f;
    public static final float MAX_VIEW_RANGE = WORLD_SIZE;
    private static final float ZOOM_STEP = 1.15f;
    public static final int DRAG_THRESHOLD = 5;


    private volatile float minX = 0f, maxX = WORLD_SIZE;
    private volatile float minY = 0f, maxY = WORLD_SIZE;

    // pan kaymasindaki kontroller

    private int pressX, pressY;
    private int lastX, lastY;
    private boolean dragging = false;


    public float minX() { 
    	return minX; 
    }
    public float maxX() {
    	return maxX; 
    }
    public float minY() { 
    	return minY; 
    }
    public float maxY() { 
    	return maxY; 
    }

    public float rangeX()  { 
    	return maxX - minX; 
    }
    public float rangeY()  {
    	return maxY - minY; 
    }
    public float centerX() {
    	return (minX + maxX) / 2f; 
    }
    public float centerY() {
    	return (minY + maxY) / 2f; 
    }

    // ekran dunya donusumu

    public float screenToWorldX(int screenX, int width) {
        return minX + (float) screenX / width * rangeX();
    }

    // Y ekseni openGL'de ters
    public float screenToWorldY(int screenY, int height) {
        return minY + (1f - (float) screenY / height) * rangeY();
    }

    //ZOOM
    public void zoom(int screenX, int screenY, int width, int height, boolean zoomIn) {
        float mouseFractionX = (float) screenX / width;         // ekrandaki yatay oran: 0 sol, 1 sag
        float mouseFractionY = 1f - (float) screenY / height;   // ekrandaki dikey oran: 0 asagi, 1 yukari

        float zoomFactor = zoomIn ? (1f / ZOOM_STEP) : ZOOM_STEP;

        float[] newRangeX = zoomAxis(minX, maxX, mouseFractionX, zoomFactor);
        this.minX = newRangeX[0]; this.maxX = newRangeX[1];

        float[] newRangeY = zoomAxis(minY, maxY, mouseFractionY, zoomFactor);
        this.minY = newRangeY[0]; this.maxY = newRangeY[1];
    }

    // tek eksende zoom
    private static float[] zoomAxis(float viewMin, float viewMax, float mouseFraction, float zoomFactor) {
        float currentRange = viewMax - viewMin;
        float worldPointUnderMouse = viewMin + mouseFraction * currentRange;   // imlecin altindaki dunya noktasi

        float newRange = currentRange * zoomFactor;
        if (newRange < MIN_VIEW_RANGE) newRange = MIN_VIEW_RANGE;
        if (newRange > MAX_VIEW_RANGE) newRange = MAX_VIEW_RANGE;

        float newViewMin = worldPointUnderMouse - mouseFraction * newRange;
        float newViewMax = newViewMin + newRange;

        // gorunum boyutu korunur, sadece dunya icine itilir
        if (newViewMin < 0f)         { newViewMax -= newViewMin;                newViewMin = 0f; }
        if (newViewMax > WORLD_SIZE) { newViewMin -= (newViewMax - WORLD_SIZE); newViewMax = WORLD_SIZE; }
        if (newViewMin < 0f)           newViewMin = 0f;

        return new float[] { newViewMin, newViewMax };
    }

    // PAN

    //suruklenme baslangici
    public void panPress(int x, int y) {
        pressX = x; pressY = y;
        lastX = x;  lastY = y;
        dragging = false;
    }

    public boolean isDragging() { 
    	return dragging; 
    }
    public void panRelease()    {
    	dragging = false; 
    }

    //suruklenme
    public void panDrag(int x, int y, int width, int height) {
        if (!dragging) {
            if (Math.abs(x - pressX) < DRAG_THRESHOLD && Math.abs(y - pressY) < DRAG_THRESHOLD) return;
            dragging = true;
        }

        int deltaPixelX = x - lastX;
        int deltaPixelY = y - lastY;
        lastX = x; lastY = y;

        
        float worldDeltaX = -(float) deltaPixelX / width  * rangeX();
        float worldDeltaY =  (float) deltaPixelY / height * rangeY();

        float[] shiftedX = shift(minX, maxX, worldDeltaX);
        this.minX = shiftedX[0]; this.maxX = shiftedX[1];
        float[] shiftedY = shift(minY, maxY, worldDeltaY);
        this.minY = shiftedY[0]; this.maxY = shiftedY[1];
    }

    //araligi verilen noktaya ortalar
    public void centerOn(float targetX, float targetY) {
        float[] shiftedX = shift(minX, maxX, targetX - centerX());
        this.minX = shiftedX[0]; this.maxX = shiftedX[1];
        float[] shiftedY = shift(minY, maxY, targetY - centerY());
        this.minY = shiftedY[0]; this.maxY = shiftedY[1];
    }

    //araligin dunya sinirlari icinde kalmasını saglar.
    public static float[] shift(float viewMin, float viewMax, float delta) {
        float range = viewMax - viewMin;
        viewMin += delta;
        viewMax += delta;
        if (range >= WORLD_SIZE) {
            float excess = range - WORLD_SIZE;
            return new float[] { -excess / 2f, WORLD_SIZE + excess / 2f };
        }
        if (viewMin < 0f)         { viewMin = 0f;         viewMax = range; }
        if (viewMax > WORLD_SIZE) { viewMax = WORLD_SIZE; viewMin = WORLD_SIZE - range; }
        return new float[] { viewMin, viewMax };
    }

    //ZOOM icin
    public void modelMatrix(float[] matrix, float worldCenterX, float worldCenterY, float widthWorld, float heightWorld) {
        range(matrix, worldCenterX, worldCenterY, widthWorld, heightWorld, minX, maxX, minY, maxY);
    }

    //TUM dunya kullanir
    public static void worldMatrix(float[] matrix, float worldCenterX, float worldCenterY, float widthWorld, float heightWorld) {
        range(matrix, worldCenterX, worldCenterY, widthWorld, heightWorld, 0f, WORLD_SIZE, 0f, WORLD_SIZE);
    }

    /**
     * En-boy (aspect) duzeltmesi: pencere kare degilken dunyanin ezilmesini onler.
     * Kare dunya her zaman kare kalir (cember yuvarlak, kenarlar orantili);
     * genis pencerede yanlar, uzun pencerede alt-ust letterbox olur.
     */
    public static void applyAspect(float[] matrix, int width, int height) {
        if (width <= 0 || height <= 0) return;
        float scaleX = 1f, scaleY = 1f;
        if (width >= height) {
            scaleX = (float) height / width;   // genis pencere -> x'i sikistir
        } else {
            scaleY = (float) width / height;   // uzun pencere -> y'yi sikistir
        }
        // NDC x'i olusturan tum terimler *scaleX, NDC y'yi olusturanlar *scaleY
        matrix[0] *= scaleX; matrix[4] *= scaleX; matrix[8]  *= scaleX; matrix[12] *= scaleX;
        matrix[1] *= scaleY; matrix[5] *= scaleY; matrix[9]  *= scaleY; matrix[13] *= scaleY;
    }

    private static void range(float[] matrix, float worldCenterX, float worldCenterY,
                              float widthWorld, float heightWorld,
                              float viewMinX, float viewMaxX, float viewMinY, float viewMaxY) {
        float rangeX = viewMaxX - viewMinX;
        float rangeY = viewMaxY - viewMinY;

        java.util.Arrays.fill(matrix, 0f);
        matrix[0]  = widthWorld / rangeX;                        // x olcegi
        matrix[5]  = heightWorld / rangeY;                       // y olcegi
        matrix[10] = 1f;
        matrix[12] = 2f * (worldCenterX - viewMinX) / rangeX - 1f;   // konum
        matrix[13] = 2f * (worldCenterY - viewMinY) / rangeY - 1f;
        matrix[15] = 1f;
    }
}
