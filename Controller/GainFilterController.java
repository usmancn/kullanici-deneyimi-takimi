package deneme.Controller;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import deneme.App.GainFilterSlider;

public final class GainFilterController {

    private static final int THUMB_HIT_RADIUS = 12;

    private final GainFilterSlider slider;
    private boolean draggingLow = false;
    private boolean draggingHigh = false;

    public GainFilterController(GainFilterSlider slider) {
        this.slider = slider;
    }

    public void install() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int lowX = slider.lowThumbX();
                int highX = slider.highThumbX();

                if (Math.abs(x - lowX) < THUMB_HIT_RADIUS && x <= highX) {
                    draggingLow = true;
                } else if (Math.abs(x - highX) < THUMB_HIT_RADIUS) {
                    draggingHigh = true;
                } else if (x < lowX) {
                    slider.setLowValue(slider.valueForX(x));
                    draggingLow = true;
                } else if (x > highX) {
                    slider.setHighValue(slider.valueForX(x));
                    draggingHigh = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingLow = false;
                draggingHigh = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int value = slider.valueForX(e.getX());
                if (draggingLow) {
                    slider.setLowValue(value);
                } else if (draggingHigh) {
                    slider.setHighValue(value);
                }
            }
        };

        slider.addMouseListener(mouseAdapter);
        slider.addMouseMotionListener(mouseAdapter);
    }
}
