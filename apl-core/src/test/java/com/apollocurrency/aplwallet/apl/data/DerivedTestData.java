/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.entity.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedChangeableNullableDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedChildDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedDerivedIdEntity;

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


    public final VersionedChangeableNullableDerivedEntity VCDE_1_1 = new VersionedChangeableNullableDerivedEntity(1000L, 1 , 100, 225, "1_1", null,false, false);
    public final VersionedChangeableNullableDerivedEntity VCDE_1_2 = new VersionedChangeableNullableDerivedEntity(1010L, 1 , 99 , 226, null, "1_2",false, false);
    public final VersionedChangeableNullableDerivedEntity VCDE_2_1 = new VersionedChangeableNullableDerivedEntity(1020L, 2 , 99 , 226, null, "1_2",false, false);
    public final VersionedChangeableNullableDerivedEntity VCDE_3_1 = new VersionedChangeableNullableDerivedEntity(1030L, 3 , 0  , 227, null, null,true , false);
    public final VersionedChangeableNullableDerivedEntity VCDE_4_1 = new VersionedChangeableNullableDerivedEntity(1040L, 4 , 0  , 227, "4_1", "4_1",false, true);
    public final VersionedChangeableNullableDerivedEntity VCDE_2_2 = new VersionedChangeableNullableDerivedEntity(1050L, 2 , 10 , 228, "2_2", "1_2", true , false);
    public final VersionedChangeableNullableDerivedEntity VCDE_1_3 = new VersionedChangeableNullableDerivedEntity(1060L, 1 , 97 , 228, null,"1_2", true , false);
    public final VersionedChangeableNullableDerivedEntity VCDE_4_2 = new VersionedChangeableNullableDerivedEntity(1070L, 4 , 0  , 228, "4_1", null,false, true);

    public final VersionedChangeableNullableDerivedEntity NEW_VCDE = new VersionedChangeableNullableDerivedEntity(1071L, 5 , 123  , 229, "5_1", null, true, false);

    public final List<VersionedChangeableNullableDerivedEntity> ALL_VCDE = List.of(
            VCDE_1_1,
            VCDE_1_2,
            VCDE_2_1,
            VCDE_3_1,
            VCDE_4_1,
            VCDE_2_2,
            VCDE_1_3,
            VCDE_4_2
    );
//    public final VersionedChangeableNullableDerivedEntity VCNDE_1_1 = new (1010L, 1 , 100 , 200, null, "1", false);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_1_2 = new VersionedChangeableNullableDerivedEntity(1020L, 1 , 99  , 201, "1", null, false);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_1_3 = new VersionedChangeableNullableDerivedEntity(1030L, 1 , 98  , 210, null, "2", false);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_1_4 = new VersionedChangeableNullableDerivedEntity(1040L, 1 , 97  , 220, "2", null, true);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_2_1 = new VersionedChangeableNullableDerivedEntity(1050L, 2 , 102 , 205, null, null, false);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_2_2 = new VersionedChangeableNullableDerivedEntity(1060L, 2 , 101 , 210, "1", null, false);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_2_3 = new VersionedChangeableNullableDerivedEntity(1070L, 2 , 100 , 211, "2", "1", false );
//    public final VersionedChangeableNullableDerivedEntity VCNDE_2_4 = new VersionedChangeableNullableDerivedEntity(1080L, 2 , 99  , 250, null,"2", false);
//    public final VersionedChangeableNullableDerivedEntity VCNDE_2_5 = new VersionedChangeableNullableDerivedEntity(1090L, 2 , 98  , 252, null, null, true );
//    public final VersionedChangeableNullableDerivedEntity VCNDE_3_1 = new VersionedChangeableNullableDerivedEntity(1100L, 3 , 99  , 214, "1", "1", false );
//    public final VersionedChangeableNullableDerivedEntity VCNDE_3_2 = new VersionedChangeableNullableDerivedEntity(1110L, 3 , 98  , 215, null, null,false );
//    public final VersionedChangeableNullableDerivedEntity VCNDE_3_3 = new VersionedChangeableNullableDerivedEntity(1120L, 3 , 97  , 220, null, "2", false );
//    public final VersionedChangeableNullableDerivedEntity VCNDE_3_4 = new VersionedChangeableNullableDerivedEntity(1130L, 3 , 96  , 225, "2", null, true);
//
//    public final List<VersionedChangeableNullableDerivedEntity> ALL_VCNDE = List.of(
//            VCNDE_1_1,
//            VCNDE_1_2,
//            VCNDE_1_3,
//            VCNDE_1_4,
//            VCNDE_2_1,
//            VCNDE_2_2,
//            VCNDE_2_3,
//            VCNDE_2_4,
//            VCNDE_2_5,
//            VCNDE_3_1,
//            VCNDE_3_2,
//            VCNDE_3_3,
//            VCNDE_3_4
//    );













}
