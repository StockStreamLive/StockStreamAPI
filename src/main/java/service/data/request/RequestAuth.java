package service.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestAuth {
    private String channel_id;
    private String client_id;
    private String opaque_user_id;
    private String token;
}
