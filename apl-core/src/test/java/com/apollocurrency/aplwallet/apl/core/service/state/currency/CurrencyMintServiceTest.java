package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyMintServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyMintTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    @Mock
    private MonetaryCurrencyMintingService monetaryCurrencyMintingService;

    private CurrencyMintService service;
    private CurrencyMintTestData td;
    private CurrencyTestData currencyTestData;
    private AccountTestData accountTestData;
    private BlockTestData blockTestData;

    @BeforeEach
    void setUp() {
        td = new CurrencyMintTestData();
        currencyTestData = new CurrencyTestData();
        service = new CurrencyMintServiceImpl(
            table, blockChainInfoService, accountCurrencyService, currencyService, monetaryCurrencyMintingService);
    }

    @Test
    void mintCurrency() {
        accountTestData = new AccountTestData();
        //GIVEN
        MonetarySystemCurrencyMinting attachment = mock(MonetarySystemCurrencyMinting.class);
        doReturn(100L).when(attachment).getCounter();
        doReturn(currencyTestData.CURRENCY_3.getId()).when(attachment).getCurrencyId();
        doReturn(currencyTestData.CURRENCY_3.getMinReservePerUnitATM()).when(attachment).getUnits();
        Account account = mock(Account.class);
        doReturn(accountTestData.ACC_4.getId()).when(account).getId();
        doReturn(td.CURRENCY_MINT_3).when(table).get(any(DbKey.class));
        doReturn(currencyTestData.CURRENCY_3).when(currencyService).getCurrency(currencyTestData.CURRENCY_3.getId());
        doReturn(true).when(monetaryCurrencyMintingService).meetsTarget(
            anyLong(), any(Currency.class), any(MonetarySystemCurrencyMinting.class));
        LedgerEvent ledgerEvent = mock(LedgerEvent.class);

        //WHEN
        service.mintCurrency(ledgerEvent, td.CURRENCY_MINT_4.getCurrencyId(), account, attachment);

        //THEN
        verify(table).get(any(DbKey.class));
        verify(table).insert((any(CurrencyMint.class)));
        verify(currencyService).increaseSupply(any(Currency.class), anyLong());
    }

    @Test
    void getCounter() {
        //GIVEN
        doReturn(td.CURRENCY_MINT_4).when(table).get(any(DbKey.class));
        //WHEN
        long result = service.getCounter(td.CURRENCY_MINT_4.getCurrencyId(), td.CURRENCY_MINT_4.getAccountId());
        assertEquals(td.CURRENCY_MINT_4.getCounter(), result);
        //THEN
        verify(table).get(any(DbKey.class));
    }

    @Test
    void deleteCurrency() {
        blockTestData = new BlockTestData();
        //GIVEN
        DbIterator<CurrencyMint> dbIt = mock(DbIterator.class);
        doReturn(true).doReturn(true).doReturn(false).when(dbIt).hasNext();
        doReturn(td.CURRENCY_MINT_3).doReturn(td.CURRENCY_MINT_2).when(dbIt).next();
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        doReturn(blockTestData.BLOCK_10.getHeight()).when(blockChainInfoService).getHeight();
        Currency currency = mock(Currency.class);
        doReturn(100L).when(currency).getId();

        //WHEN
        service.deleteCurrency(currency);

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
    }
}