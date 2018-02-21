package service.application.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import service.application.APIService;
import service.application.LiveCommands;
import service.application.Recaptcha;
import service.computer.ConfirmationEmailer;
import stockstream.spring.*;

@Import({DatabaseBeans.class,
         ComputerBeans.class,
         CommonCacheBeans.class,
         WebGatewayBeans.class,
         LogicBeans.class,
         CacheBeans.class,
         DaoBeans.class,
         AssemblerBeans.class})
@Configuration
public class AppContext {

    @Bean
    public Recaptcha recaptcha() {
        return new Recaptcha();
    }

    @Bean
    public ConfirmationEmailer confirmationEmailer() {
        return new ConfirmationEmailer();
    }

    @Bean
    public LiveCommands liveCommands() {
        return new LiveCommands();
    }

    @Bean
    public APIService sparkServer() {
        return new APIService();
    }

}
