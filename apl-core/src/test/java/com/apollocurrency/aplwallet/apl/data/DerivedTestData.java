/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedChildDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;

import java.util.List;

public class DerivedTestData {
    public final DerivedIdEntity ENTITY_1 = new DerivedIdEntity(1000L, 500, 10);
    public final DerivedIdEntity ENTITY_2 = new DerivedIdEntity(1010L, 500, 20);
    public final DerivedIdEntity ENTITY_3 = new DerivedIdEntity(1020L, 502, 30);
    public final DerivedIdEntity ENTITY_4 = new DerivedIdEntity(1030L, 506, 40);
    public final DerivedIdEntity ENTITY_5 = new DerivedIdEntity(1040L, 506, 50);
    public final DerivedIdEntity ENTITY_6 = new DerivedIdEntity(1050L, 506, 60);

    public final DerivedIdEntity NEW_ENTITY = new DerivedIdEntity(ENTITY_6.getDbId() + 1, 70, 507);

    public final List<DerivedIdEntity> ALL = List.of(
            ENTITY_1,
            ENTITY_2,
            ENTITY_3,
            ENTITY_4,
            ENTITY_5,
            ENTITY_6
    );

    public VersionedDerivedIdEntity VERSIONED_ENTITY_1_1 = new VersionedDerivedIdEntity(1000L, 99 , 1L, true );
    public VersionedDerivedIdEntity VERSIONED_ENTITY_2_1 = new VersionedDerivedIdEntity(1010L, 100, 2L, false);
    public VersionedDerivedIdEntity VERSIONED_ENTITY_3_1 = new VersionedDerivedIdEntity(1020L, 100, 3L, false);
    public VersionedDerivedIdEntity VERSIONED_ENTITY_2_2 = new VersionedDerivedIdEntity(1030L, 101, 2L, false);
    public VersionedDerivedIdEntity VERSIONED_ENTITY_4_1 = new VersionedDerivedIdEntity(1040L, 101, 4L, false);
    public VersionedDerivedIdEntity VERSIONED_ENTITY_2_3 = new VersionedDerivedIdEntity(1050L, 102, 2L, true );
    public VersionedDerivedIdEntity VERSIONED_ENTITY_4_2 = new VersionedDerivedIdEntity(1060L, 102, 4L, false);
    public VersionedDerivedIdEntity VERSIONED_ENTITY_3_2 = new VersionedDerivedIdEntity(1070L, 105, 3L, true );

    public VersionedDerivedIdEntity NEW_VERSIONED_ENTITY = new VersionedDerivedIdEntity(1071L, 106, 5L, true);

    public final List<VersionedDerivedIdEntity> ALL_VERSIONED = List.of(
            VERSIONED_ENTITY_1_1,
            VERSIONED_ENTITY_2_1,
            VERSIONED_ENTITY_3_1,
            VERSIONED_ENTITY_2_2,
            VERSIONED_ENTITY_4_1,
            VERSIONED_ENTITY_2_3,
            VERSIONED_ENTITY_4_2,
            VERSIONED_ENTITY_3_2
    );
    //                                               parentId, childId, stage
    public VersionedChildDerivedEntity VCE_1_1_1 = new VersionedChildDerivedEntity(1000L, 1, 1, 125, false);
    public VersionedChildDerivedEntity VCE_2_1_1 = new VersionedChildDerivedEntity(1010L, 2, 1, 126, false);
    public VersionedChildDerivedEntity VCE_2_1_2 = new VersionedChildDerivedEntity(1020L, 2, 1, 127, false);
    public VersionedChildDerivedEntity VCE_2_2_2 = new VersionedChildDerivedEntity(1030L, 2, 2, 127, false);
    public VersionedChildDerivedEntity VCE_3_1_1 = new VersionedChildDerivedEntity(1040L, 3, 1, 127, false);
    public VersionedChildDerivedEntity VCE_1_1_2 = new VersionedChildDerivedEntity(1050L, 1, 1, 128, true );
    public VersionedChildDerivedEntity VCE_1_2_2 = new VersionedChildDerivedEntity(1060L, 1, 2, 128, true );
    public VersionedChildDerivedEntity VCE_2_1_3 = new VersionedChildDerivedEntity(1070L, 2, 1, 129, true );
    public VersionedChildDerivedEntity VCE_2_2_3 = new VersionedChildDerivedEntity(1080L, 2, 2, 129, true );
    public VersionedChildDerivedEntity VCE_2_3_3 = new VersionedChildDerivedEntity(1090L, 2, 3, 129, true );
    public VersionedChildDerivedEntity VCE_3_1_2 = new VersionedChildDerivedEntity(1100L, 3, 1, 130, false);
    public VersionedChildDerivedEntity VCE_4_1_1 = new VersionedChildDerivedEntity(1110L, 4, 1, 130, true );

    public VersionedChildDerivedEntity NEW_VCE_1 = new VersionedChildDerivedEntity(1111L, 5, 1, 131, true );
    public VersionedChildDerivedEntity NEW_VCE_2 = new VersionedChildDerivedEntity(1112L, 5, 2, 131, true );

    public final List<VersionedChildDerivedEntity> ALL_CHILD = List.of(
            VCE_1_1_1,
            VCE_2_1_1,
            VCE_2_1_2,
            VCE_2_2_2,
            VCE_3_1_1,
            VCE_1_1_2,
            VCE_1_2_2,
            VCE_2_1_3,
            VCE_2_2_3,
            VCE_2_3_3,
            VCE_3_1_2,
            VCE_4_1_1
    );















}
