package service.dao;

import org.springframework.beans.factory.annotation.Autowired;
import service.dao.cache.VotesCache;
import stockstream.database.PlayerVote;

import java.util.Collections;
import java.util.List;

public class VotesDao {

    @Autowired
    private VotesCache votesCache;

    public List<PlayerVote> getVotesForOrderId(final String orderId) {
        return votesCache.getOrderIdToVotes().getOrDefault(orderId, Collections.emptyList());
    }

    public List<PlayerVote> getVotesForDate(final String date) {
        return votesCache.getDateToVotes().getOrDefault(date, Collections.emptyList());
    }

    public List<PlayerVote> getVotesForPlayer(final String player) {
        return votesCache.getPlayerToVotes().getOrDefault(player, Collections.emptyList());
    }

}
