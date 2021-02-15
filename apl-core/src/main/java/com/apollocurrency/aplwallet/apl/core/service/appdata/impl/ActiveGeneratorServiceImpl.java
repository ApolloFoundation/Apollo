/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ActiveGenerator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.ActiveGeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class ActiveGeneratorServiceImpl implements ActiveGeneratorService {

    private final int MAX_TRACKED_GENERATORS = 50;
    /**
     * Active block generators
     */
    private final Set<Long> activeGeneratorIds = new HashSet<>();
    /**
     * Sorted list of generators for the next block
     */
    private final List<ActiveGenerator> activeGenerators = new ArrayList<>();
    private Blockchain blockchain;
    private AccountService accountService;
    private GeneratorService generatorService;

    /**
     * Active block identifier
     */
    private long activeBlockId;
    /**
     * Generator list has been initialized
     */
    private boolean generatorsInitialized = false;

    @Inject
    public ActiveGeneratorServiceImpl(Blockchain blockchain, AccountService accountService, GeneratorService generatorService) {
        this.blockchain = blockchain;
        this.accountService = accountService;
        this.generatorService = generatorService;
    }


    @PostConstruct
    public void init() {
        if (!generatorsInitialized) {
            activeGeneratorIds.addAll(blockchain.getBlockGenerators(MAX_TRACKED_GENERATORS));
            activeGeneratorIds.forEach(activeGeneratorId -> activeGenerators.add(new ActiveGenerator(activeGeneratorId)));
            log.debug(activeGeneratorIds.size() + " block generators found");
            generatorsInitialized = true;
        } else {
            throw new IllegalStateException("Active generators already initialized");
        }
    }

    /**
     * Return a list of generators for the next block.  The caller must hold the blockchain
     * read lock to ensure the integrity of the returned list.
     *
     * @return List of generator account identifiers
     */
    @Override
    public synchronized List<ActiveGenerator> getNextGenerators() {
        List<ActiveGenerator> generatorList;
        if (!generatorsInitialized) {
            throw new IllegalStateException("Active generators not yet initialized");
        }
        long blockId = blockchain.getLastBlock().getId();
        if (blockId != activeBlockId) {
            activeBlockId = blockId;
            for (ActiveGenerator generator : activeGenerators) {
                this.setGeneratorFiedls(generator);
            }
            Collections.sort(activeGenerators);
        }
        generatorList = new ArrayList<>(activeGenerators);
        return generatorList;

    }

    public synchronized void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        long generatorId = block.getGeneratorId();
        synchronized (activeGenerators) {
            if (!activeGeneratorIds.contains(generatorId)) {
                activeGeneratorIds.add(generatorId);
                activeGenerators.add(new ActiveGenerator(generatorId));
            }
        }
    }

    private void setGeneratorFiedls(ActiveGenerator generator) {
        Block lastBlock = blockchain.getLastBlock();
        int height = lastBlock.getHeight();

        if (generator.getPublicKey() == null) {
            byte[] publicKey = accountService.getPublicKeyByteArray(generator.getAccountId());
            if (publicKey == null) {
                generator.setHitTime(Long.MAX_VALUE); // set hit time
                return;
            } else {
                generator.setPublicKey(publicKey); // set publicKey
            }
        }
        Account account = accountService.getAccount(generator.getAccountId(), height);
        if (account == null) {
            generator.setHitTime(Long.MAX_VALUE); // set hit time
            return;
        }
        long effectiveBalanceAPL = Math.max(accountService.getEffectiveBalanceAPL(account, height, true), 0);
        generator.setEffectiveBalanceAPL(effectiveBalanceAPL); // set effective ballance
        if (effectiveBalanceAPL == 0) {
            generator.setHitTime(Long.MAX_VALUE); // set hit time
            return;
        }
        BigInteger effectiveBalance = BigInteger.valueOf(effectiveBalanceAPL);
        BigInteger hit = generatorService.getHit(generator.getPublicKey(), lastBlock);
        long hitTime = generatorService.getHitTime(effectiveBalance, hit, lastBlock);
        generator.setHitTime(hitTime); // set hit time
    }

}