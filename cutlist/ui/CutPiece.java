package cutlist.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

import cutlist.CutListUi;
import cutlist.packer.Node;
import cutlist.util.EventBus;

public class CutPiece extends JPanel implements EventBus.Listener {
    protected Node    m_demand;
    protected boolean m_mouseEntered = false;

    protected boolean m_selected;

    protected Border m_hoverBorder;
    protected Border m_selectedBorder;

    protected SingleBoard m_container;

    protected int m_numCuts;

    public CutPiece(Node demand, SingleBoard container) {
        m_demand = demand;

        addMouseListener(container);

        m_container = container;

        setBackground(CutListUi.getDemandColor(m_demand));

        m_hoverBorder = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createRaisedBevelBorder());
        m_hoverBorder = BorderFactory.createCompoundBorder(m_hoverBorder,                            BorderFactory.createLineBorder(Color.WHITE, 1));

        m_selectedBorder = BorderFactory.createLineBorder(Color.RED, 3, false);

        EventBus.INSTANCE.addListener(List.of(EventBus.Event.DEMAND_SELECTED, EventBus.Event.DEMAND_DESELECTED), this, m_demand.key);
        // SALMAN: Uncomment to highlight demands as they're cut
        // EventBus.INSTANCE.addListener(List.of(EventBus.Event.DEMAND_CUT, EventBus.Event.DEMAND_UNCUT), this, m_demand);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (m_mouseEntered) {
            setBorder(m_hoverBorder);
        } else if (m_selected || m_numCuts > 0) {
            setBorder(m_selectedBorder);
        } else {
            setBorder(null);
        }

        setBackground(m_numCuts > 1 ? Color.DARK_GRAY : m_numCuts > 0 ? Color.GRAY : m_selected ? Color.PINK : CutListUi.getDemandColor(m_demand));
    }

    public void setMouseEntered(boolean entered) {
        m_mouseEntered = entered;

        repaint();
    }

    public Node getDemand() {
        return m_demand;
    }

    @Override
    public void eventTriggered(EventBus.Event event, Object source) {
        switch (event) {
            case DEMAND_SELECTED, DEMAND_DESELECTED:
                m_selected = event == EventBus.Event.DEMAND_SELECTED;

                if (m_selected) {
                    // SALMAN: This will potentially create a situation where the bottom board is in view instead of the top board if the demand is split across multiple
                    // SALMAN: stock pieces. Might need to come up with a way to figure out which board should be in view (the top one or the one that requires less
                    // SALMAN: scrolling) if this is bothersome.
                    m_container.scrollIntoView();
                }

                break;

            case DEMAND_CUT:
                m_numCuts++;

            default:
                // // SALMAN: Finish this
        }


        repaint();
    }
}
