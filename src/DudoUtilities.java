public class DudoUtilities {
    public static final int NUM_SIDES = 6, NUM_ACTIONS = (2 * NUM_SIDES) + 1,
        DUDO = NUM_ACTIONS - 1;
    public static final int[] claimNum = {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2};
    public static final int[] claimRank = {2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 1};

    public static String claimHistoryToString(boolean[] isClaimed) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < NUM_ACTIONS; a++) {
            if (a == 12 && isClaimed[a]) {
                sb.append(',');
                sb.append("Dudo!");
                return sb.toString();
            } else if (a == 12) {
                return sb.toString();
            }
            if (isClaimed[a]) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(claimNum[a]);
                sb.append('*');
                sb.append(claimRank[a]);
            }
        }
        return sb.toString();
    }

    public static int infoSetToInteger(int playerRoll, boolean[] isClaimed) {
        int infoSetNum = playerRoll;
        for (int a = NUM_ACTIONS - 2; a >= 0; a--) {
            infoSetNum = 2 * infoSetNum + (isClaimed[a] ? 1 : 0);
        }
        return infoSetNum;
    }

    private static int rankCount(int[] dice, int rank) {
        int count = 0;
        for (int i = 0; i < dice.length; i++) {
            if (dice[i] == rank || dice[i] == 1) {
                count ++;
            }
        }
        return count;
    }

    // Utility for challenged
    public static int dudoUtility(int[] dice, boolean[] isClaimed) {
        int claimedNum = 0;
        int claimedRank = 0;
        for (int a = claimNum.length - 1; a >= 0; a--) {
            if (isClaimed[a]) {
                claimedNum = claimNum[a];
                claimedRank = claimRank[a];
                break;
            }
        }

        if (rankCount(dice, claimedRank) > claimedNum) {
            return rankCount(dice, claimedRank) - claimedNum;
        } else if (rankCount(dice, claimedRank) < claimedNum) {
            return - (claimedNum - rankCount(dice, claimedRank));
        } else {
            return 1;
        }
    }

    public static int latestAction(boolean[] isClaimed) {
        for (int a = isClaimed.length - 1; a >= 0; a--) {
            if (isClaimed[a]) {
                return a;
            }
        }
        return -1;
    }

    public static int plays(boolean[] isClaimed) {
        int plays = 0;
        for (int a = 0; a < NUM_ACTIONS; a++) {
            if (isClaimed[a]) {
                plays += 1;
            }
        }
        return plays;
    }
}
