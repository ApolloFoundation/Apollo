package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingApprovalResult;

public class PhasingApprovedResultTestData {
            public final PhasingApprovalResult RESULT_1 = new PhasingApprovalResult(110L         ,    510  , 5                 , 120              );
            public final PhasingApprovalResult RESULT_2 = new PhasingApprovalResult(120L         ,    525  , 10                , 110              );
            public final PhasingApprovalResult RESULT_3 = new PhasingApprovalResult(130L         ,    525  , 10                , 130              );
            public final PhasingApprovalResult RESULT_4 = new PhasingApprovalResult(140L         ,    550  , 15                , 140              );

    public final PhasingApprovalResult NEW_RESULT = new PhasingApprovalResult(141L         ,    551  , 12                , 150              );
}
