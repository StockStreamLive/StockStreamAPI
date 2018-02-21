package service.computer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.mutable.MutableFloat;
import org.springframework.beans.factory.annotation.Autowired;
import service.data.Position;
import stockstream.database.RobinhoodOrder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PositionAssembler {

    @Autowired
    private ScoreComputer scoreComputer;

    @Autowired
    private LiabilityComputer liabilityComputer;

    public List<Position> computeAllPositions(final Collection<RobinhoodOrder> forRobinhoodOrders) {
        final Map<String, List<RobinhoodOrder>> ordersBySymbol = new ConcurrentHashMap<>();
        forRobinhoodOrders.forEach(order -> ordersBySymbol.computeIfAbsent(order.getSymbol(), l -> new ArrayList<>()).add(order));

        final List<Position> positions = new ArrayList<>();

        final MutableFloat totalExcessSaleValue = new MutableFloat();

        ordersBySymbol.forEach((key, value) -> {
            final List<RobinhoodOrder> buyOrders = new ArrayList<>();
            assembleSoldOrders(value, buyOrders, positions, totalExcessSaleValue);
            assembleUnsoldOrders(buyOrders, positions);
        });

        log.info("Excess Sale sell orders total value ${}", totalExcessSaleValue.floatValue());

        return positions;
    }

    private void assembleSoldOrders(final List<RobinhoodOrder> inputOrders,
                                    final List<RobinhoodOrder> buyOrders,
                                    final List<Position> positions,
                                    final MutableFloat totalExcessSaleValue) {
        for (final RobinhoodOrder robinhoodOrder : inputOrders) {
            final int quantity = (int) Double.parseDouble(robinhoodOrder.getQuantity());

            if (!"filled".equalsIgnoreCase(robinhoodOrder.getState())) {
                continue;
            }
            if ("buy".equalsIgnoreCase(robinhoodOrder.getSide())) {
                for (int i = 0; i < quantity; ++i) {
                    buyOrders.add(robinhoodOrder);
                }
            } else {
                for (int i = 0; i < quantity; ++i) {
                    if (buyOrders.size() == 0) {
                        log.debug("Excess sale of robinhoodOrder! Free share? {} {}", robinhoodOrder.getSymbol(), robinhoodOrder.getAverage_price());
                        totalExcessSaleValue.add(robinhoodOrder.computeCost());
                        continue;
                    }

                    final RobinhoodOrder buyOrder = liabilityComputer.computeBuyOrderForSell(buyOrders, robinhoodOrder);
                    buyOrders.remove(buyOrder);


                    final Set<String> liablePlayers = liabilityComputer.computeLiablePlayers(buyOrder.getId());

                    final double influence = 1.0d / liablePlayers.size();
                    final boolean isWalletOrder = liabilityComputer.isWalletOrder(buyOrder.getId());

                    final boolean isQualifiedForPromo = scoreComputer.qualifiedForPromotion(buyOrder, robinhoodOrder) && !isWalletOrder;

                    final Position position = new Position(buyOrder, robinhoodOrder, influence, liablePlayers, isWalletOrder, isQualifiedForPromo);
                    positions.add(position);
                }
            }
        }
    }

    private void assembleUnsoldOrders(final List<RobinhoodOrder> buyOrders, final List<Position> positions) {
        while (buyOrders.size() > 0) {
            final RobinhoodOrder buyOrder = buyOrders.get(0);
            buyOrders.remove(buyOrder);

            final Set<String> liablePlayers = liabilityComputer.computeLiablePlayers(buyOrder.getId());

            final double influence = 1.0d / liablePlayers.size();
            final boolean isWalletOrder = liabilityComputer.isWalletOrder(buyOrder.getId());

            final Position position = new Position(buyOrder, null, influence, liablePlayers, isWalletOrder, false);
            positions.add(position);
        }
    }

}
