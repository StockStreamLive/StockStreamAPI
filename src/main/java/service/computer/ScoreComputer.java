package service.computer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import service.application.Config;
import service.dao.cache.ContestCache;
import service.dao.cache.PositionsCache;
import service.data.Position;
import service.data.Score;
import service.data.ScoreComparator;
import stockstream.database.RobinhoodOrder;
import stockstream.util.TimeUtil;

import java.util.*;

public class ScoreComputer {

    @VisibleForTesting
    protected static Date PROMO_START_DATE = new Date(Config.PROMO_SCORING_START);

    @VisibleForTesting
    protected static Date PROMO_END_DATE = new Date(Config.PROMO_SCORING_END);

    private static final int MINIMUM_BUY_PRICE_DOLLARS = 5;

    @Autowired
    private PositionsCache positionsCache;

    @Autowired
    private ContestCache contestCache;

    private static final Set<String> DISQUALIFIED_PLAYERS = ImmutableSet.of("twitch:michrob");


    public List<Score> computeHighScoreList() {
        final List<Position> positions = positionsCache.getAllNonWalletPositions();

        final Map<String, Set<Position>> playerToPositions = new HashMap<>();
        positions.forEach(position -> position.getLiablePlayers().forEach(player -> playerToPositions.computeIfAbsent(player, set -> new HashSet<>()).add(position)));

        DISQUALIFIED_PLAYERS.forEach(playerToPositions::remove);

        final Map<String, Score> playerToScore = new HashMap<>();
        playerToPositions.forEach((key, value) -> {
            final Score score = computeScoreForPositions(key, value);
            boolean isPlayerRegistered = contestCache.getPlayerIdToEntry().keySet().contains(score.getPlayerId());

            if (isPlayerRegistered && score.getQualifiedTrades() > 0) {
                playerToScore.put(key, score);
            }
        });

        final List<Score> playerScores = new ArrayList<>(playerToScore.values());
        playerScores.sort(new ScoreComparator());

        return playerScores;
    }

    public Score computeScoreForPositions(final String playerId, final Set<Position> playerPositions) {

        final MutableInt qualifiedTrades = new MutableInt(0);
        final MutableDouble dollars_spent = new MutableDouble(0);
        final MutableDouble dollars_sold = new MutableDouble(0);

        playerPositions.forEach(position -> {
            if (!position.isQualifiedForPromotion()) {
                return;
            }

            final Double buyOrderPrice = position.getBuyOrder().computeCost();
            final Double sellOrderPrice = position.getSellOrder().computeCost();

            dollars_spent.add(buyOrderPrice * position.getInfluence());
            dollars_sold.add(sellOrderPrice * position.getInfluence());

            qualifiedTrades.increment();
        });

        double dollar_return = qualifiedTrades.intValue() > 0 ? dollars_sold.doubleValue() - dollars_spent.doubleValue() : 0;
        double decimal_return = dollars_spent.doubleValue() > 0 ? dollar_return / dollars_spent.doubleValue() : 0;

        return new Score(playerId, decimal_return, dollar_return, qualifiedTrades.intValue(), dollars_spent.doubleValue(), dollars_sold.doubleValue());
    }


    public boolean qualifiedForPromotion(final RobinhoodOrder buyOrder, final RobinhoodOrder sellOrder) {
        if (buyOrder == null || sellOrder == null) {
            return false;
        }

        final Optional<Date> buyOrderDate = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", buyOrder.getCreated_at(), "UTC");
        final Optional<Date> sellOrderDate = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", sellOrder.getCreated_at(), "UTC");

        final boolean datesValid = buyOrderDate.isPresent() && sellOrderDate.isPresent();
        final boolean withinDates = datesValid && buyOrderDate.get().after(PROMO_START_DATE) && sellOrderDate.get().before(PROMO_END_DATE);
        final boolean withinPrice = buyOrder.computeCost() > MINIMUM_BUY_PRICE_DOLLARS;

        return withinDates && withinPrice;

    }

}
