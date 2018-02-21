package service.computer;

import com.google.common.collect.ImmutableList;
import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage;
import com.microtripit.mandrillapp.lutung.view.MandrillTemplate;
import lombok.extern.slf4j.Slf4j;
import service.application.Config;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

@Slf4j
public class ConfirmationEmailer {

    private MandrillTemplate confirmationTemplate;

    @PostConstruct
    public void init() {
        final MandrillApi mandrillApi = new MandrillApi(Config.MANDRILL_API_KEY);

        try {
            final MandrillTemplate[] templates = mandrillApi.templates().list("confirmation");
            if (templates.length > 0) {
                confirmationTemplate = templates[0];
            }
        } catch (final Exception e) {
            log.warn("Exception pulling mandrill template.", e);
        }
    }

    public void sendConfirmationEmail(final String toEmailAddress, final String twitchUsername) throws Exception {
        final MandrillApi mandrillApi = new MandrillApi(Config.MANDRILL_API_KEY);

        final MandrillMessage confirmationMessage = new MandrillMessage();
        confirmationMessage.setSubject("Confirmation Of StockStream Contest Registration");

        confirmationMessage.setHtml(confirmationTemplate.getCode());

        confirmationMessage.setFromEmail("noreply@stockstream.live");
        confirmationMessage.setFromName("StockStream Bot");

        MandrillMessage.Recipient recipient = new MandrillMessage.Recipient();
        recipient.setEmail(toEmailAddress);

        ArrayList<MandrillMessage.Recipient> recipients = new ArrayList<>(ImmutableList.of(recipient));

        confirmationMessage.setTo(recipients);
        confirmationMessage.setPreserveRecipients(true);

        mandrillApi.messages().send(confirmationMessage, true);
    }

}
