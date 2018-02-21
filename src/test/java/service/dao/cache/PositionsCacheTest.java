package service.dao.cache;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import service.computer.PositionAssembler;
import service.computer.WalletAssembler;
import service.data.Position;
import stockstream.database.RobinhoodOrder;
import stockstream.database.Wallet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class PositionsCacheTest {

    @Mock
    private VotesCache votesCache;

    @Mock
    private OrdersCache ordersCache;

    @Mock
    private WalletCache walletCache;

    @Mock
    private PositionAssembler positionAssembler;

    @Mock
    private WalletAssembler walletAssembler;

    @InjectMocks
    private PositionsCache positionsCache;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRefreshDataMaps_sampleDataPassed_expectMapsPopulated() {
        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder();
        robinhoodOrder.setId("123");
        robinhoodOrder.setSymbol("AMZN");
        robinhoodOrder.setCreated_at("2017-10-13T21:13:05.939928Z");

        final Position testPosition = new Position(robinhoodOrder, null, .5d, ImmutableSet.of("mike"), false, false);
        when(positionAssembler.computeAllPositions(any())).thenReturn(ImmutableList.of(testPosition));
        when(walletAssembler.constructWalletFromPositions(any(), any())).thenReturn(new Wallet("mike", 5d, .5d, 100));

        positionsCache.reloadCache();

        assertEquals(1, positionsCache.getPlayerToPositions().get("mike").size());
        assertEquals(1, positionsCache.getSymbolToPositions().get("AMZN").size());
    }


}
