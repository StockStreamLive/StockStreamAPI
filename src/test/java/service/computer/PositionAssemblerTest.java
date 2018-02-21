package service.computer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import service.data.Position;
import stockstream.database.PlayerVote;
import stockstream.database.RobinhoodOrder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class PositionAssemblerTest {

    @Mock
    private LiabilityComputer liabilityComputer;

    @Mock
    private ScoreComputer scoreComputer;

    @InjectMocks
    private PositionAssembler positionAssembler;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testComputeAllPositions_oneOpenClosed_expectValidPositions() {
        final RobinhoodOrder buyOrder1 = new RobinhoodOrder();
        buyOrder1.setId("123");
        buyOrder1.setSymbol("AMZN");
        buyOrder1.setCreated_at("2017-10-13T21:13:05.939928Z");
        buyOrder1.setSide("buy");
        buyOrder1.setState("filled");
        buyOrder1.setQuantity("1.000");

        final RobinhoodOrder buyOrder2 = new RobinhoodOrder();
        buyOrder2.setId("124");
        buyOrder2.setSymbol("AMZN");
        buyOrder2.setCreated_at("2017-10-13T21:14:05.939928Z");
        buyOrder2.setSide("buy");
        buyOrder2.setState("filled");
        buyOrder2.setQuantity("1.000");

        final RobinhoodOrder sellOrder1 = new RobinhoodOrder();
        sellOrder1.setId("125");
        sellOrder1.setSymbol("AMZN");
        sellOrder1.setCreated_at("2017-10-13T21:15:05.939928Z");
        sellOrder1.setSide("sell");
        sellOrder1.setState("filled");
        sellOrder1.setQuantity("1.000");

        final PlayerVote testVote = new PlayerVote();
        testVote.setOrder_id("123");
        testVote.setPlatform_username("twitch:mike");
        testVote.setDate("05/30/2017");

        when(liabilityComputer.computeLiablePlayers(buyOrder1.getId())).thenReturn(ImmutableSet.of("twitch:mike"));
        when(liabilityComputer.computeBuyOrderForSell(any(), any())).thenReturn(buyOrder1);
        when(scoreComputer.qualifiedForPromotion(any(), any())).thenReturn(false);

        final List<Position> positions = positionAssembler.computeAllPositions(ImmutableList.of(buyOrder1, buyOrder2, sellOrder1));

        assertEquals(2, positions.size());
        assertEquals("123", positions.get(0).getBuyOrder().getId());

        assertEquals(null, positions.get(1).getSellOrder());
        assertEquals("124", positions.get(1).getBuyOrder().getId());
    }

    @Test
    public void testComputeAllPositions_oneWallet_expectNotQualified() {
        final RobinhoodOrder buyOrder1 = new RobinhoodOrder();
        buyOrder1.setId("123");
        buyOrder1.setSymbol("AMZN");
        buyOrder1.setCreated_at("2017-10-13T21:13:05.939928Z");
        buyOrder1.setSide("buy");
        buyOrder1.setState("filled");
        buyOrder1.setQuantity("1.000");

        final RobinhoodOrder buyOrder2 = new RobinhoodOrder();
        buyOrder2.setId("124");
        buyOrder2.setSymbol("AMZN");
        buyOrder2.setCreated_at("2017-10-13T21:14:05.939928Z");
        buyOrder2.setSide("buy");
        buyOrder2.setState("filled");
        buyOrder2.setQuantity("1.000");

        final RobinhoodOrder sellOrder1 = new RobinhoodOrder();
        sellOrder1.setId("125");
        sellOrder1.setSymbol("AMZN");
        sellOrder1.setCreated_at("2017-10-13T21:15:05.939928Z");
        sellOrder1.setSide("sell");
        sellOrder1.setState("filled");
        sellOrder1.setQuantity("1.000");

        final PlayerVote testVote = new PlayerVote();
        testVote.setOrder_id("123");
        testVote.setPlatform_username("twitch:mike");
        testVote.setDate("05/30/2017");

        when(liabilityComputer.computeLiablePlayers(buyOrder1.getId())).thenReturn(ImmutableSet.of("twitch:mike"));
        when(liabilityComputer.computeBuyOrderForSell(any(), any())).thenReturn(buyOrder1);
        when(liabilityComputer.isWalletOrder(any())).thenReturn(true);
        when(scoreComputer.qualifiedForPromotion(any(), any())).thenReturn(false);

        final List<Position> positions = positionAssembler.computeAllPositions(ImmutableList.of(buyOrder1, buyOrder2, sellOrder1));

        assertEquals(2, positions.size());
        assertEquals(false, positions.get(0).isQualifiedForPromotion());
    }

}
