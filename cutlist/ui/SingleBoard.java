package cutlist.ui;

import javax.swing.BorderFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.Border;

import cutlist.CutListUi;
import cutlist.packer.Node;
import cutlist.packer.Packer.Cut;
import cutlist.packer.Packer.CutDirection;
import cutlist.util.ComponentScroller;
import cutlist.util.EventBus;

public class SingleBoard extends JPanel implements MouseListener, EventBus.Listener {
    protected BoardPanel m_boardPanel;
    protected Node       m_stock;

    protected Map<Cut, Integer> m_highlightedCuts = new HashMap<>();

    protected Map<Node, CutPiece> m_demandPanels = new HashMap<>();

    protected int m_highlightCount;

    protected Border m_baseBorder     = BorderFactory.createLineBorder(Color.BLACK, 1, false);
    protected Border m_selectedBorder = BorderFactory.createLineBorder(Color.RED,   3, true);

    // Cut higlights (m_normalCut is a dashed line)
    protected final Stroke m_normalCut      = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {3}, 0);
    protected final Stroke m_highlightedCut = new BasicStroke(5);

    public SingleBoard(BoardPanel boardPanel) {
        m_boardPanel = boardPanel;
        m_stock      = boardPanel.getStock();

        setLayout(null);
        addMouseListener(this);

        m_boardPanel.getCutPieces().stream().forEach(demand -> {
            final CutPiece demandPanel = new CutPiece(demand, this);

            add(demandPanel);

            m_demandPanels.put(demand, demandPanel);
        });

        setBackground(new Color(230, 230, 230));

        EventBus.INSTANCE.addListener(List.of(EventBus.Event.STOCK_SELECTED, EventBus.Event.STOCK_DESELECTED), this, m_stock);
        EventBus.INSTANCE.addListener(List.of(EventBus.Event.CUT_SELECTED,   EventBus.Event.CUT_DESELECTED),   this);

        setBorder(m_baseBorder);
    }

    public Node getStock() {
        return m_stock;
    }

    // SALMAN: remove
    /*
    @Override
    protected void paintBorder(Graphics g) {
        super.paintBorder(g);

        final Graphics2D g2 = (Graphics2D) g;

        g.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(m_highlightCount > 0 ? 7 : 3));

        g.drawRect(0, 0, CutListUi.scaleValue(m_stock.w) - 1, CutListUi.scaleValue(m_stock.h) - 1);
    }
    */

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        final Graphics2D g2 = (Graphics2D) g;

        g2.setStroke(new BasicStroke(1));

        double halfExtraPadding = m_boardPanel.getSolution().getExtraPadding() / 2;

        m_demandPanels.entrySet().stream().forEach(entry -> {
            final Node     demand   = entry.getKey();
            // final CutPiece cutPiece = entry.getValue();
            double       width  = demand.rotated ? demand.h : demand.w;
            double       height = demand.rotated ? demand.w : demand.h;

            entry.getValue().setBounds(CutListUi.scaleValue(demand.fit.x + halfExtraPadding) + 4, CutListUi.scaleValue(demand.fit.y + halfExtraPadding) + 4,
                    CutListUi.scaleValue(width) - 8, CutListUi.scaleValue(height) - 8);
        });

        double kerf           = m_boardPanel.getSolution().getKerf();
        int    scaledKerf     = CutListUi.scaleValue(kerf);
        int    scaledHalfKerf = CutListUi.scaleValue(kerf / 2);

        m_boardPanel.getCuts().forEach(cut -> {
            int cutStart = CutListUi.scaleValue(cut.start());
            int cutAt    = CutListUi.scaleValue(cut.at());
            int cutStop  = CutListUi.scaleValue(cut.stop());

            boolean highlight = Optional.ofNullable(m_highlightedCuts.get(cut)).orElse(0) > 0;

            if (highlight) {
                g2.setStroke(m_highlightedCut);
            } else {
                g2.setStroke(m_normalCut);
            }

            boolean drawRectangle = highlight && scaledKerf > 3;

            if (cut.direction() == CutDirection.HORIZONTAL) {
                g.setColor(Color.RED);

                if (drawRectangle) {
                    g.fillRect(cutStart - scaledHalfKerf, cutAt, cutStop - cutStart + scaledKerf, scaledKerf);
                } else {
                    g.drawLine(cutStart - scaledHalfKerf, cutAt + scaledHalfKerf, cutStop + scaledHalfKerf, cutAt + scaledHalfKerf);
                }
            } else {
                g.setColor(Color.BLUE);

                if (drawRectangle) {
                    g.fillRect(cutAt, cutStart - scaledHalfKerf, scaledKerf, cutStop - cutStart + scaledKerf);
                } else {
                    g.drawLine(cutAt + scaledHalfKerf, cutStart - scaledHalfKerf, cutAt + scaledHalfKerf, cutStop + scaledHalfKerf);
                }
            }
        });
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CutListUi.scaleValue(m_stock.w), CutListUi.scaleValue(m_stock.h));
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

    protected void adjustHighlight(boolean add) {
        m_highlightCount += add ? 1 : -1;

        if (m_highlightCount < 0) {
            m_highlightCount = 0;
        }

        EventBus.INSTANCE.triggerEvent(EventBus.Event.TEMP, Integer.valueOf(m_highlightCount));

        setBorder(m_highlightCount > 0 ? m_selectedBorder : m_baseBorder);
    }

    protected void adjustHighlightedCuts(Cut cut, boolean add) {
        // if (!add) { return; } // SALMAN: remove

        if (add) {
            EventBus.INSTANCE.triggerEvent(EventBus.Event.DEMAND_CUT, cut.demandPiece());
        }

        Integer numHighlights = m_highlightedCuts.get(cut);

        if (numHighlights == null) {
            numHighlights = Integer.valueOf(0);
        }

        numHighlights += add ? 1 : -1;

        if (numHighlights < 0) {
            numHighlights = 0;
        }

        m_highlightedCuts.put(cut, numHighlights);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        adjustHighlight(true);

        EventBus.INSTANCE.triggerEvent(EventBus.Event.MOUSE_ENTERED_BOARD, this);

        if (e.getSource() instanceof final CutPiece cutPiece) {
            cutPiece.setMouseEntered(true);

            EventBus.INSTANCE.triggerEvent(EventBus.Event.MOUSE_ENTERED_CUT_PIECE, e.getSource());
        }

        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        adjustHighlight(false);

        if (e.getSource() instanceof final CutPiece cutPiece) {
            cutPiece.setMouseEntered(false);

            EventBus.INSTANCE.triggerEvent(EventBus.Event.MOUSE_EXITED_CUT_PIECE, e.getSource());
        } else {
            EventBus.INSTANCE.triggerEvent(EventBus.Event.MOUSE_EXITED_BOARD, e.getSource());
        }

        repaint();
    }

    /**
     * Scroll to put the board in view if it's not visible.
     */
    protected void scrollIntoView() {
        Component parent = getParent();
        Rectangle bounds = m_boardPanel.getBounds();

        while (!(parent instanceof JScrollPane)) {
            parent = parent.getParent();

            if (parent == null) {
                break;
            }
        }

        if (parent instanceof JScrollPane scrollPane) {
            JViewport  viewPort   = scrollPane.getViewport();
            JScrollBar vertBar    = scrollPane.getVerticalScrollBar();
            int        resultingY = -1;

            int viewPortHeight = (int) viewPort.getSize().getHeight();
            int pieceStartY    = (int) bounds.getY();
            int pieceEndY      = (int) (pieceStartY + bounds.getHeight());

            int viewStartY = (int) viewPort.getViewPosition().getY(); // Top of view
            int viewEndY   = viewStartY + viewPortHeight;

            // System.out.println("-=-=-=-=-= scrollIntoView " + "[" + "viewPortHeight: " + viewPortHeight + "], [" + "viewStartY: " + viewStartY + "], [" + "viewEndY: " + viewEndY + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
            // System.out.println("-=-=-=-=-= scrollIntoView " + "[" + "pieceStartY: " + pieceStartY + "], [" + "pieceEndY: " + pieceEndY + "]" + " (" + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove

            if (pieceEndY - pieceStartY <= viewPortHeight) { // Only scroll if the board isn't too big for the current viewport.
                if (pieceStartY < vertBar.getValue()) {
                    // Scroll up
                    resultingY = pieceStartY;
                } else if (viewEndY < pieceEndY) {
                    // Scroll down just enough so the bottom of the piece is visible (set the scrollbar to the top of the view where the bottom of the board
                    // is at the bottom of the view).
                    resultingY = pieceEndY - viewPortHeight;
                }
            } else if (pieceStartY >= viewEndY || pieceEndY <= viewStartY) { // If the baord isn't visible at all, scroll to the top of the baord
                resultingY = pieceStartY;
            }

            if (resultingY >= 0) {
                // vertBar.setValue(resultingY);
                ComponentScroller.INSTANCE.scrollVertically(scrollPane, resultingY);
            }
        }
    }

    @Override
    public void eventTriggered(EventBus.Event event, Object source) {
        switch (event) {
            case STOCK_SELECTED:
                scrollIntoView();

                adjustHighlight(true);

                break;

            case STOCK_DESELECTED:
                adjustHighlight(false);

                break;

            case CUT_SELECTED:
                if (m_boardPanel.getCuts().contains(source)) {
                    adjustHighlightedCuts((Cut) source, true);

                    scrollIntoView();

                    repaint();
                }

                break;

            case CUT_DESELECTED:
                if (m_boardPanel.getCuts().contains(source)) {
                    adjustHighlightedCuts((Cut) source, false);

                    repaint();
                }

                break;

            default:
                // Do nothing; we don't handle every event.
        }

        repaint();
    }
}
