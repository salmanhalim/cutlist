package cutlist.ui;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import cutlist.packer.Packer.Cut;
import cutlist.packer.Packer.CutDirection;

public class CutCellRenderer extends JLabel implements ListCellRenderer<Cut>, MouseListener {
    protected boolean m_simpleLabel;

    public CutCellRenderer() {
        super();

        addMouseListener(this);
    }

    public CutCellRenderer(boolean simpleLabel) {
        this();

        m_simpleLabel = simpleLabel;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Cut> list, Cut cut, int index, boolean isSelected, boolean cellHasFocus){
        if (m_simpleLabel) {
            setText("%s @ %.3f (%.3f wide), %.3f-%.3f".formatted(cut.direction() == CutDirection.HORIZONTAL ? "R" : "C",
                            cut.at(),
                            cut.at() - (cut.direction() == CutDirection.HORIZONTAL ? cut.offCut().y : cut.offCut().x),
                            cut.start(),
                            cut.stop()));
        } else {
            setText("%s: %s @ %.3f (%.3f wide), %.3f-%.3f".formatted(cut.board().name,
                            cut.direction() == CutDirection.HORIZONTAL ? "R" : "C",
                            cut.at(),
                            cut.at() - (cut.direction() == CutDirection.HORIZONTAL ? cut.offCut().y : cut.offCut().x),
                            cut.start(),
                            cut.stop()));
        }

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
        System.out.println("-=-=-=-=-= mouseEntered e: " + (e) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
    }

    @Override
    public void mouseExited(MouseEvent e) {
        System.out.println("-=-=-=-=-= mouseExited e: " + (e) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
    }
}
