package cutlist.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import cutlist.CutList.Demand;

public class Packer {
    protected static Random s_rand = new Random();

    public static enum CutPreference {
        CROSS("Cross"),
        RIP("Rip", true),
        MIXED("Mix"),
        LONG_SIDE_FIRST("Long side first"),
        SHORT_SIDE_FIRST("Short side first", true);

        protected String  m_label;
        protected boolean m_rip;

        CutPreference(String label) {
            this(label, false);
        }

        CutPreference(String label, boolean rip) {
            m_label = label;
            m_rip   = rip;
        }

        public String getLabel() {
            return m_label;
        }

        public boolean isRip() {
            return m_rip;
        }
    };

    public static enum FitPreference {
        FIRST("First fit"),
        BEST("Best fit");

        protected String m_label;

        FitPreference(String label) {
            m_label = label;
        }

        public String getLabel() {
            return m_label;
        }
    };

    public static enum CutDirection {
        HORIZONTAL, VERTICAL;
    };

    public static enum PieceOrdering {
        WIDTH("Width"),
        HEIGHT("Height"),
        AREA("Area"),
        RANDOM("Random"),
        ORIGINAL("Original order (unsorted)");

        protected String m_label;

        PieceOrdering(String label) {
            m_label = label;
        }

        public String getLabel() {
            return m_label;
        }
    }

    public static record Cut(Node board, Node offCut, Node demandPiece, double at, double start, double stop, CutDirection direction) {
        @Override
        public String toString() {
            return "Cut={"
                + "board="         + board().name
                + ", offCut="      + offCut().name
                + ", demandPiece=" + demandPiece().name
                + ", at="          + at()
                + ", start="       + start()
                + ", stop="        + stop()
                + ", direction="   + direction()
                + "}";
        }
    };

    protected List<Node>       m_stock  = new ArrayList<>();
    protected List<Node>       m_demand = new ArrayList<>();
    protected final List<Cut>  m_cuts   = new ArrayList<>();

    protected int                    m_numRotatedPieces;
    protected List<Node>             m_usedBoards;
    protected double                 m_cost;
    protected double                 m_totalStockArea;
    protected double                 m_totalDemandArea;
    protected double                 m_solutionEfficiency;
    protected List<Node>             m_fittedDemand;
    protected final List<Node>       m_failed              = new ArrayList<>();

    protected boolean       m_firstFit         = false;
    protected CutPreference m_cutPreference    = CutPreference.CROSS;
    protected PieceOrdering m_pieceOrdering    = PieceOrdering.WIDTH;
    protected PieceOrdering m_stockOrdering    = PieceOrdering.AREA;
    protected boolean       m_orderStockByCost = true;
    protected boolean       m_pieceRotation    = false;
    protected double        m_kerf             = 0.125;
    protected double        m_extraPadding     = 0.5;
    protected double        m_solutionScore;

    protected final Map<Demand, List<Node>> m_allDemands = new HashMap<>();

    private final Set<Node> m_bestFitCandidates = new HashSet<>();

    private CutPreference m_currentCutPreference = m_cutPreference;

    public Packer() {}

    public Packer(int numofpackets, double w, double h) {
        for (int i = 0; i < numofpackets; i++) {
            m_stock.add(new Node("Board" + i, 0, 0, w, h));
        }
    }

    public List<Node> getStock() {
        return m_stock;
    }

    public void setStock(List<Node> val) {
        m_stock = val;
    }

    public void addStock(Node board) {
        m_stock.add(board);
    }

    public List<Node> getDemand() {
        return m_demand;
    }

    public void setDemand(List<Node> val) {
        m_demand = val;
    }

    public void addDemand(Node demand) {
        m_demand.add(demand);
    }

    public void addDemand(Demand demands) {
        List<Node> boards = new ArrayList<>();

        m_allDemands.put(demands, boards);

        IntStream.range(1, demands.quantity() + 1)
            .forEach(i -> {
                Node singleBoard = new Node(demands.label() + " " + i, demands.width(), demands.height(), demands);

                boards.add(singleBoard);

                addDemand(singleBoard);
            });
    }

    public boolean isFirstFit() {
        return m_firstFit;
    }

    public void setFirstFit(boolean val) {
        m_firstFit = val;
    }

    public CutPreference getCutPreference() {
        return m_cutPreference;
    }

    public void setCutPreference(CutPreference val) {
        m_cutPreference        = val;
        m_currentCutPreference = m_cutPreference;
    }

    public List<Node> getFailed() {
        return m_failed;
    }

    public double getKerf() {
        return m_kerf;
    }

    public void setKerf(double val) {
        m_kerf = val;
    }

    public double getExtraPadding() {
        return m_extraPadding;
    }

    public void setExtraPadding(double val) {
        m_extraPadding = val;
    }

    public List<Cut> getCuts() {
        return m_cuts;
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

    public double getCost() {
        return m_cost;
    }

    public List<Node> getFittedDemand() {
        return m_fittedDemand;
    }

    public double getSolutionEfficiency() {
        return m_solutionEfficiency;
    }

    public boolean isPieceRotation() {
        return m_pieceRotation;
    }

    public void setPieceRotation(boolean val) {
        m_pieceRotation = val;
    }

    public double getSolutionScore() {
        return m_solutionScore;
    }

    public double getTotalStockArea() {
        return m_totalStockArea;
    }

    public double getTotalDemandArea() {
        return m_totalDemandArea;
    }

    public int getNumRotatedPieces() {
        return m_numRotatedPieces;
    }


    // If same size preference, rearrange based on height for rips, width for cross and area for mixed.
    // Descending order.
    protected List<Node> sortBoards(List<Node> boards, PieceOrdering ordering, boolean orderByCost) {
        // System.out.println("-=-=-=-=-= sortBoards " + "[" + "boards.size(): " + boards.size() + "], [" + "ordering: " + ordering + "], [" + "orderByCost: " + orderByCost + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        if (ordering == PieceOrdering.ORIGINAL && orderByCost) {
            boards.sort((a, b) -> Double.compare(a.cost, b.cost));
        } else if (ordering == PieceOrdering.RANDOM) {
            Collections.shuffle(boards);
        } else if (ordering != PieceOrdering.ORIGINAL) {
            boards = boards.stream().sorted((a, b) -> {
                // Order by cost if so desired.
                int result = orderByCost ? Double.compare(a.cost, b.cost) : 0;

                // If same cost (or cost not considered), then order by chosen dimension.
                if (result == 0) {
                    switch (ordering) {
                        case HEIGHT:
                            result = -Double.compare(a.h, b.h);

                            break;

                        case WIDTH:
                            result = -Double.compare(a.w, b.w);

                            break;

                        case AREA:
                        default:
                            result = -Double.compare(a.w * a.h, b.w * b.h);
                    }
                }

                return result;
            }).toList();
        }

        return boards;
    }

    public void fit() {
        if (m_stock.size() == 0) {
            m_failed.addAll(m_demand);

            return;
        }

        m_failed.clear();

        m_numRotatedPieces = 0;

        m_stock  = sortBoards(m_stock,  m_stockOrdering, m_orderStockByCost);
        m_demand = sortBoards(m_demand, m_pieceOrdering, false);

        if (m_firstFit) {
            for (var demandPiece : m_demand) {
                boolean rotated = false;
                Node    node    = null;

                // Chance of forced rotation
                // // SALMAN: remove
                // if (!m_pieceRotation || s_rand.nextDouble() > 0.5d) {
                    for (var stockBoard : m_stock) {
                        // If first fit, do it once and process.
                        // If best fit, we need to collect every stockBoard's result (findNodeFirstFit will have to add it to a collection along with returning it).
                        generateCutPreference(stockBoard);

                        node = findNodeFirstFit(stockBoard, demandPiece.w, demandPiece.h);

                        if (node != null) {
                            break;
                        }
                    }
                // }

                // Didn't fit on any board, so try rotating it.
                if (node == null && m_pieceRotation) {
                    rotated = true;

                    for (var stockBoard : m_stock) {
                        // If first fit, do it once and process.
                        // If best fit, we need to collect every stockBoard's result (findNodeFirstFit will have to add it to a collection along with returning it).
                        generateCutPreference(stockBoard);

                        node = findNodeFirstFit(stockBoard, demandPiece.h, demandPiece.w);

                        if (node != null) {
                            break;
                        }
                    }
                }

                if (node != null) {
                    demandPiece.fit = rotated ?
                        splitNode(node, demandPiece, demandPiece.h + m_extraPadding, demandPiece.w + m_extraPadding) :
                        splitNode(node, demandPiece, demandPiece.w + m_extraPadding, demandPiece.h + m_extraPadding);

                    demandPiece.rotated = rotated;

                    if (node.isroot) {
                        demandPiece.fit.isroot = true;
                    }

                    if (rotated) {
                        m_numRotatedPieces++;
                    }
                } else {
                    m_failed.add(demandPiece);
                }
            }
        } else {
            // This can't be aborted mid-stream via a break (after the first match), so can't be used for the first fit.
            m_demand.stream().forEach(demandPiece -> {
                m_bestFitCandidates.clear();

                boolean rotated = false;

                m_stock.stream().forEach(stockBoard -> {
                    generateCutPreference(stockBoard);

                    findNodeBestFit(stockBoard, demandPiece.w, demandPiece.h);
                });

                if (m_bestFitCandidates.size() == 0 && m_pieceRotation) {
                    rotated = true;

                    m_stock.stream().forEach(stockBoard -> {
                        generateCutPreference(stockBoard);

                        findNodeBestFit(stockBoard, demandPiece.h, demandPiece.w);
                    });
                }

                // SALMAN: When examining the best fit results, can we save whether this was a right baord or a down board and give preference to identical results
                // SALMAN: based on rip vs. cross? (If rip, prefer right results and vice versa.)
                // System.out.println("-=-=-=-=-= Message: " + "M_bestFitCandidates" + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                // m_bestFitCandidates.stream().forEach(System.out::println);

                // Minimize area
                // Optional<Node> best = m_bestFitCandidates.stream().min((a, b) -> Double.compare(a.w * a.h, b.w * b.h));
                Optional<Node> best = m_bestFitCandidates.stream().min(Comparator.comparing(a -> a.w * a.h));

                /*
                System.out.println("-=-=-=-=-= demandPiece: " + (demandPiece) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                System.out.println("-=-=-=-=-= best: " + (best) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
                System.out.println();
                */

                if (best.isPresent()) {
                    Node bestCandidate = best.get();

                    demandPiece.fit = rotated ?
                        splitNode(bestCandidate, demandPiece, demandPiece.h + m_extraPadding, demandPiece.w + m_extraPadding) :
                        splitNode(bestCandidate, demandPiece, demandPiece.w + m_extraPadding, demandPiece.h + m_extraPadding);

                    demandPiece.rotated = rotated;

                    if (rotated) {
                        m_numRotatedPieces++;
                    }

                    if (bestCandidate.isroot) {
                        demandPiece.fit.isroot = true;
                    }
                } else {
                    m_failed.add(demandPiece);
                }
            });
        }

        m_usedBoards     = m_stock.stream().filter(board -> board.used).toList();
        m_cost           = m_usedBoards.stream().mapToDouble(Node::getCost).sum();
        m_fittedDemand   = m_demand.stream().filter(Predicate.not(m_failed::contains)).toList();

        m_totalStockArea     = m_usedBoards.stream().mapToDouble(board -> board.w * board.h).sum();
        m_totalDemandArea    = m_fittedDemand.stream().mapToDouble(demand -> demand.w * demand.h).sum();
        m_solutionEfficiency = m_totalDemandArea / m_totalStockArea;

        computeSolutionFitnessScore();
    }

    /**
     * Higher score is better.
     */
    protected void computeSolutionFitnessScore() {
        // A complete solution is very important and trumps everything else
        double score = m_failed.size() == 0 ? 1 : 0;

        // Higher efficiency
        score += m_solutionEfficiency * 10;

        // Above this are the really important criteria
        score *= 1000 * m_demand.size();

        // Number of pieces cut
        score += m_demand.size() - m_failed.size();

        // More cuts is bad
        score -= m_cuts.size();

        // Lots of waste area is bad
        score -= (m_totalStockArea - m_totalDemandArea);

        // The discarded boards can potentially get used again, but kerf cuts are sawdust, so more kerf area is worse than just discard
        // // SALMAN: Finish this?
        // score -= areaOfKerfCuts;

        // Best fit is preferred
        score += m_firstFit ? 0 : 1;

        // Prefer fewer boards
        score -= 10 * m_demand.size() * m_usedBoards.size();

        // Prefer lower cost
        score -= 500 * m_cost;

        // Prefer fewer discarded pieces
        // // SALMAN: Finish this?
        // score -= discards.length;

        /*
        // Prefer solutions that leave one giant board over those that don't
        score += largestDiscardedPiece ? largestDiscardedPiece.getAreaOfOneBoard() : 0;
        */

        // Can be reused, but still prefer it without
        // // SALMAN: Finish this
        // score -= totalDiscardedArea;

        // Prefer solutions that rotate fewer pieces
        score -= m_numRotatedPieces * 10;

        m_solutionScore = score;
    }

    protected void generateCutPreference(Node stockBoard) {
        switch (m_cutPreference) {
            case LONG_SIDE_FIRST:
                m_currentCutPreference = stockBoard.w > stockBoard.h ? CutPreference.CROSS : CutPreference.RIP;
                // SALMAN: Fix this
                // m_currentCutPreference = CutPreference.RIP;

                break;

            case SHORT_SIDE_FIRST:
                m_currentCutPreference = stockBoard.w > stockBoard.h ? CutPreference.RIP : CutPreference.CROSS;
                // SALMAN: Fix this
                // m_currentCutPreference = CutPreference.RIP;

                break;

            case MIXED:
                m_currentCutPreference = s_rand.nextInt(0, 2) == 0 ? CutPreference.RIP : CutPreference.CROSS;

                break;

            default:
                m_currentCutPreference = m_cutPreference;
        }
    }

    protected void findNodeBestFit(Node stockBoard, double w, double h) {
        if (stockBoard == null) {
            return;
        }

        // generateCutPreference(stockBoard);

        if (stockBoard.used) {
            if (m_currentCutPreference.isRip()) {
                findNodeBestFit(stockBoard.right, w, h);
                findNodeBestFit(stockBoard.down, w, h);
            } else {
                findNodeBestFit(stockBoard.down, w, h);
                findNodeBestFit(stockBoard.right, w, h);
            }
        } else if (w + m_extraPadding <= stockBoard.w && h + m_extraPadding <= stockBoard.h) {
            m_bestFitCandidates.add(stockBoard);
        }
    }

    public Node findNodeFirstFit(Node stockBoard, double w, double h) {
        if (stockBoard == null) {
            return null;
        }

        // generateCutPreference(stockBoard);

        if (stockBoard.used) {
            Node nextNode = m_currentCutPreference.isRip() ? findNodeFirstFit(stockBoard.right, w, h) : findNodeFirstFit(stockBoard.down, w, h);

            return nextNode != null ? nextNode : m_currentCutPreference.isRip() ? findNodeFirstFit(stockBoard.down, w, h) : findNodeFirstFit(stockBoard.right, w, h);
        } else if (w + m_extraPadding <= stockBoard.w && h + m_extraPadding <= stockBoard.h) {
            return stockBoard;
        } else {
            return null;
        }
    }

    public Node splitNode(Node node, Node demandPiece, double w, double h) {
        node.used = true;

        Node    rootNode    = node.rootNode == null ? node : node.rootNode;
        boolean addRipCut   = false;
        boolean addCrossCut = false;

        if (m_cutPreference == CutPreference.MIXED) {
            m_currentCutPreference = s_rand.nextInt(0, 2) == 0 ? CutPreference.RIP : CutPreference.CROSS;  // Randomize
            // m_currentCutPreference = node.w > node.h ? CutPreference.CROSS : CutPreference.RIP;            // Base on node size
        } else {
            switch (m_cutPreference) {
                case LONG_SIDE_FIRST:
                    m_currentCutPreference = node.w > node.h ? CutPreference.CROSS : CutPreference.RIP;

                    break;

                case SHORT_SIDE_FIRST:
                    m_currentCutPreference = node.w > node.h ? CutPreference.RIP : CutPreference.CROSS;

                    break;
            }
            // generateCutPreference(node);
        }

        // The cuts done in the code are in the reverse order from those that will have to be done when actually cutting the board, so they're added to the list of
        // cuts in the opposite order from how the board is actually split here in the code.
        if (m_currentCutPreference.isRip()) {
            if (node.w > w) {
                // We have to make a cut if the board fits.
                addCrossCut = true;

                // We only get another board if there will be something left after taking the kerf into consideration.
                if (node.w > w + m_kerf) {
                    node.right = new Node(node.name + "Right", node.x + w + m_kerf, node.y, node.w - w - m_kerf, h);
                }
            }

            // We have to make a cut if the board fits.
            if (node.h > h) {
                addRipCut = true;

                // We only get another board if there will be something left after taking the kerf into consideration.
                if (node.h > h + m_kerf) {
                    node.down  = new Node(node.name + "Down", node.x, node.y + h + m_kerf, node.w, node.h - h - m_kerf);
                }
            }

            if (addRipCut) {
                m_cuts.add(new Cut(rootNode, node, demandPiece, node.y + h, node.x, node.x + node.w, CutDirection.HORIZONTAL));
            }

            if (addCrossCut) {
                m_cuts.add(new Cut(rootNode, node, demandPiece, node.x + w, node.y, node.y + h, CutDirection.VERTICAL));
            }
        } else {
            if (node.h > h) {
                // We have to make a cut if the board fits.
                addRipCut = true;

                // We only get another board if there will be something left after taking the kerf into consideration.
                if (node.h > h + m_kerf) {
                    node.down  = new Node(node.name + "Down", node.x, node.y + h + m_kerf, w, node.h - h - m_kerf);
                }
            }

            // We have to make a cut if the board fits.
            if (node.w > w) {
                addCrossCut = true;

                // We only get another board if there will be something left after taking the kerf into consideration.
                if (node.w > w + m_kerf) {
                    node.right = new Node(node.name + "Right", node.x + w + m_kerf, node.y, node.w - w - m_kerf, node.h);
                }
            }

            if (addCrossCut) {
                m_cuts.add(new Cut(rootNode, node, demandPiece, node.x + w, node.y, node.y + node.h, CutDirection.VERTICAL));
            }

            if (addRipCut) {
                m_cuts.add(new Cut(rootNode, node, demandPiece, node.y + h, node.x, node.x + w, CutDirection.HORIZONTAL));
            }
        }

        if (node.down != null) {
            node.down.rootNode = rootNode;
        }

        if (node.right != null) {
            node.right.rootNode = rootNode;
        }

        return node;
    }

    @Override
    public String toString() {
        return "Packer: [" + "getCutPreference(): "    + getCutPreference()   +
            "], ["         + "getFirstFit(): "         + isFirstFit()         +
            "], ["         + "getPieceOrdering(): "    + getPieceOrdering()   +
            "], ["         + "getStockOrdering(): "    + getStockOrdering()   +
            "], ["         + "getOrderStockByCost(): " + isOrderStockByCost() +
            "], ["         + "m_cost: " + m_cost +
            "]";
    }
}
