package service.data;


import java.util.Comparator;

public class ScoreComparator implements Comparator<Score> {

    private static final int MIN_TRADES = 5;

    public int compare(final Score score1, final Score score2) {

        final int qualifiedTradesComparison = Integer.compare(score2.getQualifiedTrades(), score1.getQualifiedTrades());
        final int decimalReturnComparison = Double.compare(score2.getDecimalReturn(), score1.getDecimalReturn());

        final boolean someTradesBelowMinimum = score1.getQualifiedTrades() < MIN_TRADES || score2.getQualifiedTrades() < MIN_TRADES;

        int comparisonToUse = decimalReturnComparison;

        if (qualifiedTradesComparison != 0 && someTradesBelowMinimum) {
            comparisonToUse = qualifiedTradesComparison;
        }

        return comparisonToUse;
    }

}
