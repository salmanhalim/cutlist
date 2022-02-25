package cutlist.ui;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class InfoLabel extends JPanel {
    protected JLabel m_labelField = new JLabel();
    // protected JLabel m_labelField = new JLabel() {
    //     {
    //         // setPreferredSize(new Dimension(500, 20));
    //         // setMinimumSize(new Dimension(9000, 26));
    //         /*
    //         System.out.println();
    //         System.out.println();
    //         System.out.println("-=-=-=-=-= getParent(): " + (getParent()) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
    //         System.out.println();
    //         System.out.println();
    //         */
    //     }
    //
    //     @Override
    //     public void paintComponent(Graphics g) {
    //         super.paintComponent(g);
    //
    //         System.out.println();
    //         System.out.println();
    //         System.out.println("-=-=-=-=-= INFOLABEL getSize(): " + (getSize()) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
    //         System.out.println();
    //         System.out.println();
    //     }
    //
    //     @Override
    //     public Dimension getPreferredSize() {
    //         return getMinimumSize();
    //     }
    //
    //     @Override
    //     public Dimension getMinimumSize() {
    //         return new Dimension(1100, (int) super.getMinimumSize().getHeight());
    //     }
    // };
    protected JLabel m_valueField = new JLabel();

    protected boolean m_alert;

    public InfoLabel(String label) {
        // setLayout(new FlowLayout(FlowLayout.LEFT));
        // setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new GridLayout(2, 1));

        setLabel(label);

        m_labelField.setLabelFor(m_valueField);
        m_valueField.setForeground(Color.BLUE);

        // SALMAN: Comment
        // setBorder(BorderFactory.createLineBorder(Color.BLACK));

        add(m_labelField);
        add(m_valueField);
        // add(Box.createVerticalStrut(5));

        // setPreferredSize(new Dimension(300, 52));
    }

    public String getLabel() {
        return m_labelField.getText();
    }

    public void setLabel(String val) {
        m_labelField.setText(val == null ? null : val + ":");
    }

    public String getText() {
        return m_valueField.getText();
    }

    public void setText(Object val) {
        // m_valueField.setText(val == null ? null : "<html>" + val + "</html>");
        m_valueField.setText(val == null ? "" : val.toString());
    }

    public boolean isAlert() {
        return m_alert;
    }

    public void setAlert(boolean val) {
        m_alert = val;

        m_valueField.setForeground(m_alert ? Color.RED : Color.BLUE);
    }

    /*
    @Override
    public Dimension getMinimumSize() {
        final Dimension result = super.getMinimumSize();

        return new Dimension(300, (int) result.getHeight());
    }
    */

    /*
    @Override
    public Dimension getPreferredSize() {
        final Dimension result = super.getPreferredSize();

        return new Dimension(300, (int) result.getHeight());
    }
    */
}
