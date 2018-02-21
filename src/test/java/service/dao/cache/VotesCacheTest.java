package service.dao.cache;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.database.PlayerVote;
import stockstream.database.PlayerVoteRegistry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class VotesCacheTest {

    @Mock
    private PlayerVoteRegistry playerVoteRegistry;

    @InjectMocks
    private VotesCache votesCache;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRefreshDataMaps_sampleDataPassed_expectMapsPopulated() {
        final PlayerVote testVote = new PlayerVote();
        testVote.setOrder_id("123");
        testVote.setPlatform_username("mike");
        testVote.setDate("05/30/2017");

        when(playerVoteRegistry.getAllPlayerVotes()).thenReturn(ImmutableList.of(testVote));

        votesCache.reloadCache();

        assertEquals(1, votesCache.getDateToVotes().get("05/30/2017").size());
        assertEquals(1, votesCache.getOrderIdToVotes().get("123").size());
        assertEquals(1, votesCache.getPlayerToVotes().get("mike").size());
    }

}
