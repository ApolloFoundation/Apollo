/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

public interface Searchable {

    default boolean isSearchable() {
        return false;
    }

}
