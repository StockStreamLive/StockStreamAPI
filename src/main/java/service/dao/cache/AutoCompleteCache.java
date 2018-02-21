package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.springframework.beans.factory.annotation.Autowired;
import service.dao.AutoCompleteItem;
import spark.utils.CollectionUtils;
import stockstream.cache.InstrumentCache;
import stockstream.database.PlayerVote;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AutoCompleteCache implements ReloadingCache {

    @Getter
    private final Map<String, Set<AutoCompleteItem>> keywordToSearchItems = new ConcurrentHashMap<>();

    @Autowired
    private VotesCache votesCache;

    @Autowired
    private OrdersCache ordersCache;

    @Autowired
    private InstrumentCache instrumentCache;

    @Override
    public void reloadCache() {
        final Map<String, List<PlayerVote>> playerToVotes = votesCache.getPlayerToVotes();

        instrumentCache.getSymbolToInstrument().values().forEach(instrument -> {
            final int rank = ordersCache.getSymbolToOrders().getOrDefault(instrument.getSymbol(), Collections.emptyList()).size();
            final AutoCompleteItem autoCompleteItem = new AutoCompleteItem(instrument.getSymbol(), instrument.getName(), String.format("/symbol/%s", instrument.getSymbol()), 1000000 + rank);
            keywordToSearchItems.computeIfAbsent(instrument.getSymbol().toLowerCase(), set -> new ConcurrentHashSet<>()).add(autoCompleteItem);
            keywordToSearchItems.computeIfAbsent(instrument.getName().toLowerCase(), set -> new ConcurrentHashSet<>()).add(autoCompleteItem);
        });

        playerToVotes.forEach((player, votes) -> {
            final String nick = player.split(":")[1];
            final AutoCompleteItem autoCompleteItem = new AutoCompleteItem(nick, "", String.format("/player/%s", player), votes.size());
            keywordToSearchItems.computeIfAbsent(nick.toLowerCase(), set -> new ConcurrentHashSet<>()).add(autoCompleteItem);
        });

        log.info("Created {} search keywords", keywordToSearchItems.size());
    }

    @Override
    public boolean isPrimed() {
        return !CollectionUtils.isEmpty(keywordToSearchItems.values());
    }
}
