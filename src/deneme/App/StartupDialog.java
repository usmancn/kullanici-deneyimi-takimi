package deneme.App;

import javax.swing.JOptionPane;

public final class StartupDialog {

    public enum DataMode {
        SIMULATION,
        IMAGE_DEMO,
        CANCEL
    }

    private StartupDialog() {
    }

    public static DataMode askDataMode() {
        Object[] options = {
                "Simulasyon",
                "Fotograf Demo"
        };

        int choice = JOptionPane.showOptionDialog(
                null,
                "Veri kaynagi sec:",
                "Baslangic",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            return DataMode.SIMULATION;
        }
        if (choice == 1) {
            return DataMode.IMAGE_DEMO;
        }
        return DataMode.CANCEL;
    }

    public static int askTargetCount(int defaultTargetCount) {
        String input = JOptionPane.showInputDialog(
                null,
                "Hedef sayisi:",
                "Baslangic",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) {
            return -1;
        }

        try {
            return Math.max(0, Integer.parseInt(input.trim()));
        } catch (NumberFormatException ex) {
            return defaultTargetCount;
        }
    }

    public static void showError(String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Hata",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
