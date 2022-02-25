package cutlist.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

import cutlist.CutListUi;
import cutlist.packer.Node;
import cutlist.packer.Packer.Cut;
import cutlist.packer.Packer.CutDirection;
import cutlist.packer.Packer.CutPreference;
import cutlist.packer.Packer;
import cutlist.util.EventBus;

/**
 * Container for a single board, cut pieces and list of cuts.
 */
public class BoardPanel extends JPanel {
    protected Node          m_stock;
    protected List<Cut>     m_cuts;
    protected List<Node>    m_cutPieces;
    protected CutListUi     m_container;
    protected CutPreference m_cutPreference;
    protected Packer        m_solution;

    public BoardPanel() {
        /*
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // This is only called when the user releases the mouse button.
                System.out.println("BoardPanel componentResized");
                System.out.println("-=-=-=-=-= getSize(): " + getSize() + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
            }
        });
        */
    }

    public BoardPanel(Packer solution, Node stock, List<Cut> cuts, List<Node> cutPieces, CutListUi container, CutPreference cutPreference) {
        this();

        m_solution      = solution;
        m_stock         = stock;
        m_cutPieces     = cutPieces;
        m_container     = container;
        m_cutPreference = cutPreference;

        m_cuts = consolidateCuts(cuts);
        optimizeCuts();

        setupUi();
    }

    public Node getStock() {
        return m_stock;
    }

    public List<Cut> getCuts() {
        return m_cuts;
    }

    public List<Node> getCutPieces() {
        return m_cutPieces;
    }

    public Packer getSolution() {
        return m_solution;
    }

    public void setSolution(Packer val) {
        m_solution = val;
    }

    // SALMAN: Copy the algorithm from BoardContainer once that's perfected or come up with a way to run it on two different collections
    protected void optimizeCuts() {
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
        // SALMAN: Copied from BoardContainer
        boolean       mixed           = m_cutPreference == CutPreference.MIXED;
        CutPreference preference      = mixed ? CutPreference.RIP : m_cutPreference;
        boolean       preferenceIsRip = m_cutPreference.isRip();

        // System.out.println("-=-=-=-=-= optimizeCuts " + "[" + "m_cutPreference: " + m_cutPreference + "], [" + "preferenceIsRip: " + preferenceIsRip + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

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

                // System.out.println("-=-=-=-=-= optimizeCuts " + "[" + "cut1.start(): " + cut1.start() + "], [" + "cut2.start(): " + cut2.start() + "], [" + "cut1.stop(): " + cut1.stop() + "], [" + "cut2.stop(): " + cut2.stop() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                //
                if (cut1.start() == cut2.start() && cut1.stop() == cut2.stop()) {
                    result = Double.compare(cut1.at(), cut2.at());

                    // System.out.println("-=-=-=-=-= optimizeCuts " + "[" + "cut1.at(): " + cut1.at() + "], [" + "cut2.at(): " + cut2.at() + "], [" + "result: " + result + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
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
                // System.out.println("-=-=-=-=-= Message: " + "optimizeCuts Here 1 (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
            } else if (cut1PrimaryDirection && cut1.start() == 0 && cut1.stop() == (d1 == CutDirection.HORIZONTAL ? stock1.w : stock1.h)) {
                // System.out.println("-=-=-=-=-= Message: " + "optimizeCuts Here 2 (" + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
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

            // System.out.println("-=-=-=-=-= optimizeCuts result: " + (result) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

            return result;
        }).toList();

        // SALMAN: Old
        // m_cuts = m_cuts.stream().sorted((cut1, cut2) -> {
        //     CutDirection d1                   = cut1.direction();
        //     CutDirection d2                   = cut2.direction();
        //     boolean      cut1PrimaryDirection = (d1 == CutDirection.HORIZONTAL && m_cutPreference == CutPreference.RIP) || (d1 == CutDirection.VERTICAL && m_cutPreference == CutPreference.CROSS);
        //     boolean      cut2PrimaryDirection = (d2 == CutDirection.HORIZONTAL && m_cutPreference == CutPreference.RIP) || (d2 == CutDirection.VERTICAL && m_cutPreference == CutPreference.CROSS);
        //     int          result               = 0;
        //
        //     // We only want to look at pieces that are BOTH the same direction as as the cut preference and the same size as the full board width; everything else is
        //     // 'equal'.
        //
        //     if (cut1PrimaryDirection && cut2PrimaryDirection) {
        //         // Same direction and it's the primary direction
        //         // If they have the same start and end point (such as two rip cuts down the length of the board at different points), then compare their starting
        //         // points.
        //         //
        //         // System.out.println("-=-=-=-=-= optimizeCuts " + "[" + "cut1.start(): " + cut1.start() + "], [" + "cut2.start(): " + cut2.start() + "], [" + "cut1.stop(): " + cut1.stop() + "], [" + "cut2.stop(): " + cut2.stop() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        //         //
        //         if (cut1.start() == cut2.start() && cut1.stop() == cut2.stop()) {
        //             result = Double.compare(cut1.at(), cut2.at());
        //         }
        //     }
        //
        //     if (cut1PrimaryDirection && cut1.start() == 0 && cut1.stop() == (d1 == CutDirection.HORIZONTAL ? m_stock.w : m_stock.h)) {
        //         // Prefer the cut that goes all the way across.
        //         result = -1;
        //     }
        //
        //     if (cut2PrimaryDirection && cut2.start() == 0 && cut2.stop() == (d2 == CutDirection.HORIZONTAL ? m_stock.w : m_stock.h)) {
        //         // Prefer the cut that goes all the way across.
        //         result = 1;
        //     }
        //
        //     return result;
        // }).toList();

        /*
        System.out.println("-=-=-=-=-= Message: " + "-------------------- Cut optimization End --------------------" + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        System.out.println();
        System.out.println();
        */
    }

    protected void setupUi() {
        Component cutsList    = getCutsList();
        Component singleBoard = new SingleBoard(this);

        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cutsList, new JPanel(){
            {
                add(singleBoard);
            }

            /*
            @Override
            public Dimension getPreferredSize() {
                int parentWidth  = (int) getParent().getParent().getSize().getWidth();
                int parentHeight = (int) getParent().getParent().getSize().getHeight();

                return new Dimension(Math.max(parentWidth - 10, 1000), Math.max(parentHeight - 10, 800));
            }
            */
        }));

        /*
        final GridBagLayout      gridBag = new GridBagLayout();
        final GridBagConstraints c       = new GridBagConstraints();
        int                      counter = 0;

        setLayout(gridBag);
        // setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        setBorder(BorderFactory.createTitledBorder(m_stock.name));

        // m_cutPieces.forEach(cutPiece -> {
        //     add(new JButton(cutPiece.name));
        // });

        c.gridx  = counter++;
        c.gridy  = 0;
        c.fill   = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        add(cutsList, c);


        c.gridx = counter++;

        add(Box.createHorizontalStrut(10), c);


        c.gridx = counter++;

        add(singleBoard, c);

        c.gridx   = counter++;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        // add(Box.createHorizontalStrut(10), c);
        add(Box.createHorizontalGlue(), c);
        */
    }

    protected Component getCutsList() {
        final JPanel result = new JPanel();

        result.setBorder(BorderFactory.createTitledBorder("%s: %d cut%s".formatted(m_stock.name, m_cuts.size(), m_cuts.size() > 1 ? "s" : "")));
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        /*
        m_cuts.forEach(cut -> {
            JLabel label = new JLabel("%s at %.3f, from %.3f to %.3f".formatted(cut.direction() == CutDirection.HORIZONTAL ? "Rip" : "Cross", cut.at(), cut.start(), cut.stop()));

            result.add(label);

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

        // SALMAN: Use this instead of labels to keep the cuts highlighted?
        final JList<Cut> cutsList = new JList<Cut>(m_cuts.toArray(new Cut[m_cuts.size()]));

        cutsList.addListSelectionListener(new ListSelectionListener() {
            protected Cut m_lastSelection = null;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (cutsList.getSelectedIndex() == -1) {
                    // No selection any more; if we have a previous selection, deselect it
                    if (m_lastSelection != null) {
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_DESELECTED, m_lastSelection);

                        m_lastSelection = null;
                    }
                } else {
                    final Cut selection = cutsList.getSelectedValue();

                    if (selection != m_lastSelection) {
                        // Deselect the previous selection.
                        EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_DESELECTED, m_lastSelection);

                        EventBus.INSTANCE.triggerEvent(EventBus.Event.CUT_SELECTED, selection);

                        m_lastSelection = selection;
                    }
                }
            }
        });

        cutsList.setCellRenderer(new CutCellRenderer(true));

        result.add(new JScrollPane(cutsList));

        return result;
    }

    protected List<Cut> consolidateCuts(List<Cut> inboundCuts) {
        // SALMAN: remove
        if (true || m_cutPreference == CutPreference.RIP || m_cutPreference == CutPreference.CROSS) {
        // SALMAN: Uncomment
        // if (m_cutPreference == CutPreference.RIP || m_cutPreference == CutPreference.CROSS) {
            return inboundCuts;
        }

        List<Cut> cuts = new ArrayList<>();

        inboundCuts.forEach(cuts::add);

        // SALMAN: Step 1, get all the cuts sorted by board using stock.forEach.filter(cut.board == stock).
        // System.out.println("-=-=-=-=-= consolidateCuts " + "[" + "inboundCuts.size(): " + inboundCuts.size() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        Set<Cut> alreadyConsideredCuts = new HashSet<>();

        // Get all cuts that don't end at the end of the board (because those can't have more cuts after them to form a chain) and sort them by their starting point
        // so we don't have to worry about accidentally starting a chain in the middle.
        List<Cut> notThroughCuts = cuts.stream()
            .filter(cut -> cut.stop() < (cut.direction() == CutDirection.HORIZONTAL ? m_stock.w : m_stock.h))
            .sorted((cut1, cut2) -> Double.compare(cut1.start(), cut2.start()))
            .toList();

        double kerf = m_solution.getKerf();

        // System.out.println("-=-=-=-=-= consolidateCuts notThroughCuts.size(): " + (notThroughCuts.size()) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        for (var cut : notThroughCuts) {
            // Already part of another cut chain.
            if (alreadyConsideredCuts.contains(cut)) {
                continue;
            }

            alreadyConsideredCuts.add(cut);

            // SALMAN: Take kerf into consideration.
            Optional<Cut> nextCutInChain = cuts.stream()
                .filter(candidateCut -> candidateCut.start() == cut.stop() + kerf && candidateCut.at() == cut.at() && candidateCut.direction() == cut.direction())
                .findFirst();

            // System.out.println("-=-=-=-=-= consolidateCuts nextCutInChain: " + (nextCutInChain) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

            boolean found = false;

            List<Cut> chain = new ArrayList<>();

            // SALMAN: remove
            if (nextCutInChain.isPresent()) {
                // m_highlightedCuts.put(cut, 10);

                found = true;
            }

            while (nextCutInChain.isPresent()) {
                Cut currentCut = nextCutInChain.get();

                chain.add(currentCut);
                alreadyConsideredCuts.add(cut);

                // m_highlightedCuts.put(currentCut, 10);

                nextCutInChain = cuts.stream()
                    .filter(candidateCut -> candidateCut.start() == currentCut.stop() + kerf && candidateCut.at() == currentCut.at() && candidateCut.direction() == currentCut.direction())
                    .findFirst();
            }

            // SALMAN: remove
            if (found) {
                // System.out.println("-=-=-=-=-= consolidateCuts chain.size(): " + (chain.size()) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                Cut lastCut = chain.get(chain.size() - 1);

                // System.out.println("-=-=-=-=-= consolidateCuts " + "[" + "cut.offCut().name: " + cut.offCut().name + "], [" + "lastCut.offCut().name: " + lastCut.offCut().name + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                Cut consolidatedCut = new Cut(cut.board(), cut.offCut(), cut.demandPiece(), cut.at(), cut.start(), lastCut.stop(), cut.direction());

                cuts.set(cuts.indexOf(cut), consolidatedCut);

                chain.stream().forEach(cuts::remove);

                // SALMAN: Need to find all cuts that go across these cuts and make them stop at the end of these (change their stop() to this board's at()) and move
                // SALMAN: this board above the very first such across board.
                // SALMAN:
                // SALMAN: These "across" cuts should go beyond, so they should start() before the at() of this cut and end after the at() and their at() should be
                // SALMAN: the stop() of each board.
                // SALMAN:
                // SALMAN: Then, move the consolidated cut before the across cut (just swap the consolidated cut and the across cut).
                // SALMAN:
                // SALMAN: Also need to remove the cuts that have been taken out from the notThroughCuts so they don't get processed incorrectly.

                Optional<Cut> optCrossCut = cuts.stream()
                    .filter(candidate -> candidate.direction() != cut.direction() && candidate.at() == cut.stop() && candidate.start() < cut.at() && candidate.stop() > cut.at())
                    .findFirst();

                // System.out.println("-=-=-=-=-= consolidateCuts optCrossCut: " + (optCrossCut) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                if (optCrossCut.isPresent()) {
                    Cut crossCut = optCrossCut.get();
                    int index    = cuts.indexOf(crossCut);

                    cuts.set(index, new Cut(crossCut.board(), crossCut.offCut(), crossCut.demandPiece(), crossCut.at(), crossCut.start(), cut.at(), crossCut.direction()));
                    cuts.add(index + 1, new Cut(crossCut.board(), crossCut.offCut(), crossCut.demandPiece(), crossCut.at(), cut.at() + kerf, crossCut.stop(), crossCut.direction()));

                    cuts.remove(consolidatedCut);
                    cuts.add(index, consolidatedCut);
                }

                break;
            }
        }

        System.out.println("-=-=-=-=-= consolidateCuts " + "[" + "cuts.size(): " + cuts.size() + "], [" + "inboundCuts.size(): " + inboundCuts.size() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        return cuts;
    }

    /*
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Dimension size = getSize();

        System.out.println("-=-=-=-=-= size: " + (size) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        System.out.println("-=-=-=-=-= getParent().getSize(): " + (getParent().getSize()) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
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
        Dimension componentSize = super.getPreferredSize();

        return new Dimension((int) componentSize.getWidth() + 20, (int) componentSize.getHeight() + 20);
    }
    */
}
