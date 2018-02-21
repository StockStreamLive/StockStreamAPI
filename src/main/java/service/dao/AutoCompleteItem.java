package service.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Data
@AllArgsConstructor
public class AutoCompleteItem {

    private String title;
    private String description;
    private String url;
    private int rank;

    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) return true;

        if (!(otherObject instanceof AutoCompleteItem)) return false;

        final AutoCompleteItem otherAutoCompleteItem = (AutoCompleteItem) otherObject;

        return new EqualsBuilder().append(title, otherAutoCompleteItem.title).append(url, otherAutoCompleteItem.url).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(title).append(url).toHashCode();
    }
}
