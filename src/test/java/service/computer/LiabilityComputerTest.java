package service.computer;

import com.cheddar.robinhood.data.Execution;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import service.dao.VotesDao;
import service.dao.cache.WalletCache;
import stockstream.database.PlayerVote;
import stockstream.database.RobinhoodOrder;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class LiabilityComputerTest {

    @Mock
    private VotesDao votesDao;

    @Mock
    private WalletCache walletCache;

    @InjectMocks
    private LiabilityComputer liabilityComputer;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testComputeLiablePlayers_notWalletOrder_expectLiableVotes() {
        when(votesDao.getVotesForOrderId(any())).thenReturn(ImmutableList.of(createPlayerVote("twitch:michrob", "123")));

        final Set<String> liablePlayers = liabilityComputer.computeLiablePlayers("123");

        assertEquals(1, liablePlayers.size());
        assertEquals("twitch:michrob", liablePlayers.iterator().next());
    }

    @Test
    public void testComputeLiablePlayers_walletOrder_expectLiableVotes() {
        when(walletCache.getOrderIdToPlayer()).thenReturn(ImmutableMap.of("123", "twitch:michrob"));

        final Set<String> liablePlayers = liabilityComputer.computeLiablePlayers("123");

        assertEquals(1, liablePlayers.size());
        assertEquals("twitch:michrob", liablePlayers.iterator().next());
    }

    @Test
    public void testComputeBuyOrderForSell_playerDoesntOwnAny_expectFIFO() {
        when(walletCache.getOrderIdToPlayer()).thenReturn(ImmutableMap.of("123", "twitch:michrob"));

        final RobinhoodOrder buyOrder1 = createRobinhoodOrder("buy", "MAGA", "buy_order_1", "2017-05-30");
        final RobinhoodOrder buyOrder2 = createRobinhoodOrder("buy", "MAGA", "buy_order_2", "2017-05-31");

        final RobinhoodOrder sellOrder1 = createRobinhoodOrder("sell", "MAGA", "sell_order_1", "2017-06-01");

        final RobinhoodOrder buyOrder = liabilityComputer.computeBuyOrderForSell(ImmutableList.of(buyOrder1, buyOrder2), sellOrder1);

        assertEquals(buyOrder, buyOrder1);
        assertEquals(buyOrder.getId(), buyOrder1.getId());
    }

    @Test
    public void testComputeBuyOrderForSell_playerOwnsNewerShare_expectNewerShareMatched() {
        when(walletCache.getOrderIdToPlayer()).thenReturn(ImmutableMap.of("123", "twitch:player1"));

        final RobinhoodOrder buyOrder1 = createRobinhoodOrder("buy", "MAGA", "buy_order_1", "2017-05-30");
        final RobinhoodOrder buyOrder2 = createRobinhoodOrder("buy", "MAGA", "buy_order_2", "2017-05-31");

        final RobinhoodOrder sellOrder1 = createRobinhoodOrder("sell", "MAGA", "sell_order_1", "2017-06-01");

        when(votesDao.getVotesForOrderId("buy_order_1")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player2", "buy_order_1")));
        when(votesDao.getVotesForOrderId("buy_order_2")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "buy_order_1")));
        when(votesDao.getVotesForOrderId("sell_order_1")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "sell_order_1")));

        final RobinhoodOrder buyOrder = liabilityComputer.computeBuyOrderForSell(ImmutableList.of(buyOrder1, buyOrder2), sellOrder1);

        assertEquals(buyOrder, buyOrder2);
        assertEquals(buyOrder.getId(), buyOrder2.getId());
    }

    @Test
    public void testComputeBuyOrderForSell_playerHasHalfOwnership_expectHigherOwnershipShareMatched() {
        when(walletCache.getOrderIdToPlayer()).thenReturn(ImmutableMap.of("123", "twitch:player1"));

        final RobinhoodOrder buyOrder1 = createRobinhoodOrder("buy", "MAGA", "buy_order_1", "2017-05-30");
        final RobinhoodOrder buyOrder2 = createRobinhoodOrder("buy", "MAGA", "buy_order_2", "2017-05-30");
        final RobinhoodOrder buyOrder3 = createRobinhoodOrder("buy", "MAGA", "buy_order_3", "2017-05-31");

        final RobinhoodOrder sellOrder1 = createRobinhoodOrder("sell", "MAGA", "sell_order_1", "2017-06-01");


        when(votesDao.getVotesForOrderId("buy_order_1")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player2", "buy_order_1")));

        when(votesDao.getVotesForOrderId("buy_order_2")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "buy_order_2"),
                                                                                     createPlayerVote("twitch:player2", "buy_order_2")));

        when(votesDao.getVotesForOrderId("buy_order_3")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "buy_order_3")));

        when(votesDao.getVotesForOrderId("sell_order_1")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "sell_order_1")));

        final RobinhoodOrder buyOrder = liabilityComputer.computeBuyOrderForSell(ImmutableList.of(buyOrder1, buyOrder2, buyOrder3), sellOrder1);

        assertEquals(buyOrder, buyOrder3);
        assertEquals(buyOrder.getId(), buyOrder3.getId());
    }

    @Test
    public void testComputeBuyOrderForSell_threeSellersTwoOwnShares_expectHigherOwnershipShareMatched() {
        when(walletCache.getOrderIdToPlayer()).thenReturn(ImmutableMap.of("123", "twitch:player1"));

        final RobinhoodOrder buyOrder0 = createRobinhoodOrder("buy", "MAGA", "buy_order_0", "2017-05-30");
        final RobinhoodOrder buyOrder1 = createRobinhoodOrder("buy", "MAGA", "buy_order_1", "2017-05-31");

        final RobinhoodOrder sellOrder1 = createRobinhoodOrder("sell", "MAGA", "sell_order_1", "2017-06-01");


        when(votesDao.getVotesForOrderId("buy_order_0")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "buy_order_0"),
                                                                                     createPlayerVote("twitch:player2", "buy_order_0"),
                                                                                     createPlayerVote("twitch:player3", "buy_order_0"),
                                                                                     createPlayerVote("twitch:player4", "buy_order_0")));

        when(votesDao.getVotesForOrderId("buy_order_1")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "buy_order_1"),
                                                                                     createPlayerVote("twitch:player2", "buy_order_1"),
                                                                                     createPlayerVote("twitch:player3", "buy_order_1")));


        when(votesDao.getVotesForOrderId("sell_order_1")).thenReturn(ImmutableList.of(createPlayerVote("twitch:player1", "sell_order_1"),
                                                                                      createPlayerVote("twitch:player3", "sell_order_1"),
                                                                                      createPlayerVote("twitch:player5", "sell_order_1")));

        final RobinhoodOrder buyOrder = liabilityComputer.computeBuyOrderForSell(ImmutableList.of(buyOrder0, buyOrder1), sellOrder1);

        assertEquals(buyOrder, buyOrder1);
        assertEquals(buyOrder.getId(), buyOrder1.getId());
    }

    private PlayerVote createPlayerVote(final String player, final String orderId) {
        return new PlayerVote(1L, player, "", "", "", 1L, orderId);
    }

    private RobinhoodOrder createRobinhoodOrder(final String side, final String symbol, final String id, final String created_at) {
        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder(id, "filled", created_at, "100.55", "101.25", side, "1", symbol, "");
        robinhoodOrder.setExecutions(ImmutableList.of(new Execution("1", created_at)));
        return robinhoodOrder;
    }
}
