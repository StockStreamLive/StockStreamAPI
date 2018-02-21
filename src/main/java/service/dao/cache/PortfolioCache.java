package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;
import stockstream.database.HistoricalEquityValue;
import stockstream.database.HistoricalEquityValueRegistry;
import stockstream.util.TimeUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PortfolioCache implements ReloadingCache {

    @Autowired
    private HistoricalEquityValueRegistry historicalEquityValueRegistry;

    @Getter
    private List<HistoricalEquityValue> dailyPortfolioValues = new ArrayList<>();

    @Getter
    private ConcurrentHashMap<String, List<HistoricalEquityValue>> intradayPortfolioValues = new ConcurrentHashMap<>();

    @Override
    public void reloadCache() {
        final List<HistoricalEquityValue> equityValues = new ArrayList<>(historicalEquityValueRegistry.getAllHistoricalEquityValues());

        final List<HistoricalEquityValue> dailyStrToPortfolioValues = new ArrayList<>();
        final ConcurrentHashMap<String, List<HistoricalEquityValue>> intradayStrPortfolioValues = new ConcurrentHashMap<>();

        equityValues.forEach(value -> {
            final Optional<Date> dateTimeOptional = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss'Z'", value.getBegins_at(), "GMT");
            if (!dateTimeOptional.isPresent()) {
                log.info("Equity Value {} has bad date!");
                return;
            }
            final Date date = dateTimeOptional.get();
            final Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
            calendar.setTime(date);
            final int hour = calendar.get(Calendar.HOUR_OF_DAY);
            final int minute = calendar.get(Calendar.MINUTE);
            if (hour == 0 && minute == 0) {
                dailyStrToPortfolioValues.add(value);
            } else {
                final String dateStr = TimeUtil.getCanonicalYMDString(dateTimeOptional.get());
                intradayStrPortfolioValues.computeIfAbsent(dateStr, list -> new ArrayList<>()).add(value);
            }
        });

        log.info("Got {} dailyPortfolioValues", dailyStrToPortfolioValues.size());
        log.info("Got {} intradayStrPortfolioValues", intradayStrPortfolioValues.size());

        this.dailyPortfolioValues = dailyStrToPortfolioValues;
        this.intradayPortfolioValues = intradayStrPortfolioValues;
    }

    @Override
    public boolean isPrimed() {
        return !CollectionUtils.isEmpty(intradayPortfolioValues.values());
    }
}
