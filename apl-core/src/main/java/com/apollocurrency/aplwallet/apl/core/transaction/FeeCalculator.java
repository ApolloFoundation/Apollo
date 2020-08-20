package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionType.lookupBlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;

import javax.inject.Singleton;

@Singleton
public class FeeCalculator {
    public long getMinimumFeeATM(Transaction transaction, int blockchainHeight) {
        long totalFee = 0;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            appendage.loadPrunable(transaction);
            if (blockchainHeight < appendage.getBaselineFeeHeight()) {
                return 0; // No need to validate fees before baseline block
            }
            Fee fee = blockchainHeight >= appendage.getNextFeeHeight() ? appendage.getNextFee(transaction) : appendage.getBaselineFee(transaction);
            totalFee = Math.addExact(totalFee, fee.getFee(transaction, appendage));
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            totalFee = Math.addExact(totalFee, lookupBlockchainConfig().getOneAPL());
        }
        return totalFee;
    }
}
