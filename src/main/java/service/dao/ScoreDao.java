package service.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import service.computer.ScoreComputer;
import service.data.Score;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ScoreDao {

    @Autowired
    private ScoreComputer scoreComputer;

    private LoadingCache<String, List<Score>> electionCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(500, TimeUnit.MILLISECONDS)
                        .build(new CacheLoader<String, List<Score>>() {
                            @Override
                            public List<Score> load(final String key) throws Exception {
                                return scoreComputer.computeHighScoreList();
                            }
                        });

    public List<Score> getHighScoreList() {
        try {
            return electionCache.get("stockstream");
        } catch (ExecutionException e) {
            log.warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }


}
