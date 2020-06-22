package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyMintServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyMintTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyMintServiceTest {

    @Mock
    private CurrencyMintTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private AccountCurrencyService accountCurrencyService;
    @Mock
    private CurrencyService currencyService;

    private CurrencyMintService service;
    private CurrencyMintTestData td;
    private AccountTestData accountTestData;
    private BlockTestData blockTestData;

    @BeforeEach
    void setUp() {
        td = new CurrencyMintTestData();
        service = spy(new CurrencyMintServiceImpl(
            table, blockChainInfoService, accountCurrencyService, currencyService));
    }

    @Disabled
    void mintCurrency() {
        accountTestData = new AccountTestData();
        //GIVEN
        MonetarySystemCurrencyMinting attachment = mock(MonetarySystemCurrencyMinting.class);
        doReturn(100L).when(attachment).getCounter();
        Account account = mock(Account.class);
        doReturn(accountTestData.ACC_4.getId()).when(account).getId();
        doReturn(td.CURRENCY_MINT_3).when(table).get(any(DbKey.class));
        LedgerEvent ledgerEvent = mock(LedgerEvent.class);

        //WHEN
        service.mintCurrency(ledgerEvent, td.CURRENCY_MINT_4.getCurrencyId(), account, attachment);

        //THEN
        verify(table).get(any(DbKey.class));
    }

    @Test
    void getCounter() {
        //GIVEN
        doReturn(td.CURRENCY_MINT_4).when(table).get(any(DbKey.class));
        //WHEN
        long result = service.getCounter(anyLong(), anyLong());
        assertEquals(td.CURRENCY_MINT_4.getCounter(), result);
        //THEN
        verify(table).get(any(DbKey.class));
    }

    @Disabled
    void deleteCurrency() {
        blockTestData = new BlockTestData();
        //GIVEN
        DbIterator<CurrencyMint> dbIt = mock(DbIterator.class);
        doReturn(true).doReturn(true).when(dbIt).hasNext();
        doReturn(td.CURRENCY_MINT_3).doReturn(td.CURRENCY_MINT_2).when(dbIt).next();
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        doReturn(blockTestData.BLOCK_10).when(blockChainInfoService).getLastBlock();
        Currency currency = mock(Currency.class);
        doReturn(100L).when(currency).getId();

        //WHEN
        service.deleteCurrency(currency);

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
    }
}