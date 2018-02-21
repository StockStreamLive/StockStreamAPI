package service.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import service.dao.cache.OrdersCache;
import service.data.OrderStats;
import stockstream.database.RobinhoodOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class OrdersDao {

    @Autowired
    private OrdersCache ordersCache;

    public List<RobinhoodOrder> getOrdersForSymbol(final String symbol) {
        return ordersCache.getSymbolToOrders().getOrDefault(symbol, new ArrayList<>());
    }

    public List<RobinhoodOrder> getOrdersForIds(final Collection<String> ids) {
        final ArrayList<RobinhoodOrder> ordersForIds = new ArrayList<>();

        ids.forEach(id -> ordersForIds.addAll(ordersCache.getIdToOrders().getOrDefault(id, new ArrayList<>())));

        return ordersForIds;
    }

    public List<RobinhoodOrder> getOrdersForDate(final String date) {
        return ordersCache.getDateToOrders().getOrDefault(date, new ArrayList<>());
    }

    public OrderStats getOrderStats() {
        return ordersCache.getOrderStats();
    }
}
