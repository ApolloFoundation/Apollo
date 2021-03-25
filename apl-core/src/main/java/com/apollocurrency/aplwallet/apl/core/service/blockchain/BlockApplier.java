/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class BlockApplier {

    private final Blockchain blockchain;
    private final ShardDao shardDao;
    private final AccountService accountService;
    private final AccountPublicKeyService accountPublicKeyService;

    @Inject
    public BlockApplier(Blockchain blockchain, ShardDao shardDao, AccountService accountService, AccountPublicKeyService accountPublicKeyService) {
        this.blockchain = blockchain;
        this.shardDao = shardDao;
        this.accountService = accountService;
        this.accountPublicKeyService = accountPublicKeyService;
    }

    public void apply(Block block) {
        long totalBackFees = 0;
        int height = block.getHeight();
        if (height > 3) {
            long[] backFees = new long[3];
            for (Transaction transaction : blockchain.getOrLoadTransactions(block)) {
                long[] fees = transaction.getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            int shardHeight = blockchain.getShardInitialBlock().getHeight();
            long[] generators = null;
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                int blockHeight = block.getHeight() - i - 1;
                if (generators == null && shardHeight > blockHeight) {
                    generators = shardDao.getLastShard().getGeneratorIds();
                }
                Account previousGeneratorAccount;
                if (shardHeight > blockHeight) {
                    int index = shardHeight - blockHeight - 1;
                    previousGeneratorAccount = accountService.getAccount(generators[index]);
                } else {
                    previousGeneratorAccount = accountService.getAccount(blockchain.getBlockAtHeight(blockHeight).getGeneratorId());
                }
                log.debug("Back transaction fees {} ATM to forger {} at height {}",
                    backFees[i],
                    Convert2.rsAccount(previousGeneratorAccount.getId()),
                    height - i - 1);
                accountService.addToBalanceAndUnconfirmedBalanceATM(previousGeneratorAccount, LedgerEvent.BLOCK_GENERATED, block.getId(), backFees[i]);
                accountService.addToForgedBalanceATM(previousGeneratorAccount, backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            log.trace("Fee reduced by {} ATM at height {}", totalBackFees, height);
        }
        //fetch account after a possible change in previousGeneratorAccount
        Account account = accountService.createAccount(block.getGeneratorId());
        accountPublicKeyService.apply(account, block.getGeneratorPublicKey());
        @TransactionFee(FeeMarker.FORGER_FEE)
        long amountATM = block.getTotalFeeATM() - totalBackFees;
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, LedgerEvent.BLOCK_GENERATED, block.getId(), amountATM);
        accountService.addToForgedBalanceATM(account, amountATM);
        log.debug("Transaction fee {} ATM awarded to forger {} at height {}",
            block.getTotalFeeATM() - totalBackFees,
            Convert2.rsAccount(account.getId()),
            height);
    }

}
