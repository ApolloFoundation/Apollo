/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
class TransactionConverterTest {

    static TransactionTestData td = new TransactionTestData();

    TransactionBuilder transactionBuilder = new TransactionBuilder(td.getTransactionTypeFactory());
    TransactionEntityToModelConverter toModelConverter = new TransactionEntityToModelConverter(td.getTransactionTypeFactory(), transactionBuilder);
    TransactionModelToEntityConverter toEntityConverter = new TransactionModelToEntityConverter();

    @ParameterizedTest
    @MethodSource("provideTransactions")
    void testModelToEntityToModelConverting(Transaction transaction) {
        //GIVEN transaction
        //System.out.println(Convert.toHexString(transaction.bytes()));

        //WHEN
        TransactionEntity entity = toEntityConverter.convert(transaction);
        Transaction model = toModelConverter.convert(entity);

        //THEN
        assertArrayEquals(transaction.bytes(), model.bytes());
    }

    static Stream<Arguments> provideTransactions() {
        return Stream.of(
            arguments(td.TRANSACTION_V2_1),
            arguments(td.TRANSACTION_13),
            arguments(td.TRANSACTION_14),
            arguments(td.NEW_TRANSACTION_0),
            arguments(td.NEW_TRANSACTION_1)
        );
    }
}