package service.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockstream.data.TradeCommand;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeVoteRequest {
    private RequestUser user;
    private String electionId;
    private TradeCommand vote;
}
