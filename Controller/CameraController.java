package deneme.Controller;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import deneme.GLCore.Camera;
import deneme.GLCore.Minimap;
import deneme.GLCore.Viewport;

public final class CameraController {

    private final Component canvas;
    private final Camera camera;
    private final Minimap minimap;

    private boolean minimapDragging = false;

    public CameraController(Component canvas, Camera camera, Minimap minimap) {
        this.canvas = canvas;
        this.camera = camera;
        this.minimap = minimap;
    }
    
    public CameraController(Component canvas, Camera camera) {
        this(canvas, camera, null);
    }

    public void install() {
        canvas.addMouseWheelListener(e -> {
            boolean zoomIn = e.getWheelRotation() < 0;
            int side = Viewport.side(canvas.getWidth(), canvas.getHeight());

            camera.zoom(
                    Viewport.mouseX(e.getX(), canvas.getWidth(), canvas.getHeight()),
                    Viewport.mouseY(e.getY(), canvas.getWidth(), canvas.getHeight()),
                    side,
                    side,
                    zoomIn
            );
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                if (minimap != null && minimap.contains(e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight())) {
                    minimapDragging = true;
                    minimap.navigate(camera, e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight());
                    return;
                }

                camera.panPress(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                minimapDragging = false;
                camera.panRelease();
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
            	if (minimap != null && minimapDragging) {
            	    minimap.navigate(camera, e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight());
            	    return;
            	}

                int side = Viewport.side(canvas.getWidth(), canvas.getHeight());
                camera.panDrag(e.getX(), e.getY(), side, side);
            }
        });

        canvas.setFocusable(true);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (minimap != null && e.getKeyCode() == KeyEvent.VK_TAB) {
                    minimap.toggle();
                }
            }
        });
    }
}