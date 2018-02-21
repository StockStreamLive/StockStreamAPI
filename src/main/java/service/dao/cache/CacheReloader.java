package service.dao.cache;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheReloader {

    private static final int RELOAD_INTERVAL_SECONDS = Integer.parseInt(System.getenv("RELOAD_INTERVAL_SECONDS"));

    private static final ScheduledExecutorService cacheUpdater = Executors.newScheduledThreadPool(1);

    @Autowired
    private ImmutableList<ReloadingCache> reloadingCaches;

    @PostConstruct
    public void init() {
        cacheUpdater.scheduleAtFixedRate(() -> {
            try {
                log.info("Refreshing data in OrdersCache");
                reloadingCaches.forEach(ReloadingCache::reloadCache);
            } catch (final Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
        }, 0L, RELOAD_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean cachesPrimed() {
        for (final ReloadingCache reloadingCache : reloadingCaches) {
            if (!reloadingCache.isPrimed()) {
                return false;
            }
        }
        return true;
    }

}
