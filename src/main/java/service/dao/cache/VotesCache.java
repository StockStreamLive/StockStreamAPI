package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;
import stockstream.database.PlayerVote;
import stockstream.database.PlayerVoteRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class VotesCache implements ReloadingCache {

    @Autowired
    private PlayerVoteRegistry playerVoteRegistry;

    @Getter
    private Map<String, List<PlayerVote>> orderIdToVotes = new ConcurrentHashMap<>();

    @Getter
    private Map<String, List<PlayerVote>> dateToVotes = new ConcurrentHashMap<>();

    @Getter
    private Map<String, List<PlayerVote>> playerToVotes = new ConcurrentHashMap<>();

    @Override
    public void reloadCache() {
        final Collection<PlayerVote> allVotes = playerVoteRegistry.getAllPlayerVotes();

        log.info("Got {} votes from dynamoDB", allVotes.size());

        ConcurrentHashMap<String, List<PlayerVote>> idStrToVotes = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<PlayerVote>> dateStrToVotes = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<PlayerVote>> playerStrToVotes = new ConcurrentHashMap<>();

        allVotes.forEach(vote -> {
            if (!StringUtils.isEmpty(vote.getOrder_id())) {
                idStrToVotes.computeIfAbsent(vote.getOrder_id(), votes -> new ArrayList<>()).add(vote);
            }
            dateStrToVotes.computeIfAbsent(vote.getDate(), votes -> new ArrayList<>()).add(vote);
            playerStrToVotes.computeIfAbsent(vote.getPlatform_username(), votes -> new ArrayList<>()).add(vote);
        });

        orderIdToVotes = idStrToVotes;
        dateToVotes = dateStrToVotes;
        playerToVotes = playerStrToVotes;
    }

    @Override
    public boolean isPrimed() {
        return !CollectionUtils.isEmpty(playerToVotes.values());
    }
}
