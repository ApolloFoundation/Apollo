/*
 *  Copyright © 2019-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

/**
 * Interface is used to mark several Entity Tables to be managed inside 'Full Test Service Engine' (Lucene)
 */
public interface SearchableTableMarkerInterface<T extends DerivedEntity> {
    // EMPTY  because it is 'marker interface'
}
