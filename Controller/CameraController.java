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
    private final CameraControlOptions options;

    private boolean minimapDragging = false;

    public CameraController(Component canvas, Camera camera, Minimap minimap) {
        this(canvas, camera, minimap, CameraControlOptions.defaults());
    }

    public CameraController(Component canvas, Camera camera, Minimap minimap, CameraControlOptions options) {
        this.canvas = canvas;
        this.camera = camera;
        this.minimap = minimap;
        this.options = options;
    }
    
    public CameraController(Component canvas, Camera camera) {
        this(canvas, camera, null, CameraControlOptions.withoutMinimap());
    }

    public void install() {
        canvas.setFocusable(true);
        canvas.setFocusTraversalKeysEnabled(false);

        if (options.zoomEnabled) {
            installZoomControl();
        }

        if (options.panEnabled || options.minimapEnabled) {
            installMouseNavigation();
        }

        if (options.keyboardEnabled) {
            installKeyboardControl();
        }
    }

    private void installZoomControl() {
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
    }

    private void installMouseNavigation() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                if (canUseMinimap()
                        && minimap.contains(e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight())) {
                    minimapDragging = true;
                    minimap.navigate(camera, e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight());
                    return;
                }

                if (options.panEnabled) {
                    camera.panPress(e.getX(), e.getY());
                }
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
            	if (canUseMinimap() && minimapDragging) {
            	    minimap.navigate(camera, e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight());
            	    return;
            	}

                if (!options.panEnabled) {
                    return;
                }

                int side = Viewport.side(canvas.getWidth(), canvas.getHeight());
                camera.panDrag(e.getX(), e.getY(), side, side);
            }
        });
    }

    private void installKeyboardControl() {
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (canUseMinimap() && e.getKeyCode() == KeyEvent.VK_TAB) {
                    minimap.toggle();
                }
            }
        });
    }

    private boolean canUseMinimap() {
        return options.minimapEnabled && minimap != null;
    }
}
