package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import service.application.Config;
import stockstream.database.ContestEntry;
import stockstream.database.ContestRegistry;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ContestCache implements ReloadingCache {

    @Autowired
    private ContestRegistry contestRegistry;

    @Getter
    private Map<String, ContestEntry> playerIdToEntry = new HashMap<>();

    @Override
    public void reloadCache() {
        final Collection<ContestEntry> contestEntries = contestRegistry.getContestEntries(Config.CONTEST_NAME);
        final Map<String, ContestEntry> playerIdToEntry = contestEntries.stream().collect(Collectors.toMap(ContestEntry::getPlayerId, entry -> entry));

        this.playerIdToEntry = playerIdToEntry;
        log.info("Loaded {} contest registration entries.", playerIdToEntry.size());
    }

    @Override
    public boolean isPrimed() {
        return CollectionUtils.isEmpty(playerIdToEntry);
    }
}
