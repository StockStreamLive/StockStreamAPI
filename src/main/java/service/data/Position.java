package service.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import stockstream.database.RobinhoodOrder;

import java.util.Set;

@Data
@AllArgsConstructor
public class Position {

    private final RobinhoodOrder buyOrder;

    private final RobinhoodOrder sellOrder;
    private final double influence;

    private final Set<String> liablePlayers;

    private final boolean isWalletOrder;

    private final boolean qualifiedForPromotion;
}
