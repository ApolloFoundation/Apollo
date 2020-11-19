/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.api.v2.model.BlockInfo;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class BlockInfoMapper implements Converter<Block, BlockInfo> {

    @Override
    public BlockInfo apply(Block model) {
        BlockInfo dto = new BlockInfo();
        dto.setId(model.getStringId());
        dto.setHeight((long) model.getHeight());
        dto.setGenerator(Long.toUnsignedString(model.getGeneratorId()));
        dto.setGeneratorPublicKey(Convert.toHexString(model.getGeneratorPublicKey()));
        dto.setTimestamp((long) model.getTimestamp());
        dto.setTimeout(model.getTimeout());
        dto.setNumberOfTransactions(model.getTransactions().size());
        dto.setTotalFeeATM(String.valueOf(model.getTotalFeeATM()));
        dto.setPayloadLength(model.getPayloadLength());
        dto.setVersion(model.getVersion());
        dto.setBaseTarget(String.valueOf(model.getBaseTarget()));
        dto.setCumulativeDifficulty(model.getCumulativeDifficulty().toString());
        if (model.getPreviousBlockId() != 0) {
            dto.setPreviousBlock(Long.toUnsignedString(model.getPreviousBlockId()));
        }
        dto.setPreviousBlockHash(Convert.toHexString(model.getPreviousBlockHash()));
        if (model.getNextBlockId() != 0) {
            dto.setNextBlock(Long.toUnsignedString(model.getNextBlockId()));
        }
        dto.setPayloadHash(Convert.toHexString(model.getPayloadHash()));
        dto.setGenerationSignature(Convert.toHexString(model.getGenerationSignature()));
        dto.setBlockSignature(Convert.toHexString(model.getBlockSignature()));
        dto.setTotalAmountATM(String.valueOf(
            model.getTransactions().stream().mapToLong(Transaction::getAmountATM).sum()));
        return dto;
    }

}
