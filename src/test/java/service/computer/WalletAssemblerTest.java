package service.computer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import service.data.Position;
import stockstream.database.RobinhoodOrder;
import stockstream.database.Wallet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class WalletAssemblerTest {

    @InjectMocks
    private WalletAssembler walletAssembler;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testComputeRealizedDollarReturn_noPositions_expectZero() {
        final Wallet wallet = walletAssembler.constructWalletFromPositions("p1", new ArrayList<>());
        assertEquals(0, wallet.getRealizedReturn(), .0001);
    }

    @Test
    public void testComputeRealizedDollarReturn_onePosition_expectCorrectReturn() {
        RobinhoodOrder buyOrder = new RobinhoodOrder();
        buyOrder.setPrice("100");

        RobinhoodOrder sellOrder = new RobinhoodOrder();
        sellOrder.setPrice("105");

        Position position = new Position(buyOrder, sellOrder, 1, new HashSet<>(), false, false);

        final Wallet wallet = walletAssembler.constructWalletFromPositions("twitch:michrob", ImmutableList.of(position));

        assertEquals(5, wallet.getRealizedReturn(), .0001);
    }

    @Test
    public void testComputeUnrealizedDollarsSpent_onePositionPartialInfluence_expectCorrectReturn() {
        RobinhoodOrder buyOrder = new RobinhoodOrder();
        buyOrder.setPrice("100");

        Position position = new Position(buyOrder, null, .5, new HashSet<>(), false, false);

        final Wallet wallet = walletAssembler.constructWalletFromPositions("twitch:michrob", ImmutableList.of(position));

        assertEquals(50, wallet.getUnrealizedDollarsSpent(), .0001);
    }

    @Test
    public void testComputeRankedPlayers_twoPlayers_expectSorted() {
        final Map<String, Wallet> playerToReturn = ImmutableMap.of("p1", new Wallet("p1", 1.5d, .5d, 100),
                                                                   "p2", new Wallet("p2", 2d, .5d, 100),
                                                                   "p3", new Wallet("p3", 3d, .5d, 100));

        final List<String> ranked = walletAssembler.computeRankedPlayers(playerToReturn);

        assertEquals("p3", ranked.get(0));
        assertEquals("p2", ranked.get(1));
        assertEquals("p1", ranked.get(2));
    }

    @Test
    public void testComputePlayerToRealizedReturn_playerWithPositions_expectReturn() {
        RobinhoodOrder buyOrder = new RobinhoodOrder();
        buyOrder.setPrice("100.0");

        RobinhoodOrder sellOrder = new RobinhoodOrder();
        sellOrder.setPrice("105");

        Position position = new Position(buyOrder, sellOrder, 1, new HashSet<>(), false, false);

        Map<String, Wallet> playerToWallet = walletAssembler.computePlayerToWallet(ImmutableMap.of("p1", ImmutableSet.of(position)));

        assertEquals(5d, playerToWallet.get("p1").getRealizedReturn(), .001);
    }

}
