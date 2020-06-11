package com.apollocurrency.aplwallet.apl.core.service.state.exchange;

import static org.junit.jupiter.api.Assertions.*;

import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeRequestTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.data.ExchangeRequestTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRequestServiceTest {

    @Inject
    ExchangeRequestService service;
    ExchangeRequestTestData td;
    @Mock
    private ExchangeRequestTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private IteratorToStreamConverter<ExchangeRequest> exchangeRequestIteratorToStreamConverter;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getAllExchangeRequestsStream() {
    }

    @Test
    void getCount() {
    }

    @Test
    void getExchangeRequest() {
    }

    @Test
    void getCurrencyExchangeRequestsStream() {
    }

    @Test
    void getAccountExchangeRequestsStream() {
    }

    @Test
    void getAccountCurrencyExchangeRequestsStream() {
    }

    @Test
    void addExchangeRequest() {
    }

    @Test
    void testAddExchangeRequest() {
    }
}