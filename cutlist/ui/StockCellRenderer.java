package cutlist.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import cutlist.packer.Node;

public class StockCellRenderer extends JLabel implements ListCellRenderer<Node> {
    @Override
    public Component getListCellRendererComponent(JList<? extends Node> list, Node stock, int index, boolean isSelected, boolean cellHasFocus){
        setText("%s: (%.3f x %.3f), $%.2f".formatted(stock.name, stock.h, stock.w, stock.cost));

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
}
