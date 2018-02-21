package service.application.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.computer.*;

@Configuration
public class AssemblerBeans {

    @Bean
    public PositionAssembler positionComputer() {
        return new PositionAssembler();
    }

    @Bean
    public LiabilityComputer liabilityComputer() {
        return new LiabilityComputer();
    }

    @Bean
    public AutoCompleteEngine searchEngine() {
        return new AutoCompleteEngine();
    }

    @Bean
    public WalletAssembler walletAssembler() {
        return new WalletAssembler();
    }

    @Bean
    public ScoreComputer scoreComputer() {
        return new ScoreComputer();
    }

}
