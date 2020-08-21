/*
 * Copyright © 2018-2019 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractBlockValidator implements BlockValidator {
    private static final Logger LOG = getLogger(AbstractBlockValidator.class);
    protected BlockchainConfig blockchainConfig;
    protected Blockchain blockchain;
    protected AccountService accountService;
    private static GeneratorService generatorService;
    protected final BlockSerializer blockSerializer;

    @Inject
    public AbstractBlockValidator(Blockchain blockchain,
                                  BlockchainConfig blockchainConfig,
                                  AccountService accountService,
                                  GeneratorService generatorService,
                                  BlockSerializer blockSerializer) {
        Objects.requireNonNull(blockchain, "Blockchain is null");
        Objects.requireNonNull(blockchainConfig, "Blockchain config is null");
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.accountService = accountService;
        this.generatorService = generatorService;
        this.blockSerializer = blockSerializer;
    }

    @Override
    public void validate(Block block, Block previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException {
        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
            throw new BlockchainProcessor.BlockOutOfOrderException(
                "Previous block id doesn't match", blockSerializer.getJSONObject(block));
        }
        if (block.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
            LOG.warn("Received block " + block.getStringId() + " from the future, timestamp " + block.getTimestamp()
                + " generator " + Long.toUnsignedString(block.getGeneratorId()) + " current time " + curTime + ", system clock may be off");
            throw new BlockchainProcessor.BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                + " current time is " + curTime, blockSerializer.getJSONObject(block));
        }
        if (block.getTimestamp() <= previousLastBlock.getTimestamp()) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Block timestamp " + block.getTimestamp() + " is before previous block timestamp "
                + previousLastBlock.getTimestamp(), blockSerializer.getJSONObject(block));
        }
        verifySignature(block);
        validatePreviousHash(block, previousLastBlock);
        if (block.getId() == 0L || blockchain.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Duplicate block or invalid id", blockSerializer.getJSONObject(block));
        }
        if (!verifyGenerationSignature(block)) {
            Account generatorAccount = accountService.getAccount(block.getGeneratorId());
            long generatorBalance = generatorAccount == null ? 0 : accountService.getEffectiveBalanceAPL(generatorAccount, blockchain.getHeight(), true);
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Generation signature verification failed, effective balance " + generatorBalance, blockSerializer.getJSONObject(block));
        }

        if (blockchain.getBlockTransactionCount(block.getId()) > blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Invalid block transaction count "
                + blockchain.getBlockTransactionCount(block.getId()), blockSerializer.getJSONObject(block));
        }
        if (block.getPayloadLength() > blockchainConfig.getCurrentConfig().getMaxPayloadLength() || block.getPayloadLength() < 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException(
                "Invalid block payload length " + block.getPayloadLength(), blockSerializer.getJSONObject(block));
        }
        switch (block.getVersion()) {
            case Block.LEGACY_BLOCK_VERSION:
                if (blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled()) {
                    throw new BlockchainProcessor.BlockNotAcceptedException(
                        "Legacy blocks are not accepting during adaptive forging", blockSerializer.getJSONObject(block));
                }
                break;
            case Block.INSTANT_BLOCK_VERSION:
                validateInstantBlock(block, previousLastBlock);
                break;
            case Block.ADAPTIVE_BLOCK_VERSION:
                validateAdaptiveBlock(block, previousLastBlock);
                break;
            case Block.REGULAR_BLOCK_VERSION:
                validateRegularBlock(block, previousLastBlock);
                break;
        }
    }

    private boolean verifyGenerationSignature(Block block) throws BlockchainProcessor.BlockOutOfOrderException {
        try {
            Block previousBlock = blockchain.getBlock(block.getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException(
                    "Can't verify signature because previous block is missing", blockSerializer.getJSONObject(block));
            }

            Account account = accountService.getAccount(block.getGeneratorId());
            long effectiveBalance = account == null ? 0 : accountService.getEffectiveBalanceAPL(account, blockchain.getHeight(), true);
            if (effectiveBalance <= 0) {
                LOG.warn("Account: {} Effective ballance: {}, blockchain.height: {},  verification failed",
                    account, effectiveBalance, blockchain.getHeight());
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            digest.update(previousBlock.getGenerationSignature());
            byte[] generatorPublicKey = block.getGeneratorPublicKey();
            if (generatorPublicKey == null) {
                generatorPublicKey = accountService.getPublicKeyByteArray(block.getGeneratorId());
                block.setGeneratorPublicKey(generatorPublicKey);
            }
            byte[] generationSignatureHash = digest.digest(block.getGeneratorPublicKey());
            if (!Arrays.equals(block.getGenerationSignature(), generationSignatureHash)) {
                LOG.warn("Account: {} Effective ballance: {},  gen. signature: {}, calculated: {}, blockchain.height: {}, verification failed",
                    account, effectiveBalance, block.getGenerationSignature(), generationSignatureHash, blockchain.getHeight());
                return false;
            }

            BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6],
                generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3],
                generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            boolean ret = generatorService.verifyHit(
                hit, BigInteger.valueOf(effectiveBalance), previousBlock,
                requireTimeout(block.getVersion()) ? block.getTimestamp() - block.getTimeout() : block.getTimestamp());
            if (!ret) {
                LOG.warn("Account: {} Effective balance: {}, blockchain.height: {}, Generator.verifyHit() verification failed",
                    account, effectiveBalance, blockchain.getHeight());
            }
            return ret;
        } catch (RuntimeException e) {
            LOG.info("Error verifying block generation signature", e);
            return false;
        }
    }

    private boolean requireTimeout(int version) {
        return Block.ADAPTIVE_BLOCK_VERSION == version || Block.INSTANT_BLOCK_VERSION == version;
    }


    abstract void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

}
