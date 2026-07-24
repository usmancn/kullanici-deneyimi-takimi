package deneme.Simulation;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import deneme.Interfaces.RadarDataSource;
import deneme.MessageProcess.MessagePublisher;
import deneme.MessageProcess.QueueMessage;

public class ImageDataSource implements RadarDataSource {

    private static final int SCREEN_RESOLUTION = 1000;

    private final MessagePublisher publisher;
    private final double[][] data;
    private final int fps;

    private int currentRowIndex = 0;
    private ScheduledExecutorService scheduler;

    public ImageDataSource(File imageFile, MessagePublisher publisher, int fps) throws IOException {
        this.publisher = publisher;
        this.fps = fps;
        this.data = loadImageData(imageFile);
    }

    @Override
    public void start() {
        int period = 1000 / fps;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::sendRow,
                0,
                period,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void sendRow() {
        double[] row = data[currentRowIndex];
        publisher.publish(new QueueMessage(currentRowIndex, row));
        currentRowIndex = (currentRowIndex + 1) % SCREEN_RESOLUTION;
    }

    private double[][] loadImageData(File imageFile) throws IOException {
        BufferedImage original = ImageIO.read(imageFile);
        if (original == null) {
            throw new IOException("Desteklenmeyen resim dosyasi: " + imageFile);
        }

        BufferedImage scaled = new BufferedImage(
                SCREEN_RESOLUTION,
                SCREEN_RESOLUTION,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, SCREEN_RESOLUTION, SCREEN_RESOLUTION, null);
        g.dispose();

        double[][] result = new double[SCREEN_RESOLUTION][SCREEN_RESOLUTION];
        for (int y = 0; y < SCREEN_RESOLUTION; y++) {
            for (int x = 0; x < SCREEN_RESOLUTION; x++) {
                int rgb = scaled.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                result[y][x] = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;
            }
        }

        return result;
    }
}
