package service.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeedVoteRequest {
    private RequestUser user;
    private String electionId;
    private Object vote;
}
