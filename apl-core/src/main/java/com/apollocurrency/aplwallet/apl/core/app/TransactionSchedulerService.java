/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionSchedulerService {
    private static final Logger LOG = getLogger(TransactionSchedulerService.class);

    private final Map<Transaction, TransactionScheduler> transactionSchedulers = new ConcurrentHashMap<>();

    private TransactionProcessor transactionProcessor;
    private TimeService timeService;

    @Inject
    public TransactionSchedulerService(TransactionProcessor transactionProcessor, TimeService timeService) {
        this.transactionProcessor = transactionProcessor;
        this.timeService = timeService;
    }

    public void schedule(Filter<Transaction> filter, Transaction transaction) {
        if (transactionSchedulers.size() >= 100) {
            throw new RuntimeException("Cannot schedule more than 100 transactions! Please restart your node if you want to clear existing scheduled transactions.");
        }
        TransactionScheduler transactionScheduler = new TransactionScheduler(transaction, filter);
        transactionSchedulers.put(transaction, transactionScheduler);
    }

    public List<Transaction> getScheduledTransactions(long accountId) {
        ArrayList<Transaction> list = new ArrayList<>();
        for (Transaction transaction : transactionSchedulers.keySet()) {
            if (accountId == 0 || transaction.getSenderId() == accountId) {
                list.add(transaction);
            }
        }
        return list;
    }

    public Transaction deleteScheduledTransaction(long transactionId) {
        Iterator<Transaction> iterator = transactionSchedulers.keySet().iterator();
        while (iterator.hasNext()) {
            Transaction transaction = iterator.next();
            if (transaction.getId() == transactionId) {
                iterator.remove();
                return transaction;
            }
        }
        return null;
    }


    public void onUnconfirmedTransactionsAdded(@Observes @TxEvent(TxEventType.ADDED_UNCONFIRMED_TRANSACTIONS) List<Transaction> transactions) {
        Iterator<Map.Entry<Transaction, TransactionScheduler>> iterator = transactionSchedulers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Transaction, TransactionScheduler> entry = iterator.next();
            Transaction scheduledTransaction = entry.getKey();
            TransactionScheduler transactionScheduler = entry.getValue();
            for (Transaction transaction : transactions) {
                if (processEvent(transactionScheduler, transaction)) {
                    iterator.remove();
                    LOG.info("Removed " + scheduledTransaction.getStringId() + " from transaction scheduler");
                    break;
                }
            }
        }
    }

    private boolean processEvent(TransactionScheduler scheduler, Transaction unconfirmedTransaction) {
        Transaction transaction = scheduler.getTransaction();
        if (transaction.getExpiration() < timeService.getEpochTime()) {
            LOG.info("Expired transaction in transaction scheduler " + transaction.getSenderId());
            return true;
        }
        if (!scheduler.getFilter().test(unconfirmedTransaction)) {
            return false;
        }
        try {
            transactionProcessor.broadcast(transaction);
            return true;
        } catch (AplException.ValidationException e) {
            LOG.info("Failed to broadcast: " + e.getMessage());
            return true;
        }
    }

}
