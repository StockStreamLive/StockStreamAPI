package service.application.spring;

import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.dao.cache.*;

@Configuration
public class CacheBeans {

    @Bean
    public VotesCache votesCache() {
        return new VotesCache();
    }

    @Bean
    public OrdersCache ordersCache() {
        return new OrdersCache();
    }

    @Bean
    public PositionsCache positionsCache() {
        return new PositionsCache();
    }

    @Bean
    public PortfolioCache portfolioCache() {
        return new PortfolioCache();
    }

    @Bean
    public AutoCompleteCache autoCompleteCache() {
        return new AutoCompleteCache();
    }

    @Bean
    public WalletCache walletCache() {
        return new WalletCache();
    }

    @Bean
    public ContestCache contestCache() {
        return new ContestCache();
    }

    @Bean
    public ImmutableList<ReloadingCache> reloadingCaches() {
        return ImmutableList.of(portfolioCache(),
                                contestCache(),
                                walletCache(),
                                votesCache(),
                                ordersCache(),
                                positionsCache(),
                                autoCompleteCache());
    }

    @Bean
    public CacheReloader cacheReloader() {
        return new CacheReloader();
    }

}
