/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface FindTransactionService {

    Stream<UnconfirmedTransaction> getAllUnconfirmedTransactionsStream();

    long getAllUnconfirmedTransactionsCount();

    /**
     * Returns transaction given the transaction id up to the specified height.
     * Looks for transactions in both transaction table in the unconfirmed and confirmed.
     *
     * @param transactionId the transaction id
     * @param height        the blockchain height (optional)
     * @return the transaction object
     */
    Optional<Transaction> findTransaction(long transactionId, int height);

    Optional<Transaction> findUnconfirmedTransaction(long transactionId);

    /**
     * Returns list of transactions saved in the blockchain during the time interval
     *
     * @param timeStart the start of time interval
     * @param timeEnd   the end of time interval
     * @return the list of transaction objects
     */
    List<TxReceipt> getTransactionsByPeriod(int timeStart, int timeEnd);

    /**
     * Returns count of transactions saved in the blockchain during the time interval
     *
     * @param timeStart the start of time interval
     * @param timeEnd   the end of time interval
     * @return the list of transaction objects
     */
    long getTransactionsCountByPeriod(int timeStart, int timeEnd);
}
