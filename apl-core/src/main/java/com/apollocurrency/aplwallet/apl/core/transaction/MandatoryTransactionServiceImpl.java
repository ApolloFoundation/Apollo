package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@Slf4j
public class MandatoryTransactionServiceImpl implements MandatoryTransactionService {
    private static final int INITIAL_DELAY = 2 * 60 * 1000; // 2 min in ms
    private static final int REPEAT_DELAY = 30 * 60 * 1000; // 30 min in ms
    private static final int TX_SELECT_SIZE = 50;
    private TransactionProcessor txProcessor;
    private TransactionValidator txValidator;
    private TaskDispatchManager taskManager;
    private MandatoryTransactionDao dao;
    private Blockchain blockchain;

    @Inject
    public MandatoryTransactionServiceImpl(TransactionProcessor txProcessor, TransactionValidator txValidator, TaskDispatchManager taskManager, MandatoryTransactionDao dao, Blockchain blockchain) {
        this.txProcessor = txProcessor;
        this.txValidator = txValidator;
        this.taskManager = taskManager;
        this.dao = dao;
        this.blockchain = blockchain;
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
            List<MandatoryTransaction> all = dao.getAll(dbId, TX_SELECT_SIZE);
            for (MandatoryTransaction currentTx : all) {
                try {
                    dbId = currentTx.getDbEntryId();
                    boolean expired = blockchain.isExpired(currentTx);
                    boolean confirmed = blockchain.hasTransaction(currentTx.getId());
                    if (!expired) {
                        if (!confirmed) {
                            byte[] requiredTxHash = currentTx.getRequiredTxHash();
                            MandatoryTransaction prevRequiredTx = null;
                            boolean brodcast = true; // brodcast current tx
                            while (requiredTxHash != null) {
                                long id = Convert.fullHashToId(requiredTxHash);
                                MandatoryTransaction requiredTx = dao.get(id);
                                boolean requiredTxConfirmed = blockchain.hasTransaction(requiredTx.getId());
                                if (requiredTxConfirmed) {
                                    if (prevRequiredTx != null) {
                                        validateAndBroadcast(prevRequiredTx.getTransaction());
                                        brodcast = false;
                                    }
                                    break;
                                } else if (requiredTx.getRequiredTxHash() == null) {
                                    validateAndBroadcast(requiredTx.getTransaction());
                                    break;
                                }
                                prevRequiredTx = requiredTx;
                                requiredTxHash = requiredTx.getRequiredTxHash();
                            }
                            if (brodcast) {
                                validateAndBroadcast(currentTx.getTransaction());
                            }
                        }
                    } else {
                        dao.delete(currentTx.getId());
                    }
                } catch (Throwable e) {
                    log.warn("Unable to brodcast mandatory tx {}, reason - {}", currentTx.getId(), e.getMessage());
                }
            }
            if (all.size() < TX_SELECT_SIZE) {
                break;
            }
        }
        log.debug("Finish processing of mandatory txs in {} ms", System.currentTimeMillis() - startTime);
    }

    private void validateAndBroadcast(Transaction tx) throws AplException.ValidationException {
        txValidator.validate(tx);
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
        return dao.getAll(from, limit);
    }

    @Override
    @Transactional
    public void add(Transaction tx, byte[] requiredTxHash) {
        dao.insert(new MandatoryTransaction(tx, requiredTxHash, null));
    }
}
