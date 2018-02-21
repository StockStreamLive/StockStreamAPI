package service.application;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.http.HTTPClient;
import stockstream.http.HTTPQuery;
import stockstream.http.HTTPResult;

import java.util.Map;
import java.util.Optional;

public class Recaptcha {

    private static final String ENDPOINT = "https://www.google.com/recaptcha/api/siteverify";

    @Autowired
    private HTTPClient httpClient;

    public boolean captchIsOk(final String g_Recaptcha_Response) {
        final Map<String, String> params = ImmutableMap.of("secret", Config.RECAPTCHA_SECRET, "response", g_Recaptcha_Response);
        final HTTPQuery httpQuery = new HTTPQuery(ENDPOINT, params, ImmutableMap.of());

        final Optional<HTTPResult> httpResult = httpClient.executeHTTPPostRequest(httpQuery);

        if (!httpResult.isPresent()) {
            return false;
        }

        final JSONObject response = new JSONObject(httpResult.get().getBody());
        return response.getBoolean("success");
    }

}
