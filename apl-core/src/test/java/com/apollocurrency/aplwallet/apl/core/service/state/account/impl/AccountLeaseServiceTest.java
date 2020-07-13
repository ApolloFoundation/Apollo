/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.model.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountLease;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;

import static com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventBinding.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountLeaseServiceTest {

    @Mock
    private Blockchain blockchain;
    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private AccountLeaseTable accountLeaseTable;
    @Mock
    private Event leaseEvent;

    private AccountLeaseService accountLeaseService;
    private AccountTestData testData;

    private int leasingDelay = 100;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountLeaseService = spy(new AccountLeaseServiceImpl(
            accountLeaseTable, blockchain, blockchainConfig, leaseEvent));
    }

    @Test
    void leaseEffectiveBalance1() {
        check_leaseEffectiveBalance(1,
            testData.ACC_1,
            testData.ACC_LEAS_0,
            testData.ACC_LEAS_0.getCurrentLeasingHeightTo() - leasingDelay - 1,
            1500
        );
    }

    @Test
    void leaseEffectiveBalance2() {
        check_leaseEffectiveBalance(2,
            testData.ACC_1,
            testData.ACC_LEAS_0,
            testData.ACC_LEAS_0.getCurrentLeasingHeightTo() + 1,
            1500
        );
    }

    @Test
    void leaseEffectiveBalance_newLease() {
        check_leaseEffectiveBalance(2,
            testData.ACC_1,
            null,
            testData.ACC_LEAS_0.getCurrentLeasingHeightTo() + 1,
            1500
        );
    }

    private void check_leaseEffectiveBalance(long txId, Account account, AccountLease accountLeaseFromTable, int height, int period) {
        long lesseeId = -1;

        long expectedFrom = height + leasingDelay;
        long expectedTo = expectedFrom + period;
        AccountLease accountLease = null;

        if (accountLeaseFromTable == null) {
            lesseeId = account.getId();
            accountLease = new AccountLease(account.getId(),
                height + leasingDelay,
                height + leasingDelay + period,
                lesseeId, height);
        } else {
            accountLease = accountLeaseFromTable;
            lesseeId = accountLease.getCurrentLesseeId();
            if (expectedFrom < accountLease.getCurrentLeasingHeightTo()) {
                expectedFrom = accountLease.getCurrentLeasingHeightTo();
                expectedTo = expectedFrom + period;
            }
        }

        Event firedEvent = mock(Event.class);

        doReturn(height).when(blockchain).getHeight();
        doReturn(leasingDelay).when(blockchainConfig).getLeasingDelay();
        doReturn(firedEvent).when(leaseEvent).select(literal(AccountEventType.LEASE_SCHEDULED));
        doReturn(accountLeaseFromTable).when(accountLeaseTable).get(any(DbKey.class));

        accountLeaseService.leaseEffectiveBalance(account, lesseeId, period);

        if (accountLeaseFromTable == null) {
            assertEquals(expectedFrom, accountLease.getCurrentLeasingHeightFrom());
            assertEquals(expectedTo, accountLease.getCurrentLeasingHeightTo());
            assertEquals(lesseeId, accountLease.getCurrentLesseeId());
        } else {
            assertEquals(expectedFrom, accountLease.getNextLeasingHeightFrom());
            assertEquals(expectedTo, accountLease.getNextLeasingHeightTo());
            assertEquals(lesseeId, accountLease.getNextLesseeId());
        }
        verify(accountLeaseTable).insert(accountLease);
        verify(firedEvent).fire(accountLease);
    }


}