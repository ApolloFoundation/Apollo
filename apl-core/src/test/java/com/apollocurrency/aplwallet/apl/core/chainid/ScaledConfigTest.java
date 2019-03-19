/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScaledConfigTest {
    @Test
    void testGetReferencedHeightSpan() {
        BlockTimeScaledConfig blockTimeScaledConfig = new BlockTimeScaledConfig(2);
        assertEquals(2_592_000, blockTimeScaledConfig.getReferencedTransactionHeightSpan());
        blockTimeScaledConfig = new BlockTimeScaledConfig(60);
        assertEquals(86_400, blockTimeScaledConfig.getReferencedTransactionHeightSpan());
    }
}
