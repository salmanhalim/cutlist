package cutlist.genetic;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import cutlist.CutList.Demand;
import cutlist.CutList.Stock;
import cutlist.packer.Node;
import cutlist.packer.Packer;


public class GeneticBinPacker {
    protected static Random s_rand = new Random();

    public record Gene(int demand, int stock) {};

    /*
    public record Genome(List<Integer> genes) {
        public int getFitnessScore() {
            return 0;
        }
    };
    */

    protected record IndividualStock(Stock stock,    Node singleStock) {};
    protected record IndividualDemand(Demand demand, Node singleDemand) {};

    protected List<Stock>  m_stock;
    protected List<Demand> m_demand;

    protected int    m_numGenomes                    = 100;
    protected double m_crossoverProbability          = 0.75;
    protected int    m_multiCrossverRate             = 5;
    protected double m_individualMutationProbability = 0.25;
    protected int    m_filtrationRate                = 50;
    protected double m_geneMutationProbability;

    protected List<IndividualStock>  m_individualStock;
    protected List<IndividualDemand> m_individualDemand;
    protected List<List<Integer>>    m_genomes;

    protected Packer m_packer;

    public GeneticBinPacker(List<Stock> stock, List<Demand> demand) {
        m_stock  = stock;
        m_demand = demand;
    }

    public double getFitnessScore(List<Integer> genome) {
        Map<IndividualStock, List<IndividualDemand>> demandByStock = new HashMap<>();

        for (int i = 0; i < genome.size(); i++) {
            int                    stockIndex = genome.get(i);
            IndividualStock        stock      = m_individualStock.get(stockIndex);
            List<IndividualDemand> demands    = demandByStock.get(stock);

            if (demands == null) {
                demands = new ArrayList<>();

                demandByStock.put(stock, demands);
            }

            demands.add(m_individualDemand.get(i));
        }

        m_packer = new Packer();

        // SALMAN: Set packer options such as piece rotation, best fit, etc.

        double score = demandByStock.entrySet().stream().mapToDouble(entry -> {
            // System.out.println("%s: %d".formatted(entry.getKey().singleStock().name, entry.getValue().size())); // SALMAN: remove

            m_packer.setStock(List.of(entry.getKey().singleStock));
            m_packer.setDemand(entry.getValue().stream().map(IndividualDemand::singleDemand).toList());

            m_packer.fit();

            // System.out.println("-=-=-=-=-= getFitnessScore " + "[" + "m_packer.getSolutionScore(): " + m_packer.getSolutionScore() + "], [" + "m_packer.getFailed().size(): " + m_packer.getFailed().size() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

            return m_packer.getSolutionScore();
        }).sum();

        return score;
    }

    public void initialize() {
        final int numDemand = m_demand.stream().mapToInt(Demand::quantity).sum();

        m_geneMutationProbability = 1d / numDemand;

        System.out.println("-=-=-=-=-= initialize " + "[" + "numDemand: " + numDemand + "], [" + "m_geneMutationProbability: " + m_geneMutationProbability + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        m_individualStock  = new ArrayList<>();
        m_individualDemand = new ArrayList<>();
        m_genomes          = new ArrayList<>();

        m_stock.stream()
            .forEach(stock -> IntStream.range(1, (stock.quantity() == 0 ? numDemand : stock.quantity()) + 1)
                    .forEach(j -> m_individualStock.add(new IndividualStock(stock,
                                            new Node(stock.label() + " " + j, 0, 0, stock.width(), stock.height(), stock.cost())))));

        m_demand.stream()
            .forEach(demand -> IntStream.range(1, (demand.quantity() == 0 ? 1 : demand.quantity()) + 1)
                    .forEach(j -> m_individualDemand.add(new IndividualDemand(demand,
                                            new Node(demand.label() + " " + j, 0, 0, demand.width(), demand.height())))));

        final int numStock  = m_individualStock.size();

        System.out.println("-=-=-=-=-= initialize " + "[" + "m_stock.size(): " + m_stock.size() + "], [" + "m_demand.size(): " + m_demand.size() + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        System.out.println("-=-=-=-=-= initialize " + "[" + "numStock: " + numStock + "], [" + "numDemand: " + numDemand + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        IntStream.range(0, m_numGenomes).forEach(i -> {
            List<Integer> genes = new ArrayList<>();

            IntStream.range(0, numDemand).forEach(j -> genes.add(s_rand.nextInt(0, numStock)));

            m_genomes.add(genes);
        });
    }

    public void nextGeneration() {
        Map<List<Integer>, Double> scores = new HashMap<>();

        m_genomes.stream().forEach(g -> {
            scores.put(g, getFitnessScore(g));
            // System.out.println("Score: " + getFitnessScore(g));
            String genome = String.join(":", g.stream().map(String::valueOf).toList());
            // System.out.println("-=-=-=-=-= nextGeneration genome: " + (genome) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        });

        /*
        m_genomes.stream().forEach(g -> {
            System.out.println("-=-=-=-=-= nextGeneration scores.get(g): " + (scores.get(g)) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove
        });
        */

        m_genomes.sort((g1, g2) -> Double.compare(scores.get(g1), scores.get(g2)));

        System.out.println("-=-=-=-=-= nextGeneration " + "[" + "scores.get(m_genomes.get(0)): " + scores.get(m_genomes.get(0)) + "], [" + "scores.get(m_genomes.get(m_genomes.size()-1)): " + scores.get(m_genomes.get(m_genomes.size() - 1)) + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        List<List<Integer>> newGenomes = new ArrayList<>();;

        // Weakest (lucky)
        newGenomes.addAll(m_genomes.subList(0, 3));

        // Best (fittest)
        newGenomes.addAll(m_genomes.subList(m_genomes.size() - 3, m_genomes.size()));

        System.out.println("-=-=-=-=-= nextGeneration newGenomes.size(): " + (newGenomes.size()) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        // SALMAN: Binary tournament

        // SALMAN: Crossover

        // SALMAN: Mutation
    }
}
