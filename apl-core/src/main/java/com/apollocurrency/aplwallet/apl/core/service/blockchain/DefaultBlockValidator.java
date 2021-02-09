/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Slf4j
@Singleton
public class DefaultBlockValidator extends AbstractBlockValidator {

    @Inject
    public DefaultBlockValidator(Blockchain blockchain,
                                 BlockchainConfig blockchainConfig,
                                 AccountService accountService,
                                 GeneratorService generatorService,
                                 BlockSerializer blockSerializer,
                                 AccountPublicKeyService accountPublicKeyService) {
        super(blockchain, blockchainConfig, accountService, generatorService, blockSerializer, accountPublicKeyService);
    }

    @Override
    void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        blockchain.getOrLoadTransactions(previousBlock);
        if (!Arrays.equals(Crypto.sha256().digest(((BlockImpl) previousBlock).bytes()),
            block.getPreviousBlockHash())) {
            if (log.isTraceEnabled()) {
                log.trace("Previous block={} height={}", previousBlock.getStringId(), previousBlock.getHeight());
                log.trace("Current block={} prev={} prevHash={}", block.getStringId(), Long.toUnsignedString(block.getPreviousBlockId()), block.getPreviousBlockHash());
                log.trace("PrevBlock={}", blockSerializer.getJSONObject(previousBlock).toJSONString());
                log.trace("Current block={}", blockSerializer.getJSONObject(block).toJSONString());
            }
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Previous block hash doesn't match", blockSerializer.getJSONObject(block));
        }
    }

    @Override
    void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException {
        boolean checkResult = accountPublicKeyService.setOrVerifyPublicKey(block.getGeneratorId(), block.getGeneratorPublicKey());
        if (!block.checkSignature() && !checkResult) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Block signature verification failed", blockSerializer.getJSONObject(block));
        }
    }

    @Override
    void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        int actualBlockTime = block.getTimestamp() - previousBlock.getTimestamp();
        if (actualBlockTime < blockchainConfig.getCurrentConfig().getAdaptiveBlockTime()
            && blockchain.getOrLoadTransactions(block).size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Invalid adaptive block: time - " + actualBlockTime + " height " + previousBlock.getHeight() + 1
                    + ". Perhaps blockchain config is outdated", null);
        }
    }

    @Override
    void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (blockchain.getOrLoadTransactions(block).size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Incorrect instant block", blockSerializer.getJSONObject(block));
        }
    }

    @Override
    void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (blockchain.getOrLoadTransactions(block).size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock() || block.getTimeout() != 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Incorrect regular block", blockSerializer.getJSONObject(block));
        }
    }
}
