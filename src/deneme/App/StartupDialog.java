package deneme.App;

import javax.swing.JOptionPane;

public final class StartupDialog {

    private StartupDialog() {
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
}