package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DGSObserverTest {

    @Mock
    DGSService dgsService;
    @Mock
    AccountService accountService;

    private DGSObserver observer;

    @BeforeEach
    void setUp() {
        this.observer = new DGSObserver(dgsService, accountService);
    }

    @Test
    void onBlockApplied_zeroHeight() {
        Block block = mock(Block.class);
        doReturn(0).when(block).getHeight();

        this.observer.onBlockApplied(block);

        verifyNoInteractions(dgsService);
        verifyNoInteractions(accountService);
    }

    @Test
    void onBlockApplied() {
        Block block = mock(Block.class);
        doReturn(1000).when(block).getHeight();
        DbIterator<DGSPurchase> iterator = mock(DbIterator.class);
        when(iterator.hasNext()).thenReturn(true).thenReturn(false);
        DGSPurchase purchase1 = new DGSPurchase(
            1L, 1000, 1L, 1L, 1L, 1L, 10, 120, 1100, null, 1000, false, null, false, null, false, false,
            List.of(), List.of(), 10L, 10L);
        when(iterator.next()).thenReturn(purchase1);

        Account account = new Account(1L, 1000);
        when(accountService.getAccount(1L)).thenReturn(account);
        doNothing().when(accountService).addToUnconfirmedBalanceATM(account, LedgerEvent.DIGITAL_GOODS_PURCHASE_EXPIRED,
            1L, 1200L);
        String[] parsedTags = {};
        DGSGoods dgsGoods = new DGSGoods(1L, 1000, 1L, 1L, "name", "descr", "tags",
            parsedTags, 1000, false, 10, 10L, false);
        when(dgsService.getGoods(1L)).thenReturn(dgsGoods);

        when(dgsService.getExpiredPendingPurchases(block)).thenReturn(iterator);
        doNothing().when(dgsService).changeQuantity(dgsGoods, 10);
        doNothing().when(dgsService).setPending(any(DGSPurchase.class), anyBoolean());

        this.observer.onBlockApplied(block);

        verify(dgsService).getExpiredPendingPurchases(any(Block.class));
        verify(accountService, times(1)).getAccount(anyLong());
        verify(accountService, times(1)).addToUnconfirmedBalanceATM(
            account, LedgerEvent.DIGITAL_GOODS_PURCHASE_EXPIRED,
            1L, 1200L);
        verify(dgsService).getGoods(1L);
    }
}