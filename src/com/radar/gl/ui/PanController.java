package com.radar.gl.ui;

import com.radar.gl.core.*;


/**
 * Sol tik + surukleme ile gorunumu kaydirir.
 * Esik asilana kadar surukleme baslamaz; boylece kisa tiklamalar
 * (isaret koyma/secme) pan sayilmaz.
 */
public class PanController {

    public static final int DRAG_THRESHOLD = 5;

    private final Camera camera;

    private int pressX, pressY;
    private int lastX, lastY;
    private boolean dragging = false;

    public PanController(Camera camera) { this.camera = camera; }

    public void press(int x, int y) {
        pressX = x; pressY = y;
        lastX = x;  lastY = y;
        dragging = false;
    }

    public boolean isDragging() { return dragging; }
    public void release()       { dragging = false; }

    public void drag(int x, int y, int width, int height) {
        if (!dragging) {
            if (Math.abs(x - pressX) < DRAG_THRESHOLD && Math.abs(y - pressY) < DRAG_THRESHOLD) return;
            dragging = true;
        }

        int deltaPixelX = x - lastX;
        int deltaPixelY = y - lastY;
        lastX = x; lastY = y;

        float worldDeltaX = -(float) deltaPixelX / width  * camera.rangeX();
        float worldDeltaY =  (float) deltaPixelY / height * camera.rangeY();

        float[] shiftedX = Camera.shift(camera.minX(), camera.maxX(), worldDeltaX);
        camera.setRangeX(shiftedX[0], shiftedX[1]);
        float[] shiftedY = Camera.shift(camera.minY(), camera.maxY(), worldDeltaY);
        camera.setRangeY(shiftedY[0], shiftedY[1]);
    }
}
