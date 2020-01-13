package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;

import java.sql.Timestamp;

public class DexOperationTestData {
        public final DexOperation OP_1 = op(1001L  ,"APL-RQTU-56W2-AAMY-7MTLB", 4     , "120"     , "Initiate atomic swap"    , null   , false    , "2020-01-10 12:00:01");
        public final DexOperation OP_2 = op(1002L  ,"APL-EPHR-JVKK-2PWW-HPW9H", 4     , "120"     , "Initiate atomic swap"    , "100"  , true     , "2020-01-10 12:00:02");
        public final DexOperation OP_3 = op(1003L  ,"APL-RQTU-56W2-AAMY-7MTLB", 4     , "130"     , "Initiate atomic swap"    , "100"  , false    , "2020-01-10 12:00:01");
        public final DexOperation OP_4 = op(1004L  ,"APL-EPHR-JVKK-2PWW-HPW9H", 1     , "120"     , "New order"               , "100"  , true     , "2020-01-10 12:00:02");

    private static DexOperation op(long dbId, String account, int stage, String eid, String descr, String details, boolean finished, String timestamp) {
        return new DexOperation(dbId, account, DexOperation.Stage.from(stage), eid, descr, details, finished, Timestamp.valueOf(timestamp));
    }
}

