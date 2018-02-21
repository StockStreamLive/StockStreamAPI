package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import service.data.OrderStats;
import spark.utils.CollectionUtils;
import stockstream.database.RobinhoodOrder;
import stockstream.database.RobinhoodOrderRegistry;
import stockstream.util.TimeUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class OrdersCache implements ReloadingCache {

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Getter
    private ConcurrentHashMap<String, List<RobinhoodOrder>> idToOrders = new ConcurrentHashMap<>();

    @Getter
    private ConcurrentHashMap<String, List<RobinhoodOrder>> dateToOrders = new ConcurrentHashMap<>();

    @Getter
    private ConcurrentHashMap<String, List<RobinhoodOrder>> symbolToOrders = new ConcurrentHashMap<>();

    @Getter
    private OrderStats orderStats;

    @Getter
    private List<RobinhoodOrder> sortedRobinhoodOrders = new ArrayList<>();

    @Override
    public void reloadCache() {
        final List<RobinhoodOrder> allRobinhoodOrders = new ArrayList<>(robinhoodOrderRegistry.getAllRobinhoodOrders());

        allRobinhoodOrders.sort((o1, o2) -> {
            final String order1Timestamp = o1.computeRankedTimestamp();
            final String order2Timestamp = o2.computeRankedTimestamp();
            return order1Timestamp.compareTo(order2Timestamp);
        });

        double totalBought = 0;
        double totalSold = 0;
        int totalBoughtShares = 0;
        int totalSoldShares = 0;
        for (final RobinhoodOrder robinhoodOrder : allRobinhoodOrders) {
            if (!"filled".equalsIgnoreCase(robinhoodOrder.getState())) {
                continue;
            }

            final int shares = (int) Double.parseDouble(robinhoodOrder.getQuantity());
            final double price = robinhoodOrder.computeCost();

            if (robinhoodOrder.getSide().equalsIgnoreCase("buy")) {
                totalBought += price * shares;
                totalBoughtShares += shares;
            } else {
                totalSold += price * shares;
                totalSoldShares += shares;
            }
        }

        log.info("Reloaded {} orders", allRobinhoodOrders.size());

        ConcurrentHashMap<String, List<RobinhoodOrder>> idStrToOrders = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<RobinhoodOrder>> dateStrToOrders = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<RobinhoodOrder>> symbolStrToOrders = new ConcurrentHashMap<>();

        allRobinhoodOrders.forEach(order -> {
            final Optional<Date> orderDate = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss", order.getCreated_at(), "UTC");
            if (!orderDate.isPresent()) {
                log.error("Order with bad date! {}", order);
                return;
            }
            final String dateStr = TimeUtil.getCanonicalYMDString(orderDate.get());

            idStrToOrders.computeIfAbsent(order.getId(), votes -> new ArrayList<>()).add(order);
            dateStrToOrders.computeIfAbsent(dateStr, votes -> new ArrayList<>()).add(order);
            symbolStrToOrders.computeIfAbsent(order.getSymbol(), votes -> new ArrayList<>()).add(order);
        });

        idToOrders = idStrToOrders;
        dateToOrders = dateStrToOrders;
        symbolToOrders = symbolStrToOrders;
        sortedRobinhoodOrders = allRobinhoodOrders;

        orderStats = new OrderStats(totalBoughtShares, totalSoldShares, totalBought, totalSold);
    }

    @Override
    public boolean isPrimed() {
        return !CollectionUtils.isEmpty(sortedRobinhoodOrders);
    }

}
