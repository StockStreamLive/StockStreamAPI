package service.computer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import service.dao.cache.ContestCache;
import service.dao.cache.PositionsCache;
import service.data.Position;
import service.data.Score;
import stockstream.database.ContestEntry;
import stockstream.database.RobinhoodOrder;
import stockstream.util.TimeUtil;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

public class ScoreComputerTest {

    @Mock
    private PositionsCache positionsCache;

    @Mock
    private ContestCache contestCache;

    @InjectMocks
    private ScoreComputer scoreComputer;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testComputeScoreForPositions_noPositions_expectEmptyScore() {
        final Score score = scoreComputer.computeScoreForPositions("player1", ImmutableSet.of());

        assertEquals(0, score.getDecimalReturn(), .001);
        assertEquals(0, score.getQualifiedTrades(), .001);
        assertEquals(0, score.getDollarsSpent(), .001);
        assertEquals(0, score.getDollarsSold(), .001);
    }

    @Test
    public void testComputeScoreForPositions_onePosition100PercentReturn_expectAccurateScore() {
        final RobinhoodOrder buyOrder = new RobinhoodOrder();
        buyOrder.setId("123");
        buyOrder.setSymbol("AMZN");
        buyOrder.setCreated_at("2018-01-01T21:13:05.939928Z");
        buyOrder.setAverage_price("10");


        final RobinhoodOrder sellOrder = new RobinhoodOrder();
        sellOrder.setId("123");
        sellOrder.setSymbol("AMZN");
        sellOrder.setCreated_at("2018-01-01T21:13:05.939928Z");
        sellOrder.setAverage_price("20");

        final Position position = new Position(buyOrder, sellOrder, 1, ImmutableSet.of(), false, true);

        final Score score = scoreComputer.computeScoreForPositions("player1", ImmutableSet.of(position));

        assertEquals(1, score.getDecimalReturn(), .001);
        assertEquals(1, score.getQualifiedTrades(), .001);
        assertEquals(10, score.getDollarsSpent(), .001);
        assertEquals(20, score.getDollarsSold(), .001);
    }

    @Test
    public void testComputeScoreForPositions_positionBeforeScoringStart_expectEmptyScore() {
        final RobinhoodOrder buyOrder = new RobinhoodOrder();
        buyOrder.setId("123");
        buyOrder.setSymbol("AMZN");
        buyOrder.setCreated_at("2016-01-01T21:13:05.939928Z");
        buyOrder.setAverage_price("10");


        final RobinhoodOrder sellOrder = new RobinhoodOrder();
        sellOrder.setId("123");
        sellOrder.setSymbol("AMZN");
        sellOrder.setCreated_at("2016-01-01T21:13:05.939928Z");
        sellOrder.setAverage_price("20");

        final Position position = new Position(buyOrder, sellOrder, 1, ImmutableSet.of(), false, false);

        final Score score = scoreComputer.computeScoreForPositions("player1", ImmutableSet.of(position));

        assertEquals(0, score.getDecimalReturn(), .001);
        assertEquals(0, score.getQualifiedTrades(), .001);
        assertEquals(0, score.getDollarsSpent(), .001);
        assertEquals(0, score.getDollarsSold(), .001);
    }

    @Test
    public void testComputeHighScoreList_twoPositions_expectTwoScores() {
        final RobinhoodOrder buyOrder1 = new RobinhoodOrder();
        buyOrder1.setId("123");
        buyOrder1.setSymbol("AMZN");
        buyOrder1.setCreated_at("2025-01-01T21:13:05.939928Z");
        buyOrder1.setAverage_price("10");

        final RobinhoodOrder buyOrder2 = new RobinhoodOrder();
        buyOrder2.setId("124");
        buyOrder2.setSymbol("AMZN");
        buyOrder2.setCreated_at("2025-01-01T21:13:05.939928Z");
        buyOrder2.setAverage_price("9");


        final RobinhoodOrder sellOrder1 = new RobinhoodOrder();
        sellOrder1.setId("125");
        sellOrder1.setSymbol("AMZN");
        sellOrder1.setCreated_at("2025-01-01T21:13:05.939928Z");
        sellOrder1.setAverage_price("20");

        final RobinhoodOrder sellOrder2 = new RobinhoodOrder();
        sellOrder2.setId("126");
        sellOrder2.setSymbol("AMZN");
        sellOrder2.setCreated_at("2025-01-01T21:13:05.939928Z");
        sellOrder2.setAverage_price("20");


        final Position position1 = new Position(buyOrder1, sellOrder1, 1, ImmutableSet.of("player1"), false, true);
        final Position position2 = new Position(buyOrder2, sellOrder2, 1, ImmutableSet.of("player2"), false, true);

        when(positionsCache.getAllNonWalletPositions()).thenReturn(ImmutableList.of(position1, position2));
        when(contestCache.getPlayerIdToEntry()).thenReturn(ImmutableMap.of("player1", new ContestEntry(),
                                                                           "player2", new ContestEntry()));

        final List<Score> scores = scoreComputer.computeHighScoreList();

        assertEquals(2, scores.size());
        assertEquals("player2", scores.get(0).getPlayerId());
        assertEquals("player1", scores.get(1).getPlayerId());
    }

    @Test
    public void testQualifiedForPromotion_nullValues_expectFalse() {
        final boolean qualified = scoreComputer.qualifiedForPromotion(null, null);

        assertFalse(qualified);
    }

    @Test
    public void testQualifiedForPromotion_malformedDates_expectFalse() {
        final RobinhoodOrder buyOrder1 = new RobinhoodOrder();
        buyOrder1.setId("123");
        buyOrder1.setSymbol("AMZN");
        buyOrder1.setCreated_at("bad_date");
        buyOrder1.setAverage_price("10");

        final RobinhoodOrder sellOrder1 = new RobinhoodOrder();
        sellOrder1.setId("125");
        sellOrder1.setSymbol("AMZN");
        sellOrder1.setCreated_at("bad_date");
        sellOrder1.setAverage_price("20");

        final boolean qualified = scoreComputer.qualifiedForPromotion(buyOrder1, sellOrder1);

        assertFalse(qualified);
    }

    @Test
    public void testQualifiedForPromotion_valueTooLow_expectFalse() {
        scoreComputer.PROMO_START_DATE = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", "2017-01-01T21:13:05.939928Z", "UTC").get();
        scoreComputer.PROMO_END_DATE = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", "2017-01-04T21:13:05.939928Z", "UTC").get();


        final RobinhoodOrder buyOrder1 = new RobinhoodOrder();
        buyOrder1.setId("123");
        buyOrder1.setSymbol("AMZN");
        buyOrder1.setCreated_at("2017-01-02T21:13:05.939928Z");
        buyOrder1.setAverage_price("1");

        final RobinhoodOrder sellOrder1 = new RobinhoodOrder();
        sellOrder1.setId("125");
        sellOrder1.setSymbol("AMZN");
        sellOrder1.setCreated_at("2017-01-03T21:13:05.939928Z");
        sellOrder1.setAverage_price("2");

        final boolean qualified = scoreComputer.qualifiedForPromotion(buyOrder1, sellOrder1);

        assertFalse(qualified);
    }

    @Test
    public void testQualifiedForPromotion_validDates_expectTrue() {
        scoreComputer.PROMO_START_DATE = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", "2017-01-01T21:13:05.939928Z", "UTC").get();
        scoreComputer.PROMO_END_DATE = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", "2017-01-04T21:13:05.939928Z", "UTC").get();

        final RobinhoodOrder buyOrder1 = new RobinhoodOrder();
        buyOrder1.setId("123");
        buyOrder1.setSymbol("AMZN");
        buyOrder1.setCreated_at("2017-01-02T21:13:05.939928Z");
        buyOrder1.setAverage_price("6");

        final RobinhoodOrder sellOrder1 = new RobinhoodOrder();
        sellOrder1.setId("125");
        sellOrder1.setSymbol("AMZN");
        sellOrder1.setCreated_at("2017-01-03T21:13:05.939928Z");
        sellOrder1.setAverage_price("7");

        final boolean qualified = scoreComputer.qualifiedForPromotion(buyOrder1, sellOrder1);

        assertTrue(qualified);
    }
}
