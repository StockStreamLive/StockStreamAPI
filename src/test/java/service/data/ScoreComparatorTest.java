package service.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreComparatorTest {


    private ScoreComparator scoreComparator;

    @Before
    public void setupTest() {
        scoreComparator = new ScoreComparator();
    }

    @Test
    public void testCompare_aboveMinTrades_expectDecimalComparison() {
        final Score score1 = new Score("player1", .5, 5, 50, 100, 105);
        final Score score2 = new Score("player2", .2, 4, 40, 90, 94);

        final int comparison = scoreComparator.compare(score1, score2);

        assertEquals(-1, comparison);
    }

    @Test
    public void testCompare_belowMinTrades_expectTradeComparison() {
        final Score score1 = new Score("player1", .5, 5, 1, 100, 105);
        final Score score2 = new Score("player2", .2, 4, 2, 90, 94);

        final int comparison = scoreComparator.compare(score1, score2);

        assertEquals(1, comparison);
    }

    @Test
    public void testCompare_equalBelowMinTrades_expectDecimalComparison() {
        final Score score1 = new Score("player1", .5, 5, 1, 100, 105);
        final Score score2 = new Score("player2", .2, 4, 1, 90, 94);

        final int comparison = scoreComparator.compare(score1, score2);

        assertEquals(-1, comparison);
    }
}
