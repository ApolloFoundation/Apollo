/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andrii Boiarskyi
 * @see TransactionTypeTest
 * @see TransactionTypes
 * @since 1.48.4
 */
public class TransactionTypesTest {
    @Test
    void findTransactionType() {
        assertThrows(IllegalArgumentException.class, () -> TransactionTypes.find((byte) -1, (byte) -1));

        TransactionTypes.TransactionTypeSpec ordinaryPayment = TransactionTypes.find((byte) 0, (byte) 0);
        assertNotNull(ordinaryPayment);
        assertEquals(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, ordinaryPayment);
    }
}
