/*
 * Copyright © 2019-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.converter.db.MandatoryTransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.MandatoryTransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class MandatoryTransactionServiceImpl implements MandatoryTransactionService {
    private static final int INITIAL_DELAY = 2 * 60 * 1000; // 2 min in ms
    private static final int REPEAT_DELAY = 30 * 60 * 1000; // 30 min in ms
    private static final int TX_SELECT_SIZE = 50;
    private final TransactionProcessor txProcessor;
    private final TransactionValidator txValidator;
    private final TaskDispatchManager taskManager;
    private final MandatoryTransactionDao dao;
    private final Blockchain blockchain;
    private final MandatoryTransactionEntityToModelConverter toModelConverter;
    private final MandatoryTransactionModelToEntityConverter toEntityConverter;

    @Inject
    public MandatoryTransactionServiceImpl(TransactionProcessor txProcessor, TransactionValidator txValidator, TaskDispatchManager taskManager, MandatoryTransactionDao dao, Blockchain blockchain, MandatoryTransactionEntityToModelConverter toModelConverter, MandatoryTransactionModelToEntityConverter toEntityConverter) {
        this.txProcessor = txProcessor;
        this.txValidator = txValidator;
        this.taskManager = taskManager;
        this.dao = dao;
        this.blockchain = blockchain;
        this.toModelConverter = toModelConverter;
        this.toEntityConverter = toEntityConverter;
    }

    @PostConstruct
    public void init() {
        taskManager.newScheduledDispatcher("MandatoryTransactionService")
            .schedule(Task.builder()
                .name("RebroadcastMandatoryTxs")
                .initialDelay(INITIAL_DELAY)
                .task(this::processMandatoryTransactions)
                .delay(REPEAT_DELAY)
                .build());
    }

    @Transactional
    void processMandatoryTransactions() {
        log.debug("Start to process mandatory txs");
        long startTime = System.currentTimeMillis();
        long dbId = 0;
        while (true) {
            List<MandatoryTransactionEntity> all = dao.getAll(dbId, TX_SELECT_SIZE);
            for (MandatoryTransactionEntity currentTxEntity : all) {
                try {
                    dbId = currentTxEntity.getDbId();
                    Transaction currentTx = toModelConverter.convert(currentTxEntity).getTransactionImpl();
                    boolean expired = blockchain.isExpired(currentTx);
                    boolean confirmed = blockchain.hasTransaction(currentTxEntity.getId());
                    if (!expired) {
                        if (!confirmed) {
                            byte[] requiredTxHash = currentTxEntity.getRequiredTxHash();
                            MandatoryTransactionEntity prevRequiredTxEntity = null;
                            boolean brodcast = true; // broadcast current tx
                            while (requiredTxHash != null) {
                                long id = Convert.transactionFullHashToId(requiredTxHash);
                                MandatoryTransactionEntity requiredTxEntity = dao.get(id);
                                boolean requiredTxConfirmed = blockchain.hasTransaction(requiredTxEntity.getId());
                                if (requiredTxConfirmed) {
                                    if (prevRequiredTxEntity != null) {
                                        validateAndBroadcast(toModelConverter.convert(prevRequiredTxEntity).getTransactionImpl());
                                        brodcast = false;
                                    }
                                    break;
                                } else if (requiredTxEntity.getRequiredTxHash() == null) {
                                    validateAndBroadcast(toModelConverter.convert(requiredTxEntity).getTransactionImpl());
                                    break;
                                }
                                prevRequiredTxEntity = requiredTxEntity;
                                requiredTxHash = requiredTxEntity.getRequiredTxHash();
                            }
                            if (brodcast) {
                                validateAndBroadcast(currentTx);
                            }
                        }
                    } else {
                        dao.delete(currentTxEntity.getId());
                    }
                } catch (Throwable e) {
                    log.warn("Unable to brodcast mandatory tx {}, reason - {}", currentTxEntity.getId(), e.getMessage());
                }
            }
            if (all.size() < TX_SELECT_SIZE) {
                break;
            }
        }
        log.debug("Finish processing of mandatory txs in {} ms", System.currentTimeMillis() - startTime);
    }

    private void validateAndBroadcast(Transaction tx) {
        txValidator.validateFully(tx);
        txProcessor.broadcast(tx);
    }

    @Transactional
    @Override
    public int clearAll() {
        int deleted = dao.deleteAll();
        log.debug("Deleted {} mandatory txs", deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        return dao.delete(id) > 0;
    }

    @Override
    public List<MandatoryTransaction> getAll(long from, int limit) {
        return dao.getAll(from, limit).stream().map(toModelConverter).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveMandatoryTransaction(Transaction tx, byte[] requiredTxHash) {
        MandatoryTransactionEntity entity = toEntityConverter.convert(
            new MandatoryTransaction(tx, requiredTxHash)
        );
        dao.insert(entity);
    }
}
