package service.dao.cache;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.database.HistoricalEquityValue;
import stockstream.database.HistoricalEquityValueRegistry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class PortfolioCacheTest {

    @Mock
    private HistoricalEquityValueRegistry historicalEquityValueRegistry;

    @InjectMocks
    private PortfolioCache portfolioCache;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testReloadCache_recordExists_expectRecordCached() {
        final HistoricalEquityValue historicalEquityValue = new HistoricalEquityValue("2017-05-30T21:13:05Z", 100d, 102d);
        when(historicalEquityValueRegistry.getAllHistoricalEquityValues()).thenReturn(ImmutableList.of(historicalEquityValue));

        portfolioCache.reloadCache();

        assertEquals(1, portfolioCache.getIntradayPortfolioValues().size());
        assertEquals(1, portfolioCache.getIntradayPortfolioValues().get("2017-05-30").size());
    }

}
