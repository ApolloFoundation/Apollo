/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedChildDerivedEntity;

public class VersionedChildDerivedEntityKeyFactory extends LongKeyFactory<VersionedChildDerivedEntity> {
    public VersionedChildDerivedEntityKeyFactory() {
        super("parent_id");
    }

    @Override
    public DbKey newKey(VersionedChildDerivedEntity versionedChildDerivedEntity) {
        if (versionedChildDerivedEntity.getDbKey() == null) {
            versionedChildDerivedEntity.setDbKey(new LongKey(versionedChildDerivedEntity.getParentId()));
        }
        return versionedChildDerivedEntity.getDbKey();
    }
}
