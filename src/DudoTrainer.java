import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

public class DudoTrainer {
    // Node class represents an information state
    public class Node {
        // int infoSetCode;
        String infoSetString;
        double[] regretSum = new double[DudoUtilities.NUM_ACTIONS], // regretSum is used to inform strategies using regret matching
            strategy = new double[DudoUtilities.NUM_ACTIONS], // current strategy
            strategySum = new double[DudoUtilities.NUM_ACTIONS]; // sum of all strategies, used to find average strategy

        // get strategy using regret matching and realization weight
        private double[] getStrategy(double realizationWeight) {
            double normalizingSum = 0;
            for (int a = 0; a < DudoUtilities.NUM_ACTIONS; a++) {
                strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
                normalizingSum += strategy[a];
            }
            for (int a = 0; a < DudoUtilities.NUM_ACTIONS; a++) {
                if (normalizingSum > 0) {
                    strategy[a] /= normalizingSum;
                } else {
                    strategy[a] = 1.0 / DudoUtilities.NUM_ACTIONS;
                }
                strategySum[a] += realizationWeight * strategy[a];
            }

            return strategy;
        }

        // get average strategy from strategySum
        public double[] getAverageStrategy() {
            double[] avgStrategy = new double[DudoUtilities.NUM_ACTIONS];
            double normalizingSum = 0;
            for (int a = 0; a < DudoUtilities.NUM_ACTIONS; a++) {
                normalizingSum += strategySum[a];
            }
            for (int a = 0; a < DudoUtilities.NUM_ACTIONS; a++) {
                if (normalizingSum > 0) {
                    avgStrategy[a] = strategySum[a] / normalizingSum;
                } else {
                    avgStrategy[a] = 1.0 / DudoUtilities.NUM_ACTIONS;
                }
            }
            return avgStrategy;
        }

        @Override
        public String toString() {
            return String.format("%4s: %s", infoSetString, Arrays.toString(getAverageStrategy()));
        }
    }

    // runs cfr: simulates game, updates strategies, and returns utility 
    private double cfr(int[][] dice, boolean[] history, double p0, double p1) {
        int plays = DudoUtilities.plays(history);
        int player = plays % 2;
        // return payoff of terminal states
        if (plays > 1) {
            boolean isDudo = history[DudoUtilities.NUM_ACTIONS - 1];
            if (isDudo) {
                return DudoUtilities.dudoUtility(dice[player], history);
            }
        }
        // Get info set node or create if nonexistant
        int playerRoll = 10 * dice[player][0] + dice[player][1];
        int infoSet = DudoUtilities.infoSetToInteger(playerRoll, history);
        Node node = nodeMap.get(infoSet);
        if (node == null) {
            node = new Node();
            // node.infoSetString = dice[player] + DudoUtilities.claimHistoryToString(history);
            nodeMap.put(infoSet, node);
        }

        // Get utilities for each action, gets action with regret matching and loops through each possible action and calls cfr recursively
        double[] strategy = node.getStrategy(player == 0 ? p0 : p1);
        double[] util = new double[DudoUtilities.NUM_ACTIONS]; 
        double nodeUtil = 0;
        int latestAction = DudoUtilities.latestAction(history);
        for (int a = latestAction + 1; a < DudoUtilities.NUM_ACTIONS; a++) {
            boolean[] historyCopy = Arrays.copyOf(history, history.length);
            historyCopy[a] = true;
            boolean[] nextHistory = historyCopy;
            util[a] = player == 0
                ? - cfr(dice, nextHistory, p0 * strategy[a], p1)
                : - cfr(dice, nextHistory, p0, p1 * strategy[a]);
            nodeUtil += strategy[a] * util[a];
        }

        // compute counterfactual regret
        for (int a = 0; a < DudoUtilities.NUM_ACTIONS; a++) {
            double regret = util[a] - nodeUtil;
            node.regretSum[a] += (player == 0 ? p1 : p0) * regret;
            node.strategySum[a] += (player == 0 ? p0 : p1) * regret;
        }

        /*
        boolean isEmpty = true;
        for (Object element : history) {
            if (element != null) {
                isEmpty = false;
                break;
            }
        }

        if (player == 0 && isEmpty == true) {
            utilPlayer1 += nodeUtil;
        }
        */
        return nodeUtil;
    }

    private double simulateGame(int[][] dice, boolean[] history) {
        int plays = DudoUtilities.plays(history);
        int player = plays % 2;
        // return payoff of terminal states
        if (plays > 1) {
            boolean isDudo = history[DudoUtilities.NUM_ACTIONS - 1];
            if (isDudo) {
                return DudoUtilities.dudoUtility(dice[player], history);
            }
        }
        // Get info set node or create if nonexistant
        int playerRoll = 10 * dice[player][0] + dice[player][1];
        int infoSet = DudoUtilities.infoSetToInteger(playerRoll, history);
        Node node = nodeMap.get(infoSet);
        if (node == null) {
            node = new Node();
            // node.infoSetCode = infoSet;
            // node.infoSetString = dice[player] + DudoUtilities.claimHistoryToString(history);
            nodeMap.put(infoSet, node);
        }

        // Get utilities for each action, gets action with regret matching and loops through each possible action and calls cfr recursively
        double[] strategy = node.getAverageStrategy();
        double[] util = new double[DudoUtilities.NUM_ACTIONS]; 
        double nodeUtil = 0;
        int latestAction = DudoUtilities.latestAction(history);
        for (int a = latestAction + 1; a < DudoUtilities.NUM_ACTIONS; a++) {
            boolean[] historyCopy = Arrays.copyOf(history, history.length);
            historyCopy[a] = true;
            boolean[] nextHistory = historyCopy;
            util[a] = player == 0
                ? - simulateGame(dice, nextHistory)
                : - simulateGame(dice, nextHistory);
            nodeUtil += strategy[a] * util[a];
        }

        /*
        for (int a = 0; a < DudoUtilities.NUM_ACTIONS; a++) {
            double regret = util[a] - nodeUtil;
            node.regretSum[a] += (player == 0 ? p1 : p0) * regret;
            node.strategySum[a] += (player == 0 ? p0 : p1) * regret;
        }
        */

        /*
        System.out.println("Breakpoint");
        boolean isEmpty = true;
        for (Object element : history) {
            if (element != null) {
                isEmpty = false;
                break;
            }
        }

        if (player == 0 && isEmpty == true) {
            utilPlayer1 += nodeUtil;
        }
        */
        return nodeUtil;
    }

    public void train(int iterations) {
        double util = 0;
        Random random = new Random();
        for (int i = 0; i < iterations; i++) {
            int[][] rolls = new int[2][2];
            for (int j = 0; j < rolls.length; j++) {
                for (int k = 0; k < rolls[j].length; k++) {
                    rolls[j][k] = 1 + random.nextInt(6);
                }
            }
            boolean[] isClaimed = new boolean[DudoUtilities.NUM_ACTIONS];
            util += cfr(rolls, isClaimed, 1, 1);
        }
        System.out.println("Average game value: " + String.valueOf(util / iterations));
        /*
        for (Node n : nodeMap.values()) {
            System.out.println(n);
        }
        */
    }

    public void simulateGames(int iterations) {
        double util = 0;
        Random random = new Random();
        for (int i = 0; i < iterations; i++) {
            int[][] rolls = new int[2][2];
            for (int j = 0; j < rolls.length; j++) {
                for (int k = 0; k < rolls[j].length; k++) {
                    rolls[j][k] = 1 + random.nextInt(6);
                }
            }
            boolean[] isClaimed = new boolean[DudoUtilities.NUM_ACTIONS];
            util += simulateGame(rolls, isClaimed);
        }
        System.out.println("Expected Utility: " + String.valueOf(util / iterations));
    }

    public TreeMap<Integer, Node> nodeMap = new TreeMap<>();

    

    public static void main(String[] args) {
        int iterations = 100;
        new DudoTrainer().train(iterations);
        new DudoTrainer().simulateGames(iterations);

    }
}
