package service.computer;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import service.dao.VotesDao;
import service.dao.cache.WalletCache;
import stockstream.database.PlayerVote;
import stockstream.database.RobinhoodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LiabilityComputer {

    @Autowired
    private VotesDao votesDao;

    @Autowired
    private WalletCache walletCache;

    public Set<String> computeLiablePlayers(final String orderId) {

        if (isWalletOrder(orderId)) {
            return ImmutableSet.of(walletCache.getOrderIdToPlayer().get(orderId));
        }

        final List<PlayerVote> votes = votesDao.getVotesForOrderId(orderId);
        final Set<String> voters = votes.stream().map(PlayerVote::getPlatform_username).collect(Collectors.toSet());

        return voters;
    }

    public boolean isWalletOrder(final String orderId) {
        return walletCache.getOrderIdToPlayer().containsKey(orderId);
    }

    public RobinhoodOrder computeBuyOrderForSell(final List<RobinhoodOrder> buyOrders, final RobinhoodOrder sellOrder) {
        final List<RobinhoodOrder> robinhoodOrdersCopy = new ArrayList<>(buyOrders);
        robinhoodOrdersCopy.sort((r1, r2) -> {
            final Set<String> sellingPlayers = computeLiablePlayers(sellOrder.getId());

            final Set<String> buyingPlayers1 = computeLiablePlayers(r1.getId());
            final Set<String> buyingPlayers2 = computeLiablePlayers(r2.getId());

            final Set<String> overlap1 = SetUtils.intersection(buyingPlayers1, sellingPlayers);
            final Set<String> overlap2 = SetUtils.intersection(buyingPlayers2, sellingPlayers);

            if (overlap1.size() > 0 && overlap2.size() > 0 && overlap1.size() == overlap2.size()) {
                final double overlap1Ownership = overlap1.size() / (double)buyingPlayers1.size();
                final double overlap2Ownership = overlap2.size() / (double)buyingPlayers2.size();
                if (overlap1Ownership > overlap2Ownership) {
                    return -1;
                } else if (overlap1Ownership < overlap2Ownership) {
                    return 1;
                }
            }

            if (overlap1.size() > overlap2.size()) {
                return -1;
            } else if (overlap1.size() < overlap2.size()) {
                return 1;
            }

            return r1.computeRankedTimestamp().compareTo(r2.computeRankedTimestamp());
        });

        return robinhoodOrdersCopy.iterator().next();
    }
}
