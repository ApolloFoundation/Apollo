/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedChildDerivedEntity;

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
