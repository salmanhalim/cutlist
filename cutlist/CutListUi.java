package cutlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cutlist.CutList.Demand;
import cutlist.packer.Node;
import cutlist.packer.Packer;
import cutlist.ui.BoardContainer;
import cutlist.ui.BoardPanel;
import cutlist.ui.DemandCellRenderer;
import cutlist.ui.StockCellRenderer;
import cutlist.util.EventBus;

public class CutListUi implements ActionListener {
    public static final int MAX_DISPLAY_BOARD_WIDTH = 1000;

    public static double s_scaleFactor = 1;

    protected static final Random RAND = new Random();

    public static final Map<Demand, Color> DEMAND_COLORS = new HashMap<>();

    public static int scaleValue(double v) {
        return (int) Math.round(v * s_scaleFactor);
    }

    public static Color getDemandColor(Node demand) {
        Color result = DEMAND_COLORS.get(demand.key);

        if (result == null) {
            result = new Color(RAND.nextInt(256), RAND.nextInt(256), RAND.nextInt(256));

            DEMAND_COLORS.put(demand.key, result);
        }

        return result;
    }

    protected CutList m_cutList;
    protected Packer  m_packer;
    protected JFrame  m_frame;

    protected final BoardContainer m_boardContainer  = new BoardContainer();

    protected final JPanel        m_stockPanel     = new JPanel();
    protected final JList<Node>   m_usedStockList  = new JList<>();
    protected final JPanel        m_demandPanel    = new JPanel();
    protected final JList<Demand> m_usedDemandList = new JList<>();

    protected final JPanel m_leftPanel = new JPanel() {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, (int) super.getPreferredSize().getHeight());
        }
    };

    protected final JScrollPane m_leftComponentsContainer = new JScrollPane(m_leftPanel) {
        {
            setBorder(BorderFactory.createTitledBorder("Solution details"));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(333, 300);
        }
    };

    protected List<Node> m_usedBoards;

    protected int m_maxDisplayBoardWidth = MAX_DISPLAY_BOARD_WIDTH;

    public CutListUi(CutList cutList, JFrame frame) {
        m_cutList = cutList;
        m_frame   = frame;
    }

    public Packer getPacker() {
        return m_packer;
    }

    public void setPacker(Packer val) {
        m_packer = val;
    }

    public int getMaxDisplayBoardWidth() {
        return m_maxDisplayBoardWidth;
    }

    public void setMaxDisplayBoardWidth(int val) {
        m_maxDisplayBoardWidth = val;
    }

    public Component createCenterComponents() {
        return m_boardContainer;
    }

    public void setupSolution() {
        // SALMAN: Clear old solution
        DEMAND_COLORS.clear();
        m_boardContainer.clearOldSolution();

        m_usedBoards = m_packer.getStock().stream().filter(board -> board.used).toList();

        final double widestBoardWidth = m_usedBoards.stream().max(Comparator.comparing(b1 -> b1.w)).get().w;

        s_scaleFactor = m_maxDisplayBoardWidth / widestBoardWidth;

        // System.out.println("-=-=-=-=-= setupSolution s_scaleFactor: " + (s_scaleFactor) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        m_boardContainer.setCutPreference(m_packer.getCutPreference());
        m_boardContainer.setCuts(m_packer.getCuts());
        m_boardContainer.setupUi();

        m_usedBoards.forEach(board -> {
            final BoardPanel boardPanel = new BoardPanel(m_packer, board, m_packer.getCuts().stream().filter(cut -> cut.board() == board).toList(),
                    m_packer.getDemand().stream().filter(demand -> demand.fit != null && demand.getRoot() == board).toList(), this, m_packer.getCutPreference());

            m_boardContainer.add(boardPanel);
        });

        m_stockPanel.setBorder(BorderFactory.createTitledBorder("Used stock (%d board%s)".formatted(m_usedBoards.size(), m_usedBoards.size() > 1 ? "s" : "")));
        m_usedStockList.setListData(m_usedBoards.toArray(new Node[m_usedBoards.size()]));

        List<Demand> allDemands = m_cutList.getDemand();

        // SALMAN: Need to remove pieces that weren't fitted from this list
        List<Demand> fittedDemand = allDemands;

        m_demandPanel.setBorder(BorderFactory.createTitledBorder("Fitted Demand (%d board%s of %d)".formatted(fittedDemand.size(), fittedDemand.size() > 1 ? "s" : "", allDemands.size())));
        m_usedDemandList.setListData(fittedDemand.toArray(new Demand[fittedDemand.size()]));

        m_boardContainer.validate();
        m_leftComponentsContainer.validate();
    }

    public Component createLeftComponents() {
        final GridBagLayout      gridBag = new GridBagLayout();
        final GridBagConstraints c       = new GridBagConstraints();
        int                      counter = 0;

        m_leftPanel.setLayout(gridBag);

        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets  = new Insets(0, 0, 10, 0);

        c.gridy = counter++;
        m_leftPanel.add(createStockPanel(), c);

        c.gridy = counter++;
        m_leftPanel.add(createDemandPanel(), c);

        /*
        c.gridy = counter++;
        m_leftPanel.add(new JPanel() {{
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            IntStream.range(1, 100).forEach(i -> add(new JButton("Button " + i)));
        }}, c);
        */

        // Force the panels above to go to the top of the view.
        /*
        c.gridy   = counter++;
        c.weighty = 1;
        m_leftPanel.add(Box.createVerticalGlue(), c);
        */

        return m_leftComponentsContainer;
    }

    protected Component createStockPanel() {
        m_stockPanel.setLayout(new GridLayout(1, 1));
        m_stockPanel.setBorder(BorderFactory.createTitledBorder("Used stock"));

        m_usedStockList.addListSelectionListener(new ListSelectionListener() {
            Node m_lastSelection = null;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (m_usedStockList.getSelectedIndex() == -1) {
                    // No selection any more; if we have a previous selection, deselect it
                    if (m_lastSelection != null) {
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.STOCK_DESELECTED, m_lastSelection);

                        m_lastSelection = null;
                    }
                } else {
                    final Node selection = m_usedStockList.getSelectedValue();

                    if (selection != m_lastSelection) {
                        // Deselect the previous selection.
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.STOCK_DESELECTED, m_lastSelection);

                        EventBus.INSTANCE.triggerEvent(EventBus.Event.STOCK_SELECTED, selection);

                        m_lastSelection = selection;
                    }
                }
            }
        });

        m_usedStockList.setCellRenderer(new StockCellRenderer());

        m_stockPanel.add(m_usedStockList);

        return m_stockPanel;
    }

    protected Component createDemandPanel() {
        m_demandPanel.setLayout(new GridLayout(1, 1));

        m_demandPanel.setBorder(BorderFactory.createTitledBorder("Fitted demand"));

        m_usedDemandList.addListSelectionListener(new ListSelectionListener() {
            Demand m_lastSelection = null;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (m_usedDemandList.getSelectedIndex() == -1) {
                    // No selection any more; if we have a previous selection, deselect it
                    if (m_lastSelection != null) {
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.DEMAND_DESELECTED, m_lastSelection);

                        m_lastSelection = null;
                    }
                } else {
                    final Demand selection = m_usedDemandList.getSelectedValue();

                    if (selection != m_lastSelection) {
                        // Deselect the previous selection.
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.DEMAND_DESELECTED, m_lastSelection);

                        EventBus.INSTANCE.triggerEvent(EventBus.Event.DEMAND_SELECTED, selection);

                        m_lastSelection = selection;
                    }
                }
            }
        });

        m_usedDemandList.setCellRenderer(new DemandCellRenderer());

        m_demandPanel.add(m_usedDemandList);

        return m_demandPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("-=-=-=-=-= e: " + e + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
    }
}
