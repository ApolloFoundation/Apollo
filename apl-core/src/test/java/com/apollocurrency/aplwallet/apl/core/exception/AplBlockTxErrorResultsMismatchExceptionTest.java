/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.TxErrorHash;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AplBlockTxErrorResultsMismatchExceptionTest {

    @Test
    void create() {
        BlockTestData td = new BlockTestData();
        TxErrorHash error = new TxErrorHash(-1, "Error");

        AplBlockTxErrorResultsMismatchException ex = new AplBlockTxErrorResultsMismatchException(td.BLOCK_10, List.of(error));

        assertEquals("Tx errors after execution: " +
            "[TxErrorHash{id=18446744073709551615, " +
            "errorHash=54a0e8c17ebb21a11f8a25b8042786ef7efe52441e6cc87e92c67e0c4c0c6e78, error=Error}] are not the same " +
            "as declared: [TxErrorHash{id=9145605905642517648, " +
            "errorHash=589985a3eb90ee4eb56ffb83f1d0e068171d1fa6d8be766e5953e30992398345, error=Transaction  " +
            "#10 error message}," +
            " TxErrorHash{id=16909767887484625916, " +
            "errorHash=a603f3773430a7cc9f20f111a8fd3edd930bade6a4e7d4e05ebaee2a1b77172c, error=Transaction  #11 error message}]" +
            ", block: 12239762356076828396", ex.getMessage());
    }
}