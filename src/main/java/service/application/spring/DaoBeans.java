package service.application.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.dao.*;

@Configuration
public class DaoBeans {

    @Bean
    public VotesDao votesDao() {
        return new VotesDao();
    }

    @Bean
    public OrdersDao ordersDao() {
        return new OrdersDao();
    }

    @Bean
    public PositionsDao positionsDao() {
        return new PositionsDao();
    }

    @Bean
    public PortfolioDao portfolioDao() {
        return new PortfolioDao();
    }

    @Bean
    public WalletDao walletDao() {
        return new WalletDao();
    }

    @Bean
    public ElectionDao electionDao() {
        return new ElectionDao();
    }

    @Bean
    public GameStateDao gameStateDao() {
        return new GameStateDao();
    }

    @Bean
    public ScoreDao scoreDao() {
        return new ScoreDao();
    }

    @Bean
    public ContestDao contestDao() {
        return new ContestDao();
    }
}
