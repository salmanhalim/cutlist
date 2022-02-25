package cutlist.ui;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ComponentWithLabel extends JPanel {
    protected JLabel    m_labelField = new JLabel();
    protected Component m_component;

    public ComponentWithLabel(String label, Component component) {
        m_component = component;

        // setLayout(new FlowLayout(FlowLayout.LEFT));
        // setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new GridLayout(2, 1));

        setLabel(label);

        m_labelField.setLabelFor(component);

        add(m_labelField);
        add(component);
    }

    public Component getComponent() {
        return m_component;
    }

    public String getLabel() {
        return m_labelField.getText();
    }

    public void setLabel(String val) {
        m_labelField.setText(val == null ? null : val + ":");
    }
}
