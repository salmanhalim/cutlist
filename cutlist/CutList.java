package cutlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import cutlist.genetic.GeneticBinPacker;
import cutlist.packer.Node;
import cutlist.packer.Packer.CutPreference;
import cutlist.packer.Packer.FitPreference;
import cutlist.packer.Packer.PieceOrdering;
import cutlist.packer.Packer;
import cutlist.ui.ComponentWithLabel;
import cutlist.ui.CutPiece;
import cutlist.ui.InfoLabel;
import cutlist.ui.SingleBoard;
import cutlist.util.ArgsProcessor;
import cutlist.util.EventBus;
import cutlist.util.WrapLayout;

public class CutList implements EventBus.Listener {
    public static record Stock(String label, double width, double height, int quantity, double cost) {}
    public static record Demand(String label, double width, double height, int quantity) {}

    protected CutListUi m_ui;

    protected List<Demand> m_demand = new ArrayList<>();
    protected List<Stock>  m_stock  = new ArrayList<>();

    protected FitPreference m_fitPreference    = FitPreference.BEST;
    protected CutPreference m_cutPreference    = CutPreference.RIP;
    protected PieceOrdering m_pieceOrdering    = PieceOrdering.AREA;
    protected PieceOrdering m_stockOrdering    = PieceOrdering.AREA;
    protected double        m_kerf             = 0.125;
    protected double        m_extraPadding     = 0.5;
    protected boolean       m_orderStockByCost = true;
    protected boolean       m_pieceRotation    = false;

    protected final InfoLabel m_stockName  = new InfoLabel("Selected board");
    protected final InfoLabel m_stockSize  = new InfoLabel("Size");
    protected final InfoLabel m_demandName = new InfoLabel("Selected demand");
    protected final InfoLabel m_demandSize = new InfoLabel("Size");
    protected final InfoLabel temp = new InfoLabel("Count"); // SALMAN: remove

    // For the right side panel
    protected final InfoLabel m_cutPreferenceLabel      = new InfoLabel("Cut preference");
    protected final InfoLabel m_fitPreferenceLabel      = new InfoLabel("Fit preference");
    protected final InfoLabel m_demandOrderingLabel     = new InfoLabel("Demand ordering");
    protected final InfoLabel m_stockOrderingLabel      = new InfoLabel("Stock ordering");
    protected final InfoLabel m_orderStockByCostLabel   = new InfoLabel("Order stock by cost");
    protected final InfoLabel m_solutionCostLabel       = new InfoLabel("Solution cost");
    protected final InfoLabel m_solutionEfficiencyLabel = new InfoLabel("Solution efficiency");
    protected final InfoLabel m_completeSolutionLabel   = new InfoLabel("Complete solution");
    protected final InfoLabel m_bladeKerfLabel          = new InfoLabel("Blade kerf");
    protected final InfoLabel m_extraPaddingLabel       = new InfoLabel("Extra padding");
    protected final InfoLabel m_pieceRotationLabel      = new InfoLabel("Piece rotation");
    protected final InfoLabel m_solutionScoreLabel      = new InfoLabel("Solution score");
    protected final InfoLabel m_totalStockAreaLabel     = new InfoLabel("Total stock area");
    protected final InfoLabel m_totalDemandAreaLabel    = new InfoLabel("Total demand area");
    protected final InfoLabel m_numRotatedPiecesLabel   = new InfoLabel("Number of rotated pieces");

    protected Packer       m_packer;
    protected List<Packer> m_solutions = new ArrayList<>();
    protected List<Packer> m_completeSolutions;

    protected JTabbedPane m_tabbedPane = new JTabbedPane();


    public CutList() {
        EventBus.INSTANCE.addListener(List.of(EventBus.Event.MOUSE_ENTERED_BOARD,
                        EventBus.Event.MOUSE_EXITED_BOARD,
                        EventBus.Event.MOUSE_ENTERED_CUT_PIECE,
                        EventBus.Event.MOUSE_EXITED_CUT_PIECE,
                        EventBus.Event.TEMP),
                this);
    }

    public List<Stock> getStock() {
        return m_stock;
    }

    public void setStock(List<Stock> val) {
        m_stock = val;
    }

    public void addStock(String name, double w, double h, int quantity, double cost) {
        m_stock.add(new Stock(name, w, h, quantity, cost));
    }

    public List<Demand> getDemand() {
        return m_demand;
    }

    public void setDemand(List<Demand> val) {
        m_demand = val;
    }

    public void addDemand(String name, double w, double h, int quantity) {
        m_demand.add(new Demand(name, w, h, quantity));
    }

    public CutPreference getCutPreference() {
        return m_cutPreference;
    }

    public void setCutPreference(CutPreference val) {
        m_cutPreference = val;
    }

    public FitPreference getFitPreference() {
        return m_fitPreference;
    }

    public void setFitPreference(FitPreference val) {
        m_fitPreference = val;
    }

    public PieceOrdering getPieceOrdering() {
        return m_pieceOrdering;
    }

    public void setPieceOrdering(PieceOrdering val) {
        m_pieceOrdering = val;
    }

    public PieceOrdering getStockOrdering() {
        return m_stockOrdering;
    }

    public void setStockOrdering(PieceOrdering val) {
        m_stockOrdering = val;
    }

    public boolean isOrderStockByCost() {
        return m_orderStockByCost;
    }

    public void setOrderStockByCost(boolean val) {
        m_orderStockByCost = val;
    }

    public boolean isPieceRotation() {
        return m_pieceRotation;
    }

    public void setPieceRotation(boolean val) {
        m_pieceRotation = val;
    }


    public void generateSolution() {
        m_packer = new Packer();

        m_packer.setFirstFit(m_fitPreference == FitPreference.FIRST);
        m_packer.setCutPreference(m_cutPreference);
        m_packer.setPieceOrdering(m_pieceOrdering);
        m_packer.setOrderStockByCost(m_orderStockByCost);
        m_packer.setStockOrdering(m_stockOrdering);
        m_packer.setKerf(m_kerf);
        m_packer.setExtraPadding(m_extraPadding);
        m_packer.setPieceRotation(m_pieceRotation);

        final int demandCount = m_demand.stream().mapToInt(Demand::quantity).sum();

        System.out.println("-=-=-=-=-= demandCount: " + demandCount + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove

        m_demand.stream().forEach(m_packer::addDemand);

        // If a particular stock piece is unlimited, assume there is one for every piece of demand.
        m_stock.stream().forEach(stock -> IntStream.range(1, (stock.quantity() == 0 ? demandCount : stock.quantity()) + 1)
                .forEach(i -> m_packer.addStock(new Node(stock.label() + " " + i, 0, 0, stock.width, stock.height, stock.cost))));

        System.out.println();

        m_packer.fit();
    }

    public void generateAllSolutions() {
        m_solutions.clear();

        final int demandCount = m_demand.stream().mapToInt(Demand::quantity).sum();

        Stream.of(CutPreference.values()).forEach(cutPreference -> {
            Stream.of(FitPreference.values()).forEach(fitPreference -> {
                Stream.of(PieceOrdering.values()).filter(ordering -> ordering != PieceOrdering.RANDOM).forEach(demandPieceOrdering -> {
                    Stream.of(PieceOrdering.values()).forEach(stockPieceOrdering -> {
                        List.of(Boolean.TRUE, Boolean.FALSE).stream().forEach(orderStockByCost -> {
                            // List.of(Boolean.TRUE, Boolean.FALSE).stream().forEach(pieceRotation -> {
                                Packer solution = new Packer();

                                m_solutions.add(solution);

                                solution.setCutPreference(cutPreference);
                                solution.setFirstFit(fitPreference == FitPreference.FIRST);
                                solution.setPieceOrdering(demandPieceOrdering);
                                solution.setStockOrdering(stockPieceOrdering);
                                solution.setOrderStockByCost(orderStockByCost);

                                solution.setKerf(m_kerf);
                                solution.setExtraPadding(m_extraPadding);

                                solution.setPieceRotation(m_pieceRotation);
                                // solution.setPieceRotation(pieceRotation);

                                m_demand.stream().forEach(solution::addDemand);

                                // If a particular stock piece is unlimited, assume there is one for every piece of demand.
                                m_stock.stream().forEach(stock -> IntStream.range(1, (stock.quantity() == 0 ? demandCount : stock.quantity()) + 1)
                                        .forEach(i -> solution.addStock(new Node(stock.label() + " " + i, 0, 0, stock.width, stock.height, stock.cost))));

                                solution.fit();

                                // System.out.println("-=-=-=-=-= generateAllSolutions solution: " + (solution) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                            // });
                        });
                    });
                });
            });
        });

        System.out.println("-=-=-=-=-= generateAllSolutions m_solutions.size(): " + (m_solutions.size()) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        m_completeSolutions = m_solutions.stream().filter(solution -> solution.getFailed().size() == 0).toList();

        System.out.println("-=-=-=-=-= generateAllSolutions m_completeSolutions.size(): " + (m_completeSolutions.size()) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
    }

    public void setupGeneticAlgorithm() {
        GeneticBinPacker g = new GeneticBinPacker(m_stock, m_demand);

        g.initialize();
        g.nextGeneration(); // SALMAN: remove and replace with a run that goes over multiple generations automatically
    }

    public void printSolution() {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("-=-=-=-=-= m_packer.isFirstFit(): " + m_packer.isFirstFit() + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
        System.out.println("-=-=-=-=-= m_packer.getCutPreference(): " + m_packer.getCutPreference() + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove

        final Set<String> usedNames = new HashSet<>();

        m_packer.getDemand().stream()
            .filter(d1 -> d1.fit != null)
            .sorted((d1, d2) -> {
                int result = d1.getRootName().compareTo(d2.getRootName());

                // System.out.println("-=-=-=-=-= d1.getRootName(): " + (d1.getRootName()) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                // System.out.println("-=-=-=-=-= d2.getRootName(): " + (d2.getRootName()) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                // System.out.println("-=-=-=-=-= COMPARING NAMES result: " + (result) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                if (result == 0) {
                    if (m_packer.getCutPreference() == CutPreference.RIP) {
                        result = Double.compare(d1.fit.x, d2.fit.x);
                        // System.out.println("-=-=-=-=-= d1.fit.x: " + (d1.fit.x) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                        // System.out.println("-=-=-=-=-= d1.fit.y: " + (d1.fit.y) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                        // System.out.println("-=-=-=-=-= COMPARING X result: " + (result) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                    } else {
                        result = Double.compare(d1.fit.y, d2.fit.y);
                        // System.out.println("-=-=-=-=-= d1.fit.y: " + (d1.fit.y) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                        // System.out.println("-=-=-=-=-= d1.fit.y: " + (d1.fit.y) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                        // System.out.println("-=-=-=-=-= COMPARING Y result: " + (result) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                    }
                }

                if (result == 0) {
                    if (m_packer.getCutPreference() == CutPreference.RIP) {
                        result = Double.compare(d1.fit.y, d2.fit.y);
                        // System.out.println("-=-=-=-=-= d1.fit.y: " + (d1.fit.y) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                        // System.out.println("-=-=-=-=-= d1.fit.y: " + (d1.fit.y) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                        // System.out.println("-=-=-=-=-= COMPARING Y result: " + (result) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                    } else {
                        result = Double.compare(d1.fit.x, d2.fit.x);
                        // System.out.println("-=-=-=-=-= d1.fit.x: " + (d1.fit.x) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                        // System.out.println("-=-=-=-=-= d1.fit.y: " + (d1.fit.y) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                        // System.out.println("-=-=-=-=-= COMPARING X result: " + (result) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")");  // SALMAN: remove
                    }
                }

                // System.out.println();

                return result;
            })
            .forEach(demand -> {
                final Node   root = demand.getRoot();
                final String name = root.name;

                if (!usedNames.contains(name)) {
                    System.out.println();
                    System.out.format("%22s%24s%16s%16s%16s%16s", name, "", "", root.w, root.h, root.w * root.h);
                    System.out.println("");
                    System.out.format("%22s%24s%16s%16s%16s%16s", "Display name", "x", "y", "w", "h", "area");
                    System.out.println("");

                    usedNames.add(name);
                }

                System.out.format("%22s%24s%16s%16s%16s%16s", demand.name, demand.fit.x, demand.fit.y, demand.w, demand.h, demand.w * demand.h);
                System.out.println("");
            });

        if (m_packer.getFailed().size() > 0) {
            System.out.println("Failures");
            m_packer.getFailed().stream().forEach(System.out::println);
        }
    }

    protected boolean solutionsAreStale() {
        if (m_solutions.size() == 0) {
            return true;
        }

        Packer solution = m_solutions.get(0);

        return solution.getKerf() != m_kerf || solution.getExtraPadding() != m_extraPadding || solution.isPieceRotation() != m_pieceRotation;
        // return solution.getKerf() != m_kerf || solution.getExtraPadding() != m_extraPadding;
    }

    public void createAndShowGui() {
        try {
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (final Exception e) {
            System.out.println("-=-=-=-=-= UNABLE TO SET METAL LOOK AND FEEL; WILL USE DEFAULT e: " + e + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
        }

        // Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        // Create and set up the window.
        final JFrame frame = new JFrame("Cut List");

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        m_ui = new CutListUi(this, frame);

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        m_ui.setMaxDisplayBoardWidth(s_argsProcessor.getInt("w").orElse(CutListUi.MAX_DISPLAY_BOARD_WIDTH));

        // SALMAN: Fix this
        /*
        m_ui.setPacker(m_packer);
        m_ui.setupSolution();
        */

        final Container contentPane = frame.getContentPane();

        JPanel leftPanel  = new JPanel();

        final GridBagLayout      gridBag = new GridBagLayout();
        final GridBagConstraints c       = new GridBagConstraints();
        int                      counter = 0;

        leftPanel.setLayout(gridBag);

        c.insets = new Insets(0, 0, 10, 0);
        c.gridx  = 0;
        c.fill   = GridBagConstraints.HORIZONTAL;

        c.gridy = counter++;

        leftPanel.add(new JButton("Generate solution") {
            {
                setFont(getFont().deriveFont(30F));
                setForeground(Color.RED);
                setMnemonic(KeyEvent.VK_G);

                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("-=-=-=-=-= e: " + (e) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                        generateSolution();
                        m_ui.setPacker(m_packer);

                        // Shows a random solution from all the generated ones.
                        // m_ui.setPacker(m_solutions.get(s_rand.nextInt(0, m_solutions.size())));

                        m_ui.setupSolution();
                        updateRightPanel(m_packer);

                        if (solutionsAreStale()) {
                            generateAllSolutions();
                        }
                    }
                });
            }
        }, c);


        /*
        c.gridy = counter++;

        leftPanel.add(new JButton("Generate all solutions") {{
            setFont(getFont().deriveFont(30F));
            setForeground(Color.RED);
            setMnemonic(KeyEvent.VK_A);

            addActionListener(e -> generateAllSolutions());
        }}, c);
        */


        Component centerComponents = m_ui.createCenterComponents();

        c.weighty = 0.3f;

        c.gridy   = counter++;
        c.fill    = GridBagConstraints.BOTH;
        leftPanel.add(createInfoPanel(), c);

        c.weighty = 0.5f;

        c.gridy   = counter++;
        c.fill    = GridBagConstraints.BOTH;
        leftPanel.add(m_ui.createLeftComponents(), c);

        // m_tabbedPane.add("Single solution",    createSingleSolutionPanel());
        m_tabbedPane.add("Single solution",    createCutOptionsPanel());
        m_tabbedPane.add("Multiple solutions", createMultipleSolutionsPanel());

        // m_tabbedPane.setPreferredSize(new Dimension(300, 500));

        c.gridy = counter++;
        leftPanel.add(m_tabbedPane, c);


        final JPanel configurationPanel = new JPanel();

        configurationPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        // configurationPanel.setLayout(new BoxLayout(configurationPanel, BoxLayout.Y_AXIS));
        configurationPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 2));

        JSpinner kerfField = new JSpinner(new SpinnerNumberModel(m_kerf, 0, 15 / 16.0, 1 / 16.0));

        kerfField.setEditor(new JSpinner.NumberEditor(kerfField));

        kerfField.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                m_kerf = ((SpinnerNumberModel) kerfField.getModel()).getNumber().doubleValue();
            }
        });


        final NumberFormat paddingFormat = NumberFormat.getNumberInstance();

        paddingFormat.setMaximumFractionDigits(4);

        final JFormattedTextField extraPaddingField = new JFormattedTextField(paddingFormat);

        extraPaddingField.setValue(m_extraPadding);

        extraPaddingField.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                m_extraPadding = ((Number) extraPaddingField.getValue()).doubleValue();
            }
        });


        JCheckBox pieceRotationCheckbox = new JCheckBox("Piece rotation", m_pieceRotation);

        pieceRotationCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_pieceRotation = pieceRotationCheckbox.isSelected();
            }
        });

        configurationPanel.add(new ComponentWithLabel("Blade kerf",    kerfField));
        configurationPanel.add(new ComponentWithLabel("Extra padding", extraPaddingField));
        configurationPanel.add(new ComponentWithLabel(null,            pieceRotationCheckbox));

        c.gridy = counter++;
        leftPanel.add(configurationPanel, c);


        // c.weighty = 0;
        // c.fill    = GridBagConstraints.NONE;

        // c.gridy = counter++;
        // singleSolutionPane.add(createCutOptionsPanel(), c);

        c.gridy = counter++;
        leftPanel.add(createUiOptionsPanel(), c);

        /*
        // Force the panels above to go to the top of the view.
        c.gridy   = counter++;
        c.weighty = 0.5f;
        c.fill    = GridBagConstraints.VERTICAL;
        leftPanel.add(Box.createVerticalGlue(), c);
        */


        JScrollPane rightPanel = new JScrollPane(getRightPanelComponents());

        rightPanel.setBorder(BorderFactory.createTitledBorder("Selected solution"));


        contentPane.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerComponents, rightPanel)));

        // Just a placeholder so it has SOME size. Actual size is controlled (effectively) through BoardContainer in the contructor where the JSplitPane's size is
        // set.
        frame.setSize(new Dimension(500, 500));

        // Display the window.
        frame.pack();

        // Center it.
        frame.setLocation((screenSize.width  - frame.getSize().width)  / 2,
                          (screenSize.height - frame.getSize().height) / 2);

        frame.setVisible(true);
    }

    // For adding items to a GridBagLayout from inside a stream loop.
    protected int m_counter;

    protected Component getRightPanelComponents() {
        JPanel result = new JPanel() {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(100, 300);
            }

            public Dimension getMaximumSize() {
                return new Dimension(300, 300);
            }
        };

        // result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        final GridBagLayout      gridBag = new GridBagLayout();
        final GridBagConstraints c       = new GridBagConstraints();

        m_counter = 0;

        result.setLayout(gridBag);

        c.gridx   = 0;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.insets  = new Insets(0, 0, 10, 0);
        c.weightx = 1;

        // SALMAN: Finish this
        List.of(m_cutPreferenceLabel,
                m_fitPreferenceLabel,
                m_demandOrderingLabel,
                m_stockOrderingLabel,
                m_orderStockByCostLabel,
                m_solutionCostLabel,
                m_solutionEfficiencyLabel,
                m_totalStockAreaLabel,
                m_totalDemandAreaLabel,
                m_completeSolutionLabel,
                m_bladeKerfLabel,
                m_extraPaddingLabel,
                m_pieceRotationLabel,
                m_solutionScoreLabel,
                m_numRotatedPiecesLabel).forEach(component -> {
            c.gridy = m_counter++;

            result.add(component, c);
        });

        c.gridy   = m_counter++;
        c.weighty = 1;
        result.add(Box.createVerticalGlue(), c);

        return result;
    }

    protected Component createMultipleSolutionsPanel() {
        JPanel multipleSolutionsPanel = new JPanel();

        multipleSolutionsPanel.setLayout(new BoxLayout(multipleSolutionsPanel, BoxLayout.Y_AXIS));

        // SALMAN: Finish this
        // SALMAN: This could show a table/grid where the columns are Rip, Cross, Mixed and rows are Cheapest, Fewest Boards, Most Boards, Largest discarded board,
        // SALMAN: Smallest overall discards (best packing).
        // SALMAN:
        // SALMAN: When they select a cell, we show the appropriate solution with details on the far right.
        multipleSolutionsPanel.add(new JButton("Show highest scoring solution") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .max(Comparator.comparing(Packer::getSolutionScore));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(new JButton("Show lowest scoring COMPLETE solution") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .filter(solution -> solution.getFailed().size() == 0)
                    .min(Comparator.comparing(Packer::getSolutionScore));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(new JButton("Show lowest scoring solution") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .min(Comparator.comparing(Packer::getSolutionScore));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(Box.createVerticalStrut(5));

        multipleSolutionsPanel.add(new JButton("Show most efficient solution") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .filter(solution -> solution.getFailed().size() == 0)
                    .max(Comparator.comparing(Packer::getSolutionEfficiency));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(new JButton("Show least efficient solution") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .filter(solution -> solution.getFailed().size() == 0)
                    .min(Comparator.comparing(Packer::getSolutionEfficiency));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(Box.createVerticalStrut(5));

        multipleSolutionsPanel.add(new JButton("Show lowest number of cuts") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .filter(solution -> solution.getFailed().size() == 0)
                    .min(Comparator.comparing(s -> s.getCuts().size()));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(new JButton("Show highest number of cuts") {{
            addActionListener(e -> {
                Optional<Packer> candidateSolution = m_solutions.stream()
                    .filter(solution -> solution.getFailed().size() == 0)
                    .max(Comparator.comparing(s -> s.getCuts().size()));

                if (candidateSolution.isPresent()) {
                    m_ui.setPacker(candidateSolution.get());

                    m_ui.setupSolution();
                    updateRightPanel(candidateSolution.get());
                }
            });
        }});

        multipleSolutionsPanel.add(Box.createVerticalStrut(5));

        Stream.of(CutPreference.values()).forEach(preference -> {
            multipleSolutionsPanel.add(new JButton("Show cheapest " + preference.getLabel() + " cut solution") {{
                addActionListener(e -> {
                    Optional<Packer> candidateSolution = m_solutions.stream()
                        .filter(solution -> solution.getFailed().size() == 0)
                        .filter(s -> s.getCutPreference() == preference)
                        .min(Comparator.comparing(s -> s.getCost()));

                    if (candidateSolution.isPresent()) {
                        m_ui.setPacker(candidateSolution.get());

                        m_ui.setupSolution();
                        updateRightPanel(candidateSolution.get());
                    }
                });
            }});
        });

        multipleSolutionsPanel.add(Box.createVerticalStrut(5));

        Stream.of(CutPreference.values()).forEach(preference -> {
            multipleSolutionsPanel.add(new JButton("Show most expensive " + preference.getLabel() + " cut solution") {{
                addActionListener(e -> {
                    Optional<Packer> candidateSolution = m_solutions.stream()
                        .filter(solution -> solution.getFailed().size() == 0)
                        .filter(s -> s.getCutPreference() == preference)
                        .max(Comparator.comparing(s -> s.getCost()));

                    if (candidateSolution.isPresent()) {
                        m_ui.setPacker(candidateSolution.get());

                        m_ui.setupSolution();
                        updateRightPanel(candidateSolution.get());
                    }
                });
            }});
        });

        return new JScrollPane(multipleSolutionsPanel) {
            @Override
            public Dimension getPreferredSize() {
                // return new Dimension(333, (int) super.getPreferredSize().getHeight());
                return new Dimension(333, 300);
            }
        };
    }

    protected void updateRightPanel(Packer solution) {
        m_cutPreferenceLabel.setText(solution.getCutPreference().getLabel());
        m_fitPreferenceLabel.setText(solution.isFirstFit() ? FitPreference.FIRST.getLabel() : FitPreference.BEST.getLabel());
        m_demandOrderingLabel.setText(solution.getPieceOrdering().getLabel());
        m_stockOrderingLabel.setText(solution.getStockOrdering().getLabel());
        m_orderStockByCostLabel.setText(solution.isOrderStockByCost() ? "Yes" : "No");
        m_solutionCostLabel.setText("$%.2f".formatted(solution.getCost()));
        m_solutionEfficiencyLabel.setText("%.2f%%".formatted(solution.getSolutionEfficiency() * 100));
        m_totalStockAreaLabel.setText("%.2f".formatted(solution.getTotalStockArea()));
        m_totalDemandAreaLabel.setText("%.2f".formatted(solution.getTotalDemandArea()));
        m_bladeKerfLabel.setText(solution.getKerf());
        m_extraPaddingLabel.setText(solution.getExtraPadding());
        m_solutionScoreLabel.setText("%.3f".formatted(solution.getSolutionScore()));


        int numFailed = solution.getFailed().size();

        m_completeSolutionLabel.setText(numFailed > 0 ? "Failed count: " + numFailed : "Complete");
        m_completeSolutionLabel.setAlert(numFailed > 0);


        boolean rotation = solution.isPieceRotation();

        m_pieceRotationLabel.setText(rotation ? "Yes" : "No");
        m_pieceRotationLabel.setAlert(rotation);


        int numRotatedPieces = solution.getNumRotatedPieces();

        m_numRotatedPiecesLabel.setText(numRotatedPieces);
        m_numRotatedPiecesLabel.setAlert(numRotatedPieces > 0);
    }

    protected Component createInfoPanel() {
        final JPanel infoPanel = new JPanel();

        infoPanel.setBorder(BorderFactory.createTitledBorder("Info"));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        // // SALMAN: remove temp and TEMP
        // List.of(m_stockName, m_stockSize, m_demandName, m_demandSize, temp).forEach(component -> {
        List.of(m_stockName, m_stockSize, m_demandName, m_demandSize).forEach(component -> {
            final int numComponents = infoPanel.getComponentCount();

            if (numComponents > 0) {
                infoPanel.add(Box.createVerticalStrut(5));
            }

            infoPanel.add(component);
        });

        return infoPanel;
    }

    protected Component createCutOptionsPanel() {
        final JPanel      result    = new JPanel();
        final JScrollPane container = new JScrollPane(result) {
            {
                setBorder(BorderFactory.createTitledBorder("Cut options"));
            }

            @Override
            public Dimension getPreferredSize() {
                // return new Dimension(333, (int) super.getPreferredSize().getHeight());
                return new Dimension(333, 300);
            }
        };

        final GridBagLayout      gridBag = new GridBagLayout();
        final GridBagConstraints c       = new GridBagConstraints();
        int                      counter = 0;

        result.setLayout(gridBag);

        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets  = new Insets(0, 0, 10, 0);

        final JPanel cutPreferencePanel = new JPanel();

        cutPreferencePanel.setBorder(BorderFactory.createTitledBorder("Cut preference"));

        // cutPreferencePanel.setLayout(new BoxLayout(cutPreferencePanel, BoxLayout.Y_AXIS));
        cutPreferencePanel.setLayout(new WrapLayout(FlowLayout.LEFT));

        ButtonGroup cutPreferenceGroup = new ButtonGroup();

        Stream.of(CutPreference.values()).forEach(item -> {
            JRadioButton radio = new JRadioButton(item.getLabel(), m_cutPreference == item);

            cutPreferencePanel.add(radio);
            cutPreferenceGroup.add(radio);

            radio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    m_cutPreference = item;
                }
            });
        });

        c.gridx = 0;
        c.gridy = counter++;
        result.add(cutPreferencePanel, c);


        final JPanel fitPreferencePanel = new JPanel();

        fitPreferencePanel.setBorder(BorderFactory.createTitledBorder("Fit preference"));

        // fitPreferencePanel.setLayout(new BoxLayout(fitPreferencePanel, BoxLayout.Y_AXIS));
        fitPreferencePanel.setLayout(new WrapLayout(FlowLayout.LEFT));

        ButtonGroup fitPreferenceGroup = new ButtonGroup();

        Stream.of(FitPreference.values()).forEach(item -> {
            JRadioButton radio = new JRadioButton(item.getLabel(), m_fitPreference == item);

            fitPreferencePanel.add(radio);
            fitPreferenceGroup.add(radio);

            radio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    m_fitPreference = item;
                }
            });
        });

        c.gridy = counter++;
        result.add(fitPreferencePanel, c);


        final JPanel pieceOrderingPanel = new JPanel();

        pieceOrderingPanel.setBorder(BorderFactory.createTitledBorder("Piece ordering (use with first fit)"));

        // pieceOrderingPanel.setLayout(new BoxLayout(pieceOrderingPanel, BoxLayout.Y_AXIS));
        pieceOrderingPanel.setLayout(new WrapLayout(FlowLayout.LEFT));

        final ButtonGroup pieceOrderingGroup = new ButtonGroup();

        Stream.of(PieceOrdering.values()).forEach(item -> {
            JRadioButton radio = new JRadioButton(item.getLabel(), m_pieceOrdering == item);

            pieceOrderingPanel.add(radio);
            pieceOrderingGroup.add(radio);

            radio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    m_pieceOrdering = item;
                }
            });
        });

        c.gridy = counter++;
        result.add(pieceOrderingPanel, c);


        final JPanel stockOrderingPanel = new JPanel();

        stockOrderingPanel.setBorder(BorderFactory.createTitledBorder("Stock ordering"));
        // stockOrderingPanel.setLayout(new BoxLayout(stockOrderingPanel, BoxLayout.Y_AXIS));
        stockOrderingPanel.setLayout(new WrapLayout(FlowLayout.LEFT));

        final JPanel      stockCheckboxes    = new JPanel();
        final JPanel      stockRadios        = new JPanel();
        final ButtonGroup stockOrderingGroup = new ButtonGroup();

        stockCheckboxes.setLayout(new WrapLayout(FlowLayout.LEFT));
        stockRadios.setLayout(new WrapLayout(FlowLayout.LEFT));

        // SALMAN: remove
        // stockCheckboxes.setBorder(BorderFactory.createTitledBorder("Checkboxes"));
        // stockRadios.setBorder(BorderFactory.createTitledBorder("Radios"));

        final JCheckBox stockOrder = new JCheckBox("Lower cost first (N/A for Random)", m_orderStockByCost);

        stockOrder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_orderStockByCost = stockOrder.isSelected();
            }
        });

        stockCheckboxes.add(stockOrder);

        Stream.of(PieceOrdering.values()).forEach(item -> {
            JRadioButton radio = new JRadioButton(item.getLabel(), m_stockOrdering == item);

            stockRadios.add(radio);
            stockOrderingGroup.add(radio);

            radio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    m_stockOrdering = item;
                }
            });
        });

        stockOrderingPanel.add(stockCheckboxes);
        stockOrderingPanel.add(stockRadios);

        c.gridy = counter++;
        result.add(stockOrderingPanel, c);


        // Force the panels above to go to the top of the view.
        c.gridy   = counter++;
        c.weighty = 1;
        result.add(Box.createVerticalGlue(), c);

        return container;
    }

    protected Component createUiOptionsPanel() {
        final JPanel      result    = new JPanel();
        final JScrollPane container = new JScrollPane(result) {
            {
                setBorder(BorderFactory.createTitledBorder("Interface options"));
            }

            @Override
            public Dimension getPreferredSize() {
                // return new Dimension(333, 100);
                return new Dimension(333, (int) super.getPreferredSize().getHeight());
            }
        };

        final GridBagLayout      gridBag = new GridBagLayout();
        final GridBagConstraints c       = new GridBagConstraints();
        int                      counter = 0;

        result.setLayout(gridBag);

        final JPanel uiPreferencePanel = new JPanel();

        /*
        uiPreferencePanel.setBorder(BorderFactory.createTitledBorder("UI preferences"));

        // uiPreferencePanel.setLayout(new BoxLayout(uiPreferencePanel, BoxLayout.Y_AXIS));
        uiPreferencePanel.setLayout(new WrapLayout(FlowLayout.LEFT));

        // SALMAN: remove
        uiPreferencePanel.add(new JButton("Test"));

        c.gridx = 0;
        c.gridy = counter++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        result.add(uiPreferencePanel, c);
        */


        // Force the panels above to go to the top of the view.
        /*
        c.gridy   = counter++;
        c.weighty = 1;
        result.add(Box.createVerticalGlue(), c);
        */

        return container;
    }

    @Override
    public void eventTriggered(EventBus.Event event, Object component) {
        switch (event) {
            case MOUSE_ENTERED_CUT_PIECE:
                if (component instanceof final CutPiece cutPiece) {
                    final Node demand = cutPiece.getDemand();

                    m_demandName.setText(demand.name);
                    m_demandSize.setText(demand.h + " x " + demand.w);
                }

                break;

            case MOUSE_ENTERED_BOARD:
                if (component instanceof final SingleBoard singleBoard) {
                    final Node stock = singleBoard.getStock();

                    m_stockName.setText(stock.name);
                    m_stockSize.setText(stock.h + " x " + stock.w);
                }

                break;

            case MOUSE_EXITED_BOARD:
                m_stockName.setText(null);
                m_stockSize.setText(null);

                break;

            case MOUSE_EXITED_CUT_PIECE:
                m_demandName.setText(null);
                m_demandSize.setText(null);

                break;

            case TEMP:
                temp.setText(String.valueOf(component));

                break;

            default:
                // Nothing; we don't handle every event here, but we don't want compiler issues for missing values.
        }
    }


    // -------------------- Static running helpers Begin --------------------

    protected static Random        s_rand           = new Random();
    protected static ArgsProcessor s_argsProcessor;

    protected static void readInputFile(String filename, CutList cutList) {
        try {
            final Path   path    = FileSystems.getDefault().getPath(filename);
            final String content = Files.readString(path);

            System.out.println("-=-=-=-=-= readInputFile " + "[" + "filename: " + filename + "], [" + "path: " + path + "]" + " (" + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove

            List<String> lines = content.lines()
                .filter(line -> !line.isBlank())
                .map(String::strip)
                .filter(line -> !line.startsWith("#") && !line.startsWith("--"))
                .toList();

            System.out.println("-=-=-=-=-= lines.size(): " + lines.size() + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove

            boolean inDemand    = false;
            boolean inStock     = false;
            String  name        = null;
            int     demandCount = 1;
            int     stockCount  = 1;

            for (final var line : lines) {
                if ("Demand".equals(line)) {
                    inDemand = true;
                    inStock  = false;

                    continue;
                }
                if ("Stock".equals(line)) {
                    inStock  = true;
                    inDemand = false;

                    continue;
                }

                final String[] pieces = line.split("  ");

                if (inDemand) {
                    // System.out.println("-=-=-=-=-= DEMAND line: " + (line) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                    name = pieces.length >= 4 ? pieces[3] : "Demand" + demandCount++;

                    cutList.addDemand(name, Double.parseDouble(pieces[1]), Double.parseDouble(pieces[0]), Integer.parseInt(pieces[2]));
                } else if (inStock) {
                    // System.out.println("-=-=-=-=-= STOCK line: " + (line) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                    name = pieces.length >= 5 ? pieces[4] : "Stock" + stockCount++;

                    cutList.addStock(name, Double.parseDouble(pieces[1]), Double.parseDouble(pieces[0]), Integer.parseInt(pieces[2]), Double.parseDouble(pieces[3]));
                }
            }
        } catch (final IOException ioe) {
            System.out.println("-=-=-=-=-= ioe: " + ioe + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
        }

        // System.out.println("-=-=-=-=-= cutList.m_demand: " + (cutList.m_demand) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        // System.out.println("-=-=-=-=-= cutList.m_stock: " + (cutList.m_stock) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
    }

    protected static int randomWidth() {
        return s_rand.nextInt(60, 97);
    }

    protected static int randomHeight() {
        return s_rand.nextInt(30, 49);
    }

    public static void main(String[] args) {
        s_argsProcessor = new ArgsProcessor(args);

        final CutList cutList = new CutList();

        // readInputFile(s_argsProcessor.getExtraArguments().orElse("C:/tmp/Canvas31x34_32x35_HomeDepot.txt"), cutList);

        // IntStream.range(1, 20).forEach(i -> cutList.addStock("Board" + i, 2000, 1000));
        // cutList.addStock("Board", 96, 48, 100, 0);
        // cutList.addStock("Board", 96, 48, 2, 0);
        // cutList.addStock("Plywood", 96, 48, s_rand.nextInt(5, 11), 60);
        // cutList.addStock("Plywood rotated", 48, 96, s_rand.nextInt(5, 11), 60);
        // cutList.addStock("Baltic birch", 60, 60, s_rand.nextInt(5, 11), 100);
        IntStream.range(1, s_rand.nextInt(5, 10)).forEach(i -> cutList.addStock("Board_" + i,
                        randomWidth(),
                        randomHeight(),
                        s_rand.nextInt(1, 4),
                        s_rand.nextDouble(0d, 100d)));
        // cutList.addStock("Board" + 1, 2000, 1500);
        // cutList.addStock("Board" + 2, 2000, 1500);
        // IntStream.range(1, s_rand.nextInt(3) + 2).forEach(i -> cutList.addStock("Board" + i, 2000, 1500));
        // IntStream.range(1, 11).forEach(i -> cutList.addStock("Board" + i, s_rand.nextInt(600) + 600, s_rand.nextInt(400) + 400));
        // IntStream.range(1, s_rand.nextInt(5) + 2).forEach(i -> cutList.addStock("Board" + i, s_rand.nextInt(300) + 300, s_rand.nextInt(400) + 400));
        // IntStream.range(1, 4).forEach(i -> cutList.addStock("Board" + i, 600, 800));
        int numDemands = s_rand.nextInt(30) + 30;
        // numDemands = 6; // SALMAN: remove
        IntStream.range(1, numDemands + 1).forEach(i -> cutList.addDemand("Demand" + i, s_rand.nextDouble(10, 21), s_rand.nextDouble(2, 10), s_rand.nextInt(12) + 1));

        cutList.setCutPreference("rip".equals(s_argsProcessor.get("c")) ? CutPreference.RIP : CutPreference.CROSS);
        if (s_argsProcessor.get("c") != null) {
            cutList.setCutPreference(switch (s_argsProcessor.get("c")) {
                case "rip"          -> CutPreference.RIP;
                case "cross"        -> CutPreference.CROSS;
                case "mix", "mixed" -> CutPreference.MIXED;
                default             -> CutPreference.CROSS;
            });
        }

        // Set a default based on the cut preference.
        PieceOrdering pieceOrdering = cutList.getCutPreference() == CutPreference.RIP ? PieceOrdering.HEIGHT : PieceOrdering.WIDTH;

        // Override it if explicitly specified.
        if (s_argsProcessor.get("o") != null) {
            switch (s_argsProcessor.get("o")) {
                case "area"   -> pieceOrdering = PieceOrdering.AREA;
                case "height" -> pieceOrdering = PieceOrdering.HEIGHT;
                case "random" -> pieceOrdering = PieceOrdering.RANDOM;
                case "width"  -> pieceOrdering = PieceOrdering.WIDTH;
            }
        }

        System.out.println("-=-=-=-=-= pieceOrdering: " + (pieceOrdering) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        cutList.setPieceOrdering(pieceOrdering);

        if (s_argsProcessor.get("f") != null) {
            cutList.setFitPreference(switch (s_argsProcessor.get("f")) {
                case "first"  -> FitPreference.FIRST;
                case "best"   -> FitPreference.BEST;
                default       -> FitPreference.BEST;
            });
        }

        if (s_argsProcessor.getBoolean("p")) {
            cutList.printSolution();
        }

        SwingUtilities.invokeLater(() -> cutList.createAndShowGui());


        cutList.setupGeneticAlgorithm();

        cutList.generateAllSolutions(); // SALMAN: remove
    }
}
