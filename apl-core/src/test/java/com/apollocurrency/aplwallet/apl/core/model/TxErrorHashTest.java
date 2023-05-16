/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TxErrorHashTest {

    @Test
    void create() {
        TxErrorHash error = new TxErrorHash(Long.MIN_VALUE, "Tx Test Error");

        assertEquals("6ea420146990f55001dcdab23dde17ae28b19a5a4744e8aff3427387df8432f2",
            Convert.toHexString(error.getErrorHash()));
        assertEquals("TxErrorHash{id=9223372036854775808," +
            " errorHash=6ea420146990f55001dcdab23dde17ae28b19a5a4744e8aff3427387df8432f2, error=Tx Test Error}", error.toString());
    }

}