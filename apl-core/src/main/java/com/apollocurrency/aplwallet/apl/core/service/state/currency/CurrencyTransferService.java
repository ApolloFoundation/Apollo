/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;

import java.util.stream.Stream;

public interface CurrencyTransferService {

    DbIterator<CurrencyTransfer> getAllTransfers(int from, int to);

    Stream<CurrencyTransfer> getAllTransfersStream(int from, int to);

    int getCount();

    DbIterator<CurrencyTransfer> getCurrencyTransfers(long currencyId, int from, int to);

    Stream<CurrencyTransfer> getCurrencyTransfersStream(long currencyId, int from, int to);

    DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, int from, int to);

    Stream<CurrencyTransfer> getAccountCurrencyTransfersStream(long accountId, int from, int to);

    DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, long currencyId, int from, int to);

    Stream<CurrencyTransfer> getAccountCurrencyTransfersStream(long accountId, long currencyId, int from, int to);

    int getTransferCount(long currencyId);

    CurrencyTransfer addTransfer(Transaction transaction, MonetarySystemCurrencyTransfer attachment);

}
