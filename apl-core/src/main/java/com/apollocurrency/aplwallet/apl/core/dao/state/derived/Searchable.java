package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

public interface Searchable {

    default boolean isSearchable() {
        return false;
    }

}
