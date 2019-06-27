package com.apollocurrency.aplwallet.apl.core.account.observer;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author al
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountObserver {

    private AccountService accountService;

    private AccountLeaseService accountLeaseService;

    private AccountPublicKeyService accountPublicKeyService;

    private Event<AccountLease> accountLeaseEvent;

    @Inject
    public AccountObserver(AccountService accountService,
                           AccountLeaseService accountLeaseService,
                           AccountPublicKeyService accountPublicKeyService,
                           Event<AccountLease> accountLeaseEvent) {
        this.accountService = accountService;
        this.accountLeaseService = accountLeaseService;
        this.accountPublicKeyService = accountPublicKeyService;
        this.accountLeaseEvent = accountLeaseEvent;
    }

    public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        if (accountPublicKeyService.getPublicKeyCache() != null) {
            accountPublicKeyService.getPublicKeyCache().clear();
        }
    }

    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        if (accountPublicKeyService.getPublicKeyCache() != null) {
            accountPublicKeyService.getPublicKeyCache().remove(AccountTable.newKey(block.getGeneratorId()));
            block.getTransactions().forEach(transaction -> {
                accountPublicKeyService.getPublicKeyCache().remove(AccountTable.newKey(transaction.getSenderId()));
                if (!transaction.getAppendages(appendix -> (appendix instanceof PublicKeyAnnouncementAppendix), false).isEmpty()) {
                    accountPublicKeyService.getPublicKeyCache().remove(AccountTable.newKey(transaction.getRecipientId()));
                }
                if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                    ShufflingRecipientsAttachment shufflingRecipients = (ShufflingRecipientsAttachment) transaction.getAttachment();
                    for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                        accountPublicKeyService.getPublicKeyCache().remove(AccountTable.newKey(AccountService.getId(publicKey)));
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
                //leaseListeners.notify(lease, AccountEventType.LEASE_STARTED);
                accountLeaseEvent.select(literal(AccountEventType.LEASE_STARTED)).fire(lease);
            } else if (height == lease.currentLeasingHeightTo) {
                //leaseListeners.notify(lease, AccountEventType.LEASE_ENDED);
                accountLeaseEvent.select(literal(AccountEventType.LEASE_ENDED)).fire(lease);
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
                        //leaseListeners.notify(lease, AccountEventType.LEASE_STARTED);
                        accountLeaseEvent.select(literal(AccountEventType.LEASE_STARTED)).fire(lease);
                    }
                }
            }
            accountService.save(lessor);
        }
    }

}
