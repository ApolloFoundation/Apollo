package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TradingDataOutputToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.rest.exception.LegacyParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.DexTradingDataService;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.TimeFrame;
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
class TradingDataControllerTest {
    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    @Mock
    private DexTradingDataService service;
    private TradingDataOutput tradingDataOutput = new TradingDataOutput();

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
        TradingDataController tradingDataController = new TradingDataController();
        tradingDataController.setService(service);
        dispatcher.getRegistry().addSingletonResource(tradingDataController);
        dispatcher.getProviderFactory().registerProvider(LegacyParameterExceptionMapper.class);
        tradingDataOutput.setData(List.of(CandlestickTestUtil.fromRawData("100", "200", "150", "180", "659400.34", "325000.025", 14400)));
    }

    @Test
    void testGetCandlesticks() throws URISyntaxException, IOException {
        doReturn(tradingDataOutput).when(service).getForTimeFrame(15000, 1, DexCurrency.ETH, TimeFrame.HOUR);

        MockHttpRequest request = MockHttpRequest.get("/dex/chart?fsym=APL&tsym=ETH&toTs=15000&limit=1&timeFrame=HOUR").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String json = response.getContentAsString();
        TradingDataOutputDTO dto = mapper.readValue(json, TradingDataOutputDTO.class);
        assertEquals(new TradingDataOutputToDtoConverter().apply(tradingDataOutput), dto);
    }

    @Test
    void testGetCandlesticksForWrongBaseCurrencyType() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.get("/dex/chart?fsym=ETH&tsym=APL&toTs=15000&limit=1&timeFrame=HOUR").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String json = response.getContentAsString();
        Error error = mapper.readValue(json, Error.class);
        assertEquals("Base currency should be equal to 'APL'", error.getErrorDescription());
    }

    @Test
    void testGetCandlesticksForWrongPairedCurrencyType() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.get("/dex/chart?fsym=APL&tsym=APL&toTs=15000&limit=1&timeFrame=HOUR").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String json = response.getContentAsString();
        Error error = mapper.readValue(json, Error.class);
        assertEquals("Paired currency should not be equal to 'APL'", error.getErrorDescription());
    }

}