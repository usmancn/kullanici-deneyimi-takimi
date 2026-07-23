package deneme.Detection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import deneme.GLCore.Mark;
import deneme.MessageProcess.MessageConsumer;
import deneme.MessageProcess.QueueMessage;

/**
 * Scanline tabanli obje dedektoru.
 *
 * <p>Her satir geldiginde gain'i {@link #GAIN_THRESHOLD} (0.6) ve uzeri olan yatay
 * kosulari (run) bulur. Bir run onceki satirdan gelen acik bir obje ile x'te
 * ortusuyorsa o objeye eklenir (bayrak acik kalir, hem saga hem yukari buyur);
 * ortusmuyorsa yeni obje acilir. Bir satirda devam etmeyen (yukari biten) obje
 * kapatilir -> bir obje tespit edilmis olur.
 *
 * <p>Kapanan her obje icin merkezi {@link TargetIdentifier#identify(int, int)}
 * ile Simulation'a sorulur; donen ID (veya null=false) obje ile saklanir. ID
 * varsa {@link Mark#register} ile ekrana isaretlenmek uzere kaydedilir.
 *
 * <p>Kendi consumer thread'inde calisir (MessageConsumer), dolayisiyla tek
 * thread; sonuclar thread-safe kuyrukta tutulur.
 */
public class ObjectDetector extends MessageConsumer {

    private static final double GAIN_THRESHOLD = 0.6;

    /** Onceki satirdan devam eden, henuz kapanmamis obje. */
    private static final class OpenObject {
        int leftColumn;    // objenin en sol sutunu
        int rightColumn;   // objenin en sag sutunu
        int startRow;      // objenin ilk goruldugu satir
        int endRow;        // objenin son goruldugu satir
        boolean continuedThisRow;   // bu satirda devam etti mi

        OpenObject(int leftColumn, int rightColumn, int row) {
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;
            this.startRow = row;
            this.endRow = row;
        }
    }

    private final TargetIdentifier identifier;
    private final List<OpenObject> openObjects = new ArrayList<>();
    private final Queue<DetectedObject> detectedObjects = new ConcurrentLinkedQueue<>();
    private int previousRow = -1;

    public ObjectDetector(BlockingQueue<QueueMessage> queue, TargetIdentifier identifier) {
        super(queue);
        this.identifier = identifier;
    }

    @Override
    public void processMessage(QueueMessage message) {
        processRow(message.getRow(), message.getData());
    }

    /** Bir scanline satirini isler. */
    public void processRow(int rowIndex, double[] rowGains) {
        if (rowGains == null) return;

        // satir indexi geriye gittiyse yeni tarama basladi -> acik objeleri kapat
        if (rowIndex <= previousRow) {
            closeAllOpen();
        }
        previousRow = rowIndex;

        for (OpenObject object : openObjects) {
            object.continuedThisRow = false;
        }

        // bu satirdaki >=GAIN_THRESHOLD yatay kosulari bul ve objelerle birlestir
        int column = 0;
        int rowWidth = rowGains.length;
        while (column < rowWidth) {
            if (rowGains[column] >= GAIN_THRESHOLD) {
                int runStart = column;
                while (column < rowWidth && rowGains[column] >= GAIN_THRESHOLD) {
                    column++;
                }
                mergeRun(runStart, column - 1, rowIndex);   // [runStart, column-1] dahil
            } else {
                column++;
            }
        }

        // bu satirda devam etmeyen objeleri kapat (hem sag hem yukari bitti)
        Iterator<OpenObject> iterator = openObjects.iterator();
        while (iterator.hasNext()) {
            OpenObject object = iterator.next();
            if (!object.continuedThisRow) {
                closeObject(object);
                iterator.remove();
            }
        }
    }

    /** Bir yatay kosuyu (run) x'te ortustugu acik objeye ekler; yoksa yeni obje acar. */
    private void mergeRun(int runStart, int runEnd, int rowIndex) {
        OpenObject match = null;
        for (OpenObject object : openObjects) {
            if (runStart <= object.rightColumn && runEnd >= object.leftColumn) {   // x'te ortusme
                match = object;
                break;
            }
        }
        if (match != null) {
            match.leftColumn = Math.min(match.leftColumn, runStart);
            match.rightColumn = Math.max(match.rightColumn, runEnd);
            match.endRow = rowIndex;
            match.continuedThisRow = true;
        } else {
            OpenObject created = new OpenObject(runStart, runEnd, rowIndex);
            created.continuedThisRow = true;
            openObjects.add(created);
        }
    }

    /** Objeyi kapatir: merkezini Simulation'a sorup sonucu saklar, ID varsa mark'lar. */
    private void closeObject(OpenObject object) {
        int centerX = (object.leftColumn + object.rightColumn) / 2;
        int centerY = (object.startRow + object.endRow) / 2;
        String id = (identifier != null) ? identifier.identify(centerX, centerY) : null;

        detectedObjects.add(new DetectedObject(
                centerX, centerY,
                object.leftColumn, object.rightColumn, object.startRow, object.endRow, id));

        if (id != null) {
            Mark.register(centerX, centerY, id);   // tanimli hedef -> ekranda isaretlenir
        }
    }

    private void closeAllOpen() {
        for (OpenObject object : openObjects) {
            closeObject(object);
        }
        openObjects.clear();
    }

    /** Tespit edilen objeler (thread-safe). Okuyan taraf drain edebilir. */
    public Queue<DetectedObject> getDetected() {
        return detectedObjects;
    }
}
