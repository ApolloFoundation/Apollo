/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.model.AplQueryObject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface FindTransactionService {

    Stream<UnconfirmedTransaction> getAllUnconfirmedTransactionsStream();

    /**
     * Returns transaction given the transaction id up to the specified height.
     * Looks for transaction in the transaction table.
     *
     * @param transactionId the transaction id
     * @param height        the blockchain height (optional)
     * @return the transaction object
     */
    Optional<Transaction> findTransaction(long transactionId, int height);

    Optional<Transaction> findUnconfirmedTransaction(long transactionId);

    /**
     * Returns list of transactions saved in the blockchain given query object
     *
     * @param query the transaction filter object
     * @return the list of transaction objects
     */
    List<TxReceipt> getConfirmedTransactionsByQuery(AplQueryObject query);

    List<TxReceipt> getTransactionsByQuery(AplQueryObject query, boolean includeUnconfirmed);

    /**
     * Returns count of transactions saved in the blockchain given query object
     *
     * @param query the transaction filter object
     * @return the list of transaction objects
     */
    long getConfirmedTransactionsCountByQuery(AplQueryObject query);

    long getTransactionsCountByQuery(AplQueryObject query, boolean includeUnconfirmed);
}
