package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import static org.junit.jupiter.api.Assertions.*;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CurrencyServiceTest {

    CurrencyService service;
    CurrencyTestData td;
    BlockTestData blockTestData;
    @Mock
    private CurrencySupplyTable currencySupplyTable;
    @Mock
    private CurrencyTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountCurrencyService accountCurrencyService;
    @Mock
    private CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    @Mock
    private CurrencyFounderService currencyFounderService;
    @Mock
    private ExchangeService exchangeService;
    @Mock
    private CurrencyTransferService currencyTransferService;
    @Mock
    private IteratorToStreamConverter<CurrencyTransfer> iteratorToStreamConverter;

    @BeforeEach
    void setUp() {
        td = new CurrencyTestData();
        service = new CurrencyServiceImpl(currencySupplyTable, table, blockChainInfoService,
            accountService, accountCurrencyService, currencyExchangeOfferFacade, currencyFounderService,
            exchangeService, currencyTransferService);
    }

    @Test
    void addCurrency() {
    }

    @Test
    void increaseReserve() {
    }

    @Test
    void claimReserve() {
    }

    @Test
    void transferCurrency() {
    }

    @Test
    void loadCurrencySupplyByCurrency() {
    }

    @Test
    void increaseSupply() {
    }

    @Test
    void canBeDeletedBy() {
    }

    @Test
    void delete() {
    }
}