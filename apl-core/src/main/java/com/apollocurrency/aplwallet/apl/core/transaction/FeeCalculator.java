package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.env.config.FeeRate;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class FeeCalculator {
    private final PrunableLoadingService prunableService;

    @Inject
    public FeeCalculator(PrunableLoadingService prunableService) {
        this.prunableService = prunableService;
    }


    private final BlockchainConfig blockchainConfig;

    @Inject
    public FeeCalculator(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    @TransactionFee(FeeMarker.CALCULATOR)
    public long getMinimumFeeATM(Transaction transaction, int blockchainHeight) {
        long totalFee = 0;
        short feeRate;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
            Fee fee = appendage.getBaselineFee(transaction);
            totalFee = Math.addExact(totalFee, fee.getFee(transaction, appendage));
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            totalFee = Math.addExact(totalFee, Constants.ONE_APL);
        }
        HeightConfig heightConfig = blockchainConfig.getConfigAtHeight(blockchainHeight);
        if(heightConfig == null){
            String errMsg = "There is no blockchain config at height "+blockchainHeight;
            log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }else {
            feeRate = heightConfig.getFeeRate(transaction.getType().getType(), transaction.getType().getSubtype());
            if(log.isTraceEnabled()){
                log.trace("Calculate fee for tx type={} subtype={} at height={} totalFee={} * {} / 100 = {}",
                    transaction.getType().getType(), transaction.getType().getSubtype(), blockchainHeight,
                    totalFee, feeRate, totalFee * feeRate / FeeRate.RATE_DIVIDER);
            }
            totalFee = totalFee * feeRate / FeeRate.RATE_DIVIDER;
        }
        return totalFee;
    }
}
