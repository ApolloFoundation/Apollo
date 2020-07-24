/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
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
                                 GeneratorService generatorService) {
        super(blockchain, blockchainConfig, accountService, generatorService);
    }

    @Override
    void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!Arrays.equals(Crypto.sha256().digest(((BlockImpl) previousBlock).bytes()),
            block.getPreviousBlockHash())) {
            if (log.isTraceEnabled()) {
                log.trace("Pervious block={} height={}", previousBlock.getStringId(), previousBlock.getHeight());
                log.trace("Current block={} prev={} prevHash={}", block.getStringId(), Long.toUnsignedString(block.getPreviousBlockId()), block.getPreviousBlockHash());
                log.trace("PrevBlock={}", previousBlock.toJsonString());
                log.trace("Current block={}", block.toJsonString());
            }
            throw new BlockchainProcessor.BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
    }

    @Override
    void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException {
        boolean checkResult = accountService.setOrVerifyPublicKey(block.getGeneratorId(), block.getGeneratorPublicKey());
        if (!block.checkSignature() && !checkResult) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Block signature verification failed", block);
        }
    }

    @Override
    void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        int actualBlockTime = block.getTimestamp() - previousBlock.getTimestamp();
        if (actualBlockTime < blockchainConfig.getCurrentConfig().getAdaptiveBlockTime() && block.getOrLoadTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid adaptive block: time - " + actualBlockTime + " height " + previousBlock.getHeight() + 1 + ". Perhaps blockchain config is outdated", null);
        }
    }

    @Override
    void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getOrLoadTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect instant block", block);
        }
    }

    @Override
    void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getOrLoadTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock() || block.getTimeout() != 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect regular block", block);
        }
    }
}
