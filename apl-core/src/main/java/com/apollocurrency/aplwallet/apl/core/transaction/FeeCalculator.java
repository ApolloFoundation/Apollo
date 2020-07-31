package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.util.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeeCalculator {
    private final PrunableLoadingService prunableService;

    @Inject
    public FeeCalculator(PrunableLoadingService prunableService) {
        this.prunableService = prunableService;
    }

    public long getMinimumFeeATM(Transaction transaction, int blockchainHeight) {
        long totalFee = 0;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
            Fee fee = appendage.getBaselineFee(transaction);
            totalFee = Math.addExact(totalFee, fee.getFee(transaction, appendage));
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            totalFee = Math.addExact(totalFee, Constants.ONE_APL);
        }
        return totalFee;
    }
}
