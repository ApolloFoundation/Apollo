/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockImpl;

import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class BlockEntityToModelConverter implements Converter<BlockEntity, Block> {

    @Override
    public Block apply(BlockEntity entity) {
        Block block = new BlockImpl(entity.getVersion()
            , entity.getTimestamp()
            , entity.getPreviousBlockId()
            , entity.getTotalAmountATM()
            , entity.getTotalFeeATM()
            , entity.getPayloadLength()
            , entity.getPayloadHash()
            , entity.getGeneratorId()
            , entity.getGenerationSignature()
            , entity.getBlockSignature()
            , entity.getPreviousBlockHash()
            , entity.getCumulativeDifficulty()
            , entity.getBaseTarget()
            , entity.getNextBlockId()
            , entity.getHeight()
            , entity.getId()
            , entity.getTimeout()
            , null);

        return block;
    }
}
