package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import service.computer.PositionAssembler;
import service.computer.WalletAssembler;
import service.data.Position;
import spark.utils.CollectionUtils;
import stockstream.database.RobinhoodOrder;
import stockstream.database.Wallet;
import stockstream.database.WalletOrder;
import stockstream.util.TimeUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class PositionsCache implements ReloadingCache {

    @Autowired
    private OrdersCache ordersCache;

    @Autowired
    private WalletCache walletCache;

    @Autowired
    private PositionAssembler positionAssembler;

    @Autowired
    private WalletAssembler walletAssembler;

    @Getter
    private ConcurrentHashMap<String, Set<Position>> playerToPositions = new ConcurrentHashMap<>();

    @Getter
    private ConcurrentHashMap<String, Set<Position>> symbolToPositions = new ConcurrentHashMap<>();

    @Getter
    private ConcurrentHashMap<String, Set<Position>> dateToPositions = new ConcurrentHashMap<>();

    @Getter
    private List<Position> openPositions = new ArrayList<>();

    @Getter
    private List<Position> allNonWalletPositions = new ArrayList<>();

    @Override
    public void reloadCache() {
        final List<RobinhoodOrder> allRobinhoodOrders = ordersCache.getSortedRobinhoodOrders();

        reloadPositionInformation(allRobinhoodOrders);

        final Map<String, Wallet> playerToWallet = walletAssembler.computePlayerToWallet(this.playerToPositions);
        walletCache.updateWallets(playerToWallet);
    }

    private void reloadPositionInformation(final List<RobinhoodOrder> allRobinhoodOrders) {

        final List<RobinhoodOrder> nonWalletOrders = allRobinhoodOrders.stream()
                                                                       .filter(order -> !walletCache.getOrderIdToPlayer().containsKey(order.getId()))
                                                                       .collect(Collectors.toList());

        final List<Position> allPositions = positionAssembler.computeAllPositions(nonWalletOrders);

        allNonWalletPositions = new ArrayList<>(allPositions);

        walletCache.getPlayerToWalletOrders().forEach((key, value) -> {
            final List<String> walletOrderIds = value.stream().map(WalletOrder::getId).collect(Collectors.toList());
            final List<RobinhoodOrder> walletRobinhoodOrders = new ArrayList<>();
            walletOrderIds.forEach(orderId -> walletRobinhoodOrders.addAll(ordersCache.getIdToOrders().getOrDefault(orderId, Collections.emptyList())));
            final List<Position> positionsFromWallet = positionAssembler.computeAllPositions(walletRobinhoodOrders);
            allPositions.addAll(positionsFromWallet);
        });

        final ConcurrentHashMap<String, Set<Position>> playerStrToPositions = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Set<Position>> symbolStrToPosition = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Set<Position>> dateStrToPosition = new ConcurrentHashMap<>();
        final List<Position> openPositions = new ArrayList<>();

        loadPositionMaps(allPositions, playerStrToPositions, symbolStrToPosition, dateStrToPosition, openPositions);

        this.playerToPositions = playerStrToPositions;
        this.symbolToPositions = symbolStrToPosition;
        this.dateToPositions = dateStrToPosition;
        this.openPositions = openPositions;
    }

    private void loadPositionMaps(final List<Position> positions,
                                  final ConcurrentHashMap<String, Set<Position>> playerStrToPositions,
                                  final ConcurrentHashMap<String, Set<Position>> symbolStrToPosition,
                                  final ConcurrentHashMap<String, Set<Position>> dateStrToPosition,
                                  final List<Position> openPositions) {
        positions.forEach(position -> {
            for (final String player : position.getLiablePlayers()) {
                playerStrToPositions.computeIfAbsent(player, l -> new HashSet<>()).add(position);
            }

            final Optional<String> buyOrderDate = TimeUtil.getCanonicalYMDString("yyyy-MM-dd'T'HH:mm:ss", position.getBuyOrder().getCreated_at(), "UTC");
            buyOrderDate.ifPresent(s -> dateStrToPosition.computeIfAbsent(s, l -> new HashSet<>()).add(position));

            if (position.getSellOrder() != null) {
                final Optional<String> sellOrderDate = TimeUtil.getCanonicalYMDString("yyyy-MM-dd'T'HH:mm:ss", position.getSellOrder().getCreated_at(), "UTC");
                sellOrderDate.ifPresent(s -> dateStrToPosition.computeIfAbsent(s, l -> new HashSet<>()).add(position));
            } else {
                openPositions.add(position);
            }

            symbolStrToPosition.computeIfAbsent(position.getBuyOrder().getSymbol(), l -> new HashSet<>()).add(position);
        });

        log.info("Got {} players to positions", playerStrToPositions.size());
        log.info("Got {} symbols to positions", symbolStrToPosition.size());
    }

    @Override
    public boolean isPrimed() {
        return !CollectionUtils.isEmpty(dateToPositions.values());
    }
}
