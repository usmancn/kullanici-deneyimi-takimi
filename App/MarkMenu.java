package deneme.App;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import deneme.GLCore.Mark;
import deneme.Simulation.Simulation;
import deneme.Simulation.Target;
import deneme.Simulation.TargetType;

/**
 * Hedef uzerinde sag tik menusu: Mark / Change Mark / Unmark.
 *
 * <p><b>Mark</b> ve <b>Change Mark</b> ayni alt menuyu acar (kayitli turler +
 * "Yeni tur..."); ikisi birbirini disler: hedefin ID'si yoksa sadece Mark,
 * varsa sadece Change Mark tiklanabilir, digeri soluk gorunur. <b>Unmark</b>
 * ID'yi ve isareti siler.
 *
 * <p>Bir ture tiklandiginda hedefin turu degistirilir, {@code hasID} true
 * yapilir ve yeni ID atanir ({@code TUR + sayac}). "Yeni tur..." once isim
 * sorar, turu {@link TargetType#define} ile kayit defterine ekler, sonra ayni
 * atamayi yapar.
 *
 * <p><b>Heavyweight popup:</b> GLCanvas agir (native) bir AWT bilesenidir;
 * Swing'in varsayilan hafif popup'lari onun altinda kalir ve gorunmez. Bu
 * yuzden popup'lar heavyweight'e zorlanir.
 */
public final class MarkMenu {

    static {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    }

    private MarkMenu() {}

    /**
     * Fare olayini dunya koordinatina cevirir (kare grafikte dogrudan, dairesel
     * grafikte polar tersine cevrim). Cizim alani disindaysa null doner.
     */
    public interface WorldMapper {
        int[] toWorld(int eventX, int eventY);
    }

    /** Canvas'a sag tik menusunu baglar. */
    public static void install(Component canvas, Simulation simulation, WorldMapper mapper) {
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

                build(canvas, target).show(canvas, e.getX(), e.getY());
            }
        });
    }

    private static JPopupMenu build(Component parent, Target target) {
        JPopupMenu menu = new JPopupMenu();
        boolean marked = target.isHasID();

        JMenu mark = new JMenu("Mark");
        fillTypes(mark, parent, target);
        mark.setEnabled(!marked);          // zaten isaretliyse soluk

        JMenu changeMark = new JMenu("Change Mark");
        fillTypes(changeMark, parent, target);
        changeMark.setEnabled(marked);     // isaretli degilse soluk

        JMenuItem unmark = new JMenuItem("Unmark");
        unmark.setEnabled(marked);
        unmark.addActionListener(e -> unmark(target));

        menu.add(mark);
        menu.add(changeMark);
        menu.addSeparator();
        menu.add(unmark);
        return menu;
    }

    /** Alt menu: kayitli turler + "Yeni tur...". */
    private static void fillTypes(JMenu parentMenu, Component parent, Target target) {
        for (TargetType type : TargetType.values()) {
            JMenuItem item = new JMenuItem(type.getTypeName());
            item.addActionListener(e -> applyType(target, type));
            parentMenu.add(item);
        }

        parentMenu.addSeparator();

        JMenuItem newType = new JMenuItem("Yeni tur...");
        newType.addActionListener(e -> createTypeAndApply(parent, target));
        parentMenu.add(newType);
    }

    /** Tur atar, hasID = true yapar ve yeni ID uretir; eski isaret varsa kaldirilir. */
    private static void applyType(Target target, TargetType type) {
        String oldId = target.getID();

        target.setType(type);
        target.setHasID(true);
        target.setID();                     // ID = TUR + sayac

        if (oldId != null && !oldId.equals(target.getID())) {
            Mark.remove(oldId);             // change mark: eski ID'nin isareti kalmasin
        }
        // isaret hemen gorunsun (dedektor bir sonraki taramada ayni ID ile tazeler)
        Mark.register(target.getCenterX(), target.getTopY(),
                      target.getID(), (float) target.getGainFactor());
    }

    /** Isim sorar, turu kayit defterine ekler ve hedefe uygular. */
    private static void createTypeAndApply(Component parent, Target target) {
        String input = JOptionPane.showInputDialog(parent, "Yeni tur adi:",
                "Yeni Tur", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;          // iptal

        String name = input.trim().toUpperCase();
        if (name.isEmpty()) return;

        // yeni turun olculeri hedefin mevcut turunden alinir: veri zaten cizilmis
        // durumda oldugundan tespit/secim geometrisi degismemeli
        TargetType type = TargetType.define(name,
                target.getType().getWidth(), target.getType().getHeight());
        applyType(target, type);
    }

    /** hasID = false, ID = null ve ekrandaki isaret silinir. */
    private static void unmark(Target target) {
        Mark.remove(target.getID());
        target.clearID();
    }
}
