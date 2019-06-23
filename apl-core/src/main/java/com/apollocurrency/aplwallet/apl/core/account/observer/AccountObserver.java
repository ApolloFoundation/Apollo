package com.apollocurrency.aplwallet.apl.core.account.observer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublickKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import lombok.Setter;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseService.leaseListeners;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountObserver {

    @Inject @Setter
    private AccountService accountService;
    @Inject @Setter
    private AccountLeaseService accountLeaseService;
    @Inject @Setter
    private AccountPublickKeyService accountPublickKeyService;

    public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        if (accountPublickKeyService.getPublicKeyCache() != null) {
            accountPublickKeyService.getPublicKeyCache().clear();
        }
    }

    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        if (accountPublickKeyService.getPublicKeyCache() != null) {
            accountPublickKeyService.getPublicKeyCache().remove(AccountTable.newKey(block.getGeneratorId()));
            block.getTransactions().forEach(transaction -> {
                accountPublickKeyService.getPublicKeyCache().remove(AccountTable.newKey(transaction.getSenderId()));
                if (!transaction.getAppendages(appendix -> (appendix instanceof PublicKeyAnnouncementAppendix), false).isEmpty()) {
                    accountPublickKeyService.getPublicKeyCache().remove(AccountTable.newKey(transaction.getRecipientId()));
                }
                if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                    ShufflingRecipientsAttachment shufflingRecipients = (ShufflingRecipientsAttachment) transaction.getAttachment();
                    for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                        accountPublickKeyService.getPublicKeyCache().remove(AccountTable.newKey(Account.getId(publicKey)));
                    }
                }
            });
        }
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        int height = block.getHeight();
        List<AccountLease> changingLeases = accountLeaseService.getLeaseChangingAccounts(height);
        for (AccountLease lease : changingLeases) {
            AccountEntity lessor = accountService.getAccountEntity(lease.lessorId);
            if (height == lease.currentLeasingHeightFrom) {
                lessor.setActiveLesseeId(lease.currentLesseeId);
                leaseListeners.notify(lease, Account.Event.LEASE_STARTED);
            } else if (height == lease.currentLeasingHeightTo) {
                leaseListeners.notify(lease, Account.Event.LEASE_ENDED);
                lessor.setActiveLesseeId(0);
                if (lease.nextLeasingHeightFrom == 0) {
                    lease.currentLeasingHeightFrom = 0;
                    lease.currentLeasingHeightTo = 0;
                    lease.currentLesseeId = 0;
                    AccountLeaseTable.getInstance().delete(lease);
                } else {
                    lease.currentLeasingHeightFrom = lease.nextLeasingHeightFrom;
                    lease.currentLeasingHeightTo = lease.nextLeasingHeightTo;
                    lease.currentLesseeId = lease.nextLesseeId;
                    lease.nextLeasingHeightFrom = 0;
                    lease.nextLeasingHeightTo = 0;
                    lease.nextLesseeId = 0;
                    AccountLeaseTable.getInstance().insert(lease);
                    if (height == lease.currentLeasingHeightFrom) {
                        lessor.setActiveLesseeId(lease.currentLesseeId);
                        leaseListeners.notify(lease, Account.Event.LEASE_STARTED);
                    }
                }
            }
            accountService.save(lessor);
        }
    }

}
