package cutlist.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cutlist.packer.Node;
import cutlist.packer.Packer.Cut;
import cutlist.packer.Packer.CutDirection;
import cutlist.packer.Packer.CutPreference;
import cutlist.util.EventBus;

/**
 * Container for a number of individual boards (BoardPanel instances).
 *
 * @author
 * @version
 */
public class BoardContainer extends JPanel {
    protected JPanel m_container = new JPanel();

    protected List<Cut>     m_cuts;
    protected CutPreference m_cutPreference;

    protected final JList<Cut>  m_allCutsList    = new JList<Cut>();
    protected final JPanel      m_allCutsPanel   = new JPanel();
    protected final JScrollPane m_containerPanel = new JScrollPane(m_container);

    protected final GridBagConstraints m_c = new GridBagConstraints();

    protected int m_counter;

    public BoardContainer() {
        // m_container.setLayout(new BoxLayout(m_container, BoxLayout.Y_AXIS));

        final GridBagLayout gridBag = new GridBagLayout();

        m_container.setLayout(gridBag);

        m_c.gridx = 0;
        m_c.fill  = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // This is only called when the user releases the mouse button.
                System.out.println("BoardContainer componentResized");
                System.out.println("-=-=-=-=-= getSize(): " + getSize() + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
            }
        });

        setupAllCutsPanel();

        super.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_allCutsPanel, m_containerPanel) {
            @Override
            public Dimension getPreferredSize() {
                int parentWidth  = (int) getParent().getSize().getWidth();
                int parentHeight = (int) getParent().getSize().getHeight();

                // More or less controls the initial size of the full window.
                return new Dimension(Math.max(parentWidth - 10, 1000), Math.max(parentHeight - 10, 900));
            }
        });
    }

    public BoardContainer(List<Cut> cuts, CutPreference cutPreference) {
        this();

        m_cuts          = cuts;
        m_cutPreference = cutPreference;

        setupUi();
    }

    // SALMAN: To be called after setting up the cut preference and cuts.
    public void setupUi() {
        optimizeCuts();

        m_allCutsPanel.setBorder(BorderFactory.createTitledBorder("All cuts (%d cut%s)".formatted(m_cuts.size(), m_cuts.size() > 1 ? "s" : "")));
        m_allCutsList.setListData(m_cuts.toArray(new Cut[m_cuts.size()]));
    }

    public List<Cut> getCuts() {
        return m_cuts;
    }

    public void setCuts(List<Cut> val) {
        m_cuts = val;
    }

    public CutPreference getCutPreference() {
        return m_cutPreference;
    }

    public void setCutPreference(CutPreference val) {
        m_cutPreference = val;
    }

    protected void setupAllCutsPanel() {
        m_allCutsPanel.setBorder(BorderFactory.createTitledBorder("All cuts"));
        m_allCutsPanel.setLayout(new BoxLayout(m_allCutsPanel, BoxLayout.Y_AXIS));

        m_allCutsList.addListSelectionListener(new ListSelectionListener() {
            protected Cut m_lastSelection = null;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (m_allCutsList.getSelectedIndex() == -1) {
                    // No selection any more; if we have a previous selection, deselect it
                    if (m_lastSelection != null) {
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_DESELECTED, m_lastSelection);

                        m_lastSelection = null;
                    }
                } else {
                    final Cut selection = m_allCutsList.getSelectedValue();

                    if (selection != m_lastSelection) {
                        // Deselect the previous selection.
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_DESELECTED, m_lastSelection);

                        EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_SELECTED, selection);

                        m_lastSelection = selection;
                    }
                }
            }
        });

        m_allCutsList.setCellRenderer(new CutCellRenderer());

        m_allCutsPanel.add(new JScrollPane(m_allCutsList));

        // List of JLabels for each cut; this works just fine, but trying the JList approach, also.
        /*
        m_cuts.forEach(cut -> {
            JLabel label = new JLabel("Board %s: %s at %.3f, from %.3f to %.3f".formatted(cut.board().name, cut.direction() == CutDirection.HORIZONTAL ? "Rip" : "Cross", cut.at(), cut.start(), cut.stop()));

            m_allCutsPanel.add(label);

            label.addMouseListener(new MouseListener() {
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
                    label.setForeground(Color.RED);

                    EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_SELECTED, cut);

                    label.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    label.setForeground(Color.BLACK);

                    EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_DESELECTED, cut);

                    label.repaint();
                }
            });
        });
        */
    }

    public static record OneCut(int result, Cut cut1, Cut cut2) {};

    protected void optimizeCuts() {
        Set<String> allCuts = new HashSet<>();

        // Move all the cuts that match the type (e.g., HORIZONTAL for RIP) and go all the way across to the top, retaining their relative order.
        /*
        System.out.println();
        System.out.println();
        System.out.println("-=-=-=-=-= Message: " + "-------------------- Cut optimization Begin --------------------" + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        System.out.println("-=-=-=-=-= paintComponent " + "[" + "m_stock.name: " + m_stock.name + "], [" + "m_stock.w: " + m_stock.w + "], [" + "m_stock.h: " + m_stock.h + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        */

        // 'Sort' by HORIZONTAL (VERTICAL for cross) and whether the boards go all the way across. If so, compare by the 'at' value. All other boards are considered
        // equal.

        // General case: prefer rips that start and stop at the same offset?
        // No, because they aren't always off the same sub-board cutoff.
        boolean       mixed           = m_cutPreference == CutPreference.MIXED;
        CutPreference preference      = mixed ? CutPreference.RIP : m_cutPreference;
        boolean       preferenceIsRip = m_cutPreference.isRip();

        // System.out.println("-=-=-=-=-= BOARDCONTAINER optimizeCuts " + "[" + "m_cutPreference: " + m_cutPreference + "], [" + "preferenceIsRip: " + preferenceIsRip + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        // SALMAN: This can be inconsistent; for example, if A > B and B > C, then A > C, but sometimes it's not. This is probably what's happening here.
        // SALMAN: Or, comparing A and B should give the opposite result of B and A (or be 0 both times).
        // SALMAN:
        // SALMAN: Put each compared pair in a record with the result; wrap this whole thing in a try/catch. When the exception fires, go over the pairs to try to
        // SALMAN: find the inconsistency.
        try {
            m_cuts = m_cuts.stream().sorted((cut1, cut2) -> {
                CutDirection d1                   = cut1.direction();
                CutDirection d2                   = cut2.direction();
                boolean      cut1PrimaryDirection = (d1 == CutDirection.HORIZONTAL && preferenceIsRip) || (d1 == CutDirection.VERTICAL && !preferenceIsRip);
                boolean      cut2PrimaryDirection = (d2 == CutDirection.HORIZONTAL && preferenceIsRip) || (d2 == CutDirection.VERTICAL && !preferenceIsRip);
                int          result               = 0;
                Node         stock1               = cut1.board();
                Node         stock2               = cut2.board();

                // We only want to look at pieces that are BOTH the same direction as as the cut preference and the same size as the full board width; everything else is
                // 'equal'.

                if (cut1PrimaryDirection && cut2PrimaryDirection) {
                    // Same direction and it's the primary direction
                    // If they have the same start and end point (such as two rip cuts down the length of the board at different points), then compare their starting
                    // points.
                    //
                    // System.out.println("-=-=-=-=-= optimizeCuts " + "[" + "cut1.start(): " + cut1.start() + "], [" + "cut2.start(): " + cut2.start() + "], [" + "cut1.stop(): " + cut1.stop() + "], [" + "cut2.stop(): " + cut2.stop() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                    //
                    if (cut1.start() == cut2.start() && cut1.stop() == cut2.stop()) {
                        result = Double.compare(cut1.at(), cut2.at());
                    }
                }

                // Prefer the cut that goes all the way across, bigger first.
                if (mixed && d1 == d2 && cut1.start() == 0 && cut2.start() == 0 && cut1.stop() == (d1 == CutDirection.HORIZONTAL ? stock1.w : stock1.h) && cut2.stop() == (d2 == CutDirection.HORIZONTAL ? stock2.w : stock2.h)) {
                    result = Double.compare(d1 == CutDirection.HORIZONTAL ? cut1.offCut().y : cut1.offCut().x, d2 == CutDirection.HORIZONTAL ? cut2.offCut().y : cut2.offCut().x);

                    if (result == 0) {
                        result = Double.compare(cut1.at(), cut2.at());
                    }
                }

                if (cut2PrimaryDirection && cut2.start() == 0 && cut2.stop() == (d2 == CutDirection.HORIZONTAL ? stock2.w : stock2.h)) {
                    result = 1;
                } else if (cut1PrimaryDirection && cut1.start() == 0 && cut1.stop() == (d1 == CutDirection.HORIZONTAL ? stock1.w : stock1.h)) {
                    result = -1;
                }

                // SALMAN: This messes up the ordering where cuts that address the same demand across different boards are cut out of order.
                // Make the one on the earlier board sort first.
                /*
                if (result == 0) {
                    result = stock1.name.compareTo(stock2.name);
                }
                */

                // // SALMAN: remove as it's not working.
                /*
                if (result == 0) {
                    System.out.println("-=-=-=-=-= optimizeCuts " + "[" + "cut1.demandPiece().key.label(): " + cut1.demandPiece().key.label() + "], [" + "cut2.demandPiece().key.label(): " + cut2.demandPiece().key.label() + "], [" + "cut1.at(): " + cut1.at() + "], [" + "cut2.at(): " + cut2.at() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                    // SALMAN: Only if same direction or not at all as it's not working as is?
                    if (cut1.demandPiece().key == cut2.demandPiece().key && d1 == d2) {
                        result = Double.compare(cut1.start(), cut2.start());
                    }
                }
                */

                allCuts.add(new OneCut(result, cut1, cut2).toString());

                return result;
            }).toList();
        } catch (Exception e) {
            e.printStackTrace();

            final Path path = FileSystems.getDefault().getPath("c:/tmp/CutListComparisons");

            System.out.println("-=-=-=-=-= optimizeCuts path: " + (path) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

            try {
                Files.write(path, allCuts);
            } catch (Exception e2) {
                e2.printStackTrace();

                System.out.println("-=-=-=-=-= UNABLE TO WRITE CUT DETAILS; PRINTING THEM HERE optimizeCuts path: " + (path) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                System.out.println("-=-=-=-=-= optimizeCuts allCuts: " + (allCuts) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
            }
        }

        /*
        System.out.println("-=-=-=-=-= Message: " + "-------------------- Cut optimization End --------------------" + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        System.out.println();
        System.out.println();
        */
    }

    @Override
    public Component add(Component component) {
        final int numComponents = getComponentCount();

        if (m_counter > 0) {
            addComponent(Box.createVerticalStrut(10));
        }

        return addComponent(component);
    }

    protected Component addComponent(Component component) {
        m_c.gridy = m_counter++;

        m_container.add(component, m_c);

        return component;
    }

    public void clearOldSolution() {
        m_container.removeAll();

        m_counter = 0;
    }
}
