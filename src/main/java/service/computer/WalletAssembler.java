package service.computer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.math3.util.Precision;
import service.data.Position;
import stockstream.database.Wallet;

import java.util.*;

@Slf4j
public class WalletAssembler {

    public Map<String, Wallet> computePlayerToWallet(final Map<String, Set<Position>> playerStrToPositions) {
        final Map<String, Wallet> playerToWallet = new HashMap<>();

        final MutableDouble totalReturn = new MutableDouble();
        final MutableDouble positiveReturn = new MutableDouble();
        final MutableDouble negativeReturn = new MutableDouble();

        playerStrToPositions.forEach((key, value) -> {
            final Wallet wallet = constructWalletFromPositions(key, value);
            playerToWallet.put(key, wallet);

            totalReturn.add(wallet.getRealizedReturn());
            if (wallet.getRealizedReturn() > 0) {
                positiveReturn.add(wallet.getRealizedReturn());
            } else {
                negativeReturn.add(wallet.getRealizedReturn());
            }
        });

        log.info("totalReturn={} positiveReturn={} negativeReturn={}", totalReturn, positiveReturn, negativeReturn);

        return playerToWallet;
    }

    public Wallet constructWalletFromPositions(final String platform_username, final Collection<Position> positions) {
        double realizedDollarReturn = 0;

        double totalRealizedSpent = 0;
        double totalUnrealizedSpent = 0;

        for (final Position position : positions) {
            if (position.getSellOrder() == null) {
                final Double buyOrderPrice = position.getBuyOrder().computeCost();
                totalUnrealizedSpent += buyOrderPrice * position.getInfluence();
            } else {
                final Double buyOrderPrice = position.getBuyOrder().computeCost();
                final Double sellOrderPrice = position.getSellOrder().computeCost();

                totalRealizedSpent += buyOrderPrice * position.getInfluence();

                final double realizedReturn = (sellOrderPrice - buyOrderPrice) * position.getInfluence();
                realizedDollarReturn += realizedReturn;
            }
        }

        if (totalRealizedSpent == 0) {
            totalRealizedSpent = 1;
        }

        final double realizedDecimalReturn = realizedDollarReturn / totalRealizedSpent;

        return new Wallet(platform_username,
                          Precision.round(realizedDollarReturn, 4),
                          Precision.round(realizedDecimalReturn, 4),
                          Precision.round(totalUnrealizedSpent, 4));
    }


    public List<String> computeRankedPlayers(final Map<String, Wallet> playerToWallet) {
        final List<String> rankedPlayers = new ArrayList<>(playerToWallet.keySet());

        rankedPlayers.sort(new Comparator<String>() {
            @Override
            public int compare(final String p1, String p2) {
                return computePlayerScore(p2).compareTo(computePlayerScore(p1));
            }

            private Double computePlayerScore(final String player) {
                final Double playerReturnDollars = playerToWallet.get(player).getRealizedReturn();
                final Double playerReturnPercent = playerToWallet.get(player).getRealizedDecimalReturn();

                if (playerReturnDollars < 0) {
                    return playerReturnDollars;
                }

                return (playerReturnDollars * playerReturnDollars) * Math.abs(playerReturnPercent / 100);
            }
        });

        return rankedPlayers;
    }


}
