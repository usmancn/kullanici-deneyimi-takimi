package deneme.Controller;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import deneme.App.MarkMenuStyle;
import deneme.GLCore.Mark;
import deneme.Simulation.Simulation;
import deneme.Simulation.Target;

/**
 * Hedef uzerinde sag tik menusu: Mark / Unmark. (Eski App.MarkMenu'nun
 * controller katmanina tasinmis hali; MarkMenuStyle destegi korunmustur.)
 *
 * <p>Ikisi birbirini disler: hedef isaretli degilse sadece Mark, isaretliyse
 * sadece Unmark tiklanabilir, digeri soluk gorunur. Isaret sari cemberdir;
 * hedefin ID etiketi isaretten bagimsizdir ve her zaman gorunur.
 *
 * <p><b>Heavyweight popup:</b> GLCanvas agir (native) bir AWT bilesenidir;
 * Swing'in varsayilan hafif popup'lari onun altinda kalir ve gorunmez. Bu
 * yuzden popup'lar heavyweight'e zorlanir.
 */
public final class TargetMarkController {

    static {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    }

    private TargetMarkController() {}

    /**
     * Fare olayini dunya koordinatina cevirir (kare grafikte dogrudan, dairesel
     * grafikte polar tersine cevrim). Cizim alani disindaysa null doner.
     */
    public interface WorldMapper {
        int[] toWorld(int eventX, int eventY);
    }

    /** Canvas'a sag tik menusunu default gorunumle baglar. */
    public static void install(Component canvas, Simulation simulation, WorldMapper mapper) {
        install(canvas, simulation, new MarkMenuStyle(), mapper);
    }

    /** Canvas'a sag tik menusunu verilen gorunumle baglar. */
    public static void install(Component canvas, Simulation simulation,
                               MarkMenuStyle style, WorldMapper mapper) {
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }

            // popup tetigi platforma gore press veya release'de gelir
            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                int[] world = mapper.toWorld(e.getX(), e.getY());
                if (world == null) return;

                Target target = simulation.targetAt(world[0], world[1]);
                if (target == null) return;   // bos alan: menu acilmaz

                build(canvas, target, style).show(canvas, e.getX(), e.getY());
            }
        });
    }

    private static JPopupMenu build(Component parent, Target target, MarkMenuStyle style) {
        JPopupMenu menu = new JPopupMenu();
        boolean marked = Mark.isMarked(target.getID());

        JMenuItem mark = new JMenuItem("Mark");
        mark.setEnabled(!marked);          // zaten isaretliyse soluk
        mark.addActionListener(e -> mark(target));

        JMenuItem unmark = new JMenuItem("Unmark");
        unmark.setEnabled(marked);         // isaretli degilse soluk
        unmark.addActionListener(e -> unmark(target));

        applyStyle(menu, style, mark, unmark);

        menu.add(mark);
        menu.addSeparator();
        menu.add(unmark);
        return menu;
    }

    /** Builder'dan gelen renkleri uygular; null renkte LAF default'u kalir. */
    private static void applyStyle(JPopupMenu menu, MarkMenuStyle style, JMenuItem... items) {
        if (style == null) return;
        if (style.getBackgroundColor() != null) {
            menu.setBackground(style.getBackgroundColor());
            for (JMenuItem item : items) {
                item.setOpaque(true);
                item.setBackground(style.getBackgroundColor());
            }
        }
        if (style.getLabelColor() != null) {
            for (JMenuItem item : items) {
                item.setForeground(style.getLabelColor());
            }
        }
    }

    /** Hedefi isaretler: mark sekli cizilmeye baslar (ID etiketi zaten vardi). */
    private static void mark(Target target) {
        // hedef henuz taranmadiysa kaydi olustur, sonra isaretle
        Mark.register(target.getCenterX(), target.getTopY(),
                      target.getID(), (float) target.getGainFactor());
        Mark.mark(target.getID());
    }

    /** Isareti kaldirir: sekil gider, ID etiketi kalir. */
    private static void unmark(Target target) {
        Mark.unmark(target.getID());
    }
}
