package service.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import service.dao.cache.PositionsCache;
import service.data.Position;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
public class PositionsDao {

    @Autowired
    private PositionsCache positionsCache;

    public Set<Position> getPositionsForPlayer(final String player) {
        return positionsCache.getPlayerToPositions().getOrDefault(player, Collections.emptySet());
    }

    public Set<Position> getPositionsForDate(final String date) {
        return positionsCache.getDateToPositions().getOrDefault(date, Collections.emptySet());
    }

    public Set<Position> getPositionsForSymbol(final String symbol) {
        return positionsCache.getSymbolToPositions().getOrDefault(symbol, Collections.emptySet());
    }

    public List<Position> getOpenPositions() {
        return positionsCache.getOpenPositions();
    }

}
