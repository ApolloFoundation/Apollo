/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;

import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class BlockModelToEntityConverter implements Converter<Block, BlockEntity> {

    @Override
    public BlockEntity apply(Block model) {
        BlockEntity block = BlockEntity.builder()
            .version(model.getVersion())
            .timestamp(model.getTimestamp())
            .previousBlockId(model.getPreviousBlockId())
            .totalAmountATM(model.getTotalAmountATM())
            .totalFeeATM(model.getTotalFeeATM())
            .payloadLength(model.getPayloadLength())
            .payloadHash(model.getPayloadHash())
            .generatorId(model.getGeneratorId())
            .generationSignature(model.getGenerationSignature())
            .blockSignature(model.getBlockSignature())
            .previousBlockHash(model.getPreviousBlockHash())
            .cumulativeDifficulty(model.getCumulativeDifficulty())
            .baseTarget(model.getBaseTarget())
            .nextBlockId(model.getNextBlockId())
            .height(model.getHeight())
            .id(model.getId())
            .timeout(model.getTimeout())
            .build();
        return block;
    }
}
