package cutlist.util;

import javax.swing.JPanel;

public class UiUtils {
    public static void enablePanel(JPanel panel, boolean enabled) {
        if (panel == null) {
            return;
        }

        panel.setEnabled(enabled);

        for (var child : panel.getComponents()) {
            if (child instanceof JPanel childPanel) {
                enablePanel(childPanel, enabled);
            } else {
                child.setEnabled(enabled);
            }
        }
    }
}
