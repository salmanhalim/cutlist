package cutlist.ui;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import cutlist.CutList.Demand;

public class DemandCellRenderer extends JLabel implements ListCellRenderer<Demand>, MouseListener {
    public DemandCellRenderer() {
        // System.out.println("-=-=-=-=-= Message: " + "Here" + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove

        addMouseListener(this);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Demand> list, Demand demand, int index, boolean isSelected, boolean cellHasFocus){
        setText("%s x %d: %.3f x %.3f".formatted(demand.label(), demand.quantity(), demand.height(), demand.width()));

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);

        return this;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        System.out.println("-=-=-=-=-= mouseEntered e: " + e + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
    }

    @Override
    public void mouseExited(MouseEvent e) {
        System.out.println("-=-=-=-=-= mouseExited e: " + e + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
    }
}
