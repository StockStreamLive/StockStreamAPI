package service.computer;

import org.springframework.beans.factory.annotation.Autowired;
import service.dao.AutoCompleteItem;
import service.dao.cache.AutoCompleteCache;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

public class AutoCompleteEngine {


    @Autowired
    private AutoCompleteCache autoCompleteCache;

    public Collection<AutoCompleteItem> findItems(final String query) {
        final SortedSet<AutoCompleteItem> results = new TreeSet<>((item1, item2) -> Integer.compare(item2.getRank(), item1.getRank()));

        autoCompleteCache.getKeywordToSearchItems().forEach((key, value) -> {
            if (key.startsWith(query.toLowerCase())) {
                results.addAll(value);
            }
        });

        return results;
    }

}
