/*
 *  Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

public interface SearchableTableInterface<T extends DerivedEntity> {

    DbIterator<T> search(String query, DbClause dbClause, int from, int to);

    DbIterator<T> search(String query, DbClause dbClause, int from, int to, String sort);

}
