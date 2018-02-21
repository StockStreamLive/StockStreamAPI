package service.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {

    private String playerId;

    private double decimalReturn;
    private double dollarReturn;

    private int qualifiedTrades;

    private double dollarsSpent;
    private double dollarsSold;

    public Optional<String> extractUsername() {
        if (StringUtils.isEmpty(playerId)) {
            return Optional.empty();
        }

        final String[] tokens = playerId.split(":");

        if (tokens.length > 0) {
            return Optional.of(tokens[1]);
        }

        return Optional.empty();
    }
}
