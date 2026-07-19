package deneme.radar;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kullanicinin M tusu basiliyken tiklayarak koydugu isaretler.
 * Isaret ekleme, secme (cembere yakin tik) ve secili olani silme.
 */
public class MarkController {

    public static final float MARK_RADIUS = 20f;
    private static final float PICK_TOLERANCE = 15f;

    private final List<float[]> marks = new CopyOnWriteArrayList<>();
    private volatile int selected = -1;
    private volatile boolean markKeyDown = false;

    public List<float[]> marks()        { return marks; }
    public int selected()               { return selected; }
    public boolean isMarkKeyDown()      { return markKeyDown; }
    public void setMarkKeyDown(boolean down) { markKeyDown = down; }

    public void add(float worldX, float worldY) {
        marks.add(new float[] { worldX, worldY });
        selected = marks.size() - 1;
    }

    public void selectAt(float worldX, float worldY) {
        selected = pick(worldX, worldY);
    }

    public void removeSelected() {
        int index = selected;
        if (index >= 0 && index < marks.size()) {
            marks.remove(index);
            selected = -1;
        }
    }

    /** Cemberin kenarina yeterince yakin tik varsa o isaretin indeksi; yoksa -1. */
    private int pick(float worldX, float worldY) {
        for (int i = 0; i < marks.size(); i++) {
            float[] mark = marks.get(i);
            float dx = worldX - mark[0];
            float dy = worldY - mark[1];
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (Math.abs(distance - MARK_RADIUS) <= PICK_TOLERANCE) return i;
        }
        return -1;
    }
}
