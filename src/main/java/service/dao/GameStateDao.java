package service.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.GameStateRegistry;
import stockstream.database.GameStateStub;
import stockstream.database.RobinhoodAccountRegistry;
import stockstream.database.RobinhoodAccountStub;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameStateDao {

    @Autowired
    private GameStateRegistry gameStateRegistry;

    @Autowired
    private RobinhoodAccountRegistry robinhoodAccountRegistry;

    private LoadingCache<String, GameStateStub> gameStateCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(1, TimeUnit.SECONDS)
                        .build(new CacheLoader<String, GameStateStub>() {
                            @Override
                            public GameStateStub load(final String key) throws Exception {
                                return gameStateRegistry.getGameStateStub();
                            }
                        });

    private LoadingCache<String, RobinhoodAccountStub> accountStubCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(1, TimeUnit.SECONDS)
                        .build(new CacheLoader<String, RobinhoodAccountStub>() {
                            @Override
                            public RobinhoodAccountStub load(final String key) throws Exception {
                                return robinhoodAccountRegistry.getAccountInfo();
                            }
                        });

    private <T> Optional<T> emptyIfException(final String key, final LoadingCache<String, T> cache) {
        try {
            return Optional.ofNullable(cache.get(key));
        } catch (final ExecutionException e) {
            log.warn(e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<GameStateStub> getGameState() {
        return emptyIfException("gameState", gameStateCache);
    }

    public Optional<RobinhoodAccountStub> getRobinhoodAccountState() {
        return emptyIfException("stockstream", accountStubCache);
    }

}
