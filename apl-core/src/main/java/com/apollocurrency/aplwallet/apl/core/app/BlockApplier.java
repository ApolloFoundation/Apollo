/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class BlockApplier {
    @Inject
    @Setter
    private Blockchain blockchain;
    @Inject
    @Setter
    private ShardDao shardDao;

    public void apply(Block block) {
        Account generatorAccount = Account.addOrGetAccount(block.getGeneratorId());
        generatorAccount.apply(block.getGeneratorPublicKey());
        long totalBackFees = 0;
        int height = block.getHeight();
        if (height > 3) {
            long[] backFees = new long[3];
            for (Transaction transaction : block.getTransactions()) {
                long[] fees = ((TransactionImpl)transaction).getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            long[] generators = null;
            boolean isShardBlock = blockchain.getShardInitialBlock().getHeight() != 0;
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                if (generators == null && isShardBlock) {
                    generators = shardDao.getLastShard().getGeneratorIds();
                }
                Account previousGeneratorAccount;
                if (!isShardBlock) {
                    previousGeneratorAccount = Account.getAccount(blockchain.getBlockAtHeight(height - i - 1).getGeneratorId());
                } else {
                    previousGeneratorAccount = Account.getAccount(generators[i]);
                }
                log.trace("Back fees {} to forger at height {}", ((double)backFees[i])/ Constants.ONE_APL,
                        height - i - 1);
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, block.getId(), backFees[i]);
                previousGeneratorAccount.addToForgedBalanceATM(backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            log.trace("Fee reduced by {} at height {}", ((double)totalBackFees)/Constants.ONE_APL, height);
        }
        generatorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, block.getId(), block.getTotalFeeATM() - totalBackFees);
        generatorAccount.addToForgedBalanceATM(block.getTotalFeeATM() - totalBackFees);
    }

}
