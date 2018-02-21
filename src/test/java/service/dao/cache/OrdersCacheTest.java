package service.dao.cache;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.database.RobinhoodOrder;
import stockstream.database.RobinhoodOrderRegistry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class OrdersCacheTest {

    @Mock
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @InjectMocks
    private OrdersCache ordersCache;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRefreshDataMaps_sampleDataPassed_expectMapsPopulated() {
        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder();
        robinhoodOrder.setId("123");
        robinhoodOrder.setSymbol("AMZN");
        robinhoodOrder.setCreated_at("2017-05-30T21:13:05.939928Z");

        when(robinhoodOrderRegistry.getAllRobinhoodOrders()).thenReturn(ImmutableList.of(robinhoodOrder));

        ordersCache.reloadCache();

        assertEquals(1, ordersCache.getDateToOrders().get("2017-05-30").size());
        assertEquals(1, ordersCache.getIdToOrders().get("123").size());
        assertEquals(1, ordersCache.getSymbolToOrders().get("AMZN").size());
    }

}
