package service.dao;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import service.application.Config;
import service.application.Recaptcha;
import service.computer.ConfirmationEmailer;
import service.dao.cache.ContestCache;
import service.data.RegistrationStatus;
import stockstream.database.ContestEntry;
import stockstream.database.ContestRegistry;

@Slf4j
public class ContestDao {

    @Autowired
    private ConfirmationEmailer confirmationEmailer;

    @Autowired
    private Recaptcha recaptcha;

    @Autowired
    private ContestRegistry contestRegistry;

    @Autowired
    private ContestCache contestCache;

    public boolean isPlayerRegistered(final String playerId) {
        return contestCache.getPlayerIdToEntry().keySet().contains(playerId);
    }

    private ContestEntry constructEntry(final JSONObject jsonObject) {
        final String emailAddress = jsonObject.getString("email_address").toLowerCase();
        final String twitchUsername = jsonObject.getString("username").toLowerCase();
        final String platform = jsonObject.getString("platform");

        final ContestEntry contestEntry = new ContestEntry();
        contestEntry.setContestName(Config.CONTEST_NAME);
        contestEntry.setEmailAddress(emailAddress);
        contestEntry.setUsername(twitchUsername);
        contestEntry.setPlatform(platform);
        contestEntry.setZipCode(jsonObject.getString("zip_code"));

        return contestEntry;
    }

    public RegistrationStatus processRegistration(final JSONObject jsonObject) {
        final String recaptchaResponse = jsonObject.getString("g_recaptcha_response");

        final boolean captchaVarified = recaptcha.captchIsOk(recaptchaResponse);
        if (!captchaVarified) {
            return RegistrationStatus.BAD_CAPTCHA;
        }

        final ContestEntry contestEntry = constructEntry(jsonObject);

        if (isPlayerRegistered(contestEntry.getPlayerId())) {
            return RegistrationStatus.ALREADY_REGISTERED;
        }

        RegistrationStatus registrationStatus = RegistrationStatus.OK;

        try {
            contestRegistry.saveContestEntry(contestEntry);
        } catch (final Exception exception) {
            log.warn("Exception processing registration {}", jsonObject.toString(), exception);
            registrationStatus = RegistrationStatus.SERVER_ERROR;
        }

        try {
            confirmationEmailer.sendConfirmationEmail(contestEntry.getEmailAddress(), contestEntry.getUsername());
        } catch (final Exception ex) {
            log.warn("Exception sending confirmation to {}", contestEntry.getEmailAddress(), ex);
            registrationStatus = RegistrationStatus.SERVER_ERROR;
        }

        return registrationStatus;
    }

}