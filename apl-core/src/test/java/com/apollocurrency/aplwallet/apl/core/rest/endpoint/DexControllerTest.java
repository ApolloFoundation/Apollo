package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.ExchangeContractDTO;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.rest.converter.ExchangeContractToDTOConverter;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOrderTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class DexControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    @Mock
    private DexService service;
    @Mock
    private DexOrderTransactionCreator dexOrderTransactionCreator;
    @Mock
    private TimeService timeService;
    @Mock
    private DexEthService dexEthService;
    @Mock
    private EthereumWalletService walletService;
    private DexTestData td;

    @BeforeEach
    void setup() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        DexController dexController = new DexController(service, dexOrderTransactionCreator, timeService, dexEthService, walletService);
        dispatcher.getRegistry().addSingletonResource(dexController);
        td = new DexTestData();
    }

    @Test
    void testGetContractsForAccountOrder() throws URISyntaxException, IOException {
        List<ExchangeContract> contracts = List.of(td.EXCHANGE_CONTRACT_1, td.EXCHANGE_CONTRACT_3);
        doReturn(contracts).when(service).getContractsByAccountOrderFromStatus(td.EXCHANGE_CONTRACT_1.getSender(), td.EXCHANGE_CONTRACT_1.getOrderId(), (byte) 0);

        MockHttpRequest request = MockHttpRequest.get("/dex/contracts?accountId=" + td.EXCHANGE_CONTRACT_1.getSender() + "&orderId=" + td.EXCHANGE_CONTRACT_1.getOrderId()).contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<ExchangeContractDTO> responseErrors = mapper.readValue(errorJson, new TypeReference<>() {
        });
        ExchangeContractToDTOConverter converter = new ExchangeContractToDTOConverter();
        assertEquals(converter.convert(contracts), responseErrors);
    }

    @Test
    void testGetContractForAccountOrder() throws URISyntaxException, IOException {
        List<ExchangeContract> contracts = List.of(td.EXCHANGE_CONTRACT_1);
        doReturn(contracts).when(service).getContractsByAccountOrderFromStatus(td.EXCHANGE_CONTRACT_1.getSender(), td.EXCHANGE_CONTRACT_1.getOrderId(), (byte) 0);

        MockHttpRequest request = MockHttpRequest.get("/dex/contracts?accountId=" + td.EXCHANGE_CONTRACT_1.getSender() + "&orderId=" + td.EXCHANGE_CONTRACT_1.getOrderId()).contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<ExchangeContractDTO> responseErrors = mapper.readValue(errorJson, new TypeReference<>() {
        });
        ExchangeContractToDTOConverter converter = new ExchangeContractToDTOConverter();
        assertEquals(converter.convert(contracts), responseErrors);
    }

    @Test
    void testGetAllVersionedContractsForAccountOrder() throws URISyntaxException, IOException {
        List<ExchangeContract> contracts = List.of(td.EXCHANGE_CONTRACT_1, td.EXCHANGE_CONTRACT_3, td.EXCHANGE_CONTRACT_5);
        doReturn(contracts).when(service).getVersionedContractsByAccountOrder(td.EXCHANGE_CONTRACT_1.getSender(), td.EXCHANGE_CONTRACT_1.getOrderId());

        MockHttpRequest request = MockHttpRequest.get("/dex/all-contracts?accountId=" + td.EXCHANGE_CONTRACT_1.getSender() + "&orderId=" + td.EXCHANGE_CONTRACT_1.getOrderId()).contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<ExchangeContractDTO> responseErrors = mapper.readValue(errorJson, new TypeReference<>() {
        });
        ExchangeContractToDTOConverter converter = new ExchangeContractToDTOConverter();
        assertEquals(converter.convert(contracts), responseErrors);
    }

}
