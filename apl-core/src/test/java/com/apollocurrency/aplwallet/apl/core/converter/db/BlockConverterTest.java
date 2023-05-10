/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
class BlockConverterTest {

    byte[] publicKey = Convert.parseHexString("bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37");
    BlockTestData td = new BlockTestData();
    BlockEntityToModelConverter entityToModelConverter = new BlockEntityToModelConverter();
    BlockModelToEntityConverter modelToEntityConverter = new BlockModelToEntityConverter();

    @Test
    void testModelToEntityToModelConverting() {
        //GIVEN
        Block block = td.BLOCK_1;
        block.assignBlockData(block.getTransactions(), publicKey);
        List<Transaction> transactions = block.getTransactions();
        //WHEN
        BlockEntity entity = modelToEntityConverter.convert(block);
        Block model = entityToModelConverter.convert(entity);
        model.assignBlockData(transactions, publicKey);
        //THEN
        assertArrayEquals(block.getBytes(), model.getBytes());
    }
}