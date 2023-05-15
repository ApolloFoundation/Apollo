/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
class TransactionConverterTest {

    static TransactionTestData td = new TransactionTestData();
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }


    TransactionBuilderFactory transactionBuilderFactory = new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig);
    TransactionEntityToModelConverter toModelConverter = new TransactionEntityToModelConverter(td.getTransactionTypeFactory(), transactionBuilderFactory);
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
        assertEquals(transaction, model);
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