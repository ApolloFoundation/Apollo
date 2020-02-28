/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.apollocurrency.aplwallet.api.dto.account.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.account.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainConstantsDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStateDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.api.dto.info.TimeDto;
import com.apollocurrency.aplwallet.api.dto.info.TotalSupplyDto;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.peer.BlockchainState;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@Slf4j
class ServerInfoControllerTest {
    @Mock
    private BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    @Mock
    private ServerInfoService serverInfoService = Mockito.mock(ServerInfoService.class);

    private static ObjectMapper mapper = new ObjectMapper();
    private static Dispatcher dispatcher;

    private static String blockchainStatusUri = "/server/blockchain/status";
    private static String blockchainConstantsUri = "/server/blockchain/constants";
    private static String blockchainStateUri = "/server/blockchain/state";
    private static String timeUri = "/server/blockchain/time";
    private static String supplyUri = "/server/blockchain/supply";
    private static String propertiesUri = "/server/blockchain/properties";

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
    }

    @Test
    void blockchainStatus_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        BlockchainStatusDto dto = new BlockchainStatusDto("1.48.1", "1.2", 123, BlockchainState.UP_TO_DATE.toString());
        doReturn(dto).when(serverInfoService).getBlockchainStatus();
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = blockchainStatusUri;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        BlockchainStatusDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.application);
        assertNotNull(dtoResult.version);
        // verify
        verify(serverInfoService, times(1)).getBlockchainStatus();
    }

    @Test
    void blockchainConstants_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        BlockchainConstantsDto dto = new BlockchainConstantsDto("123-567", "234-567", 100L);
        doReturn(dto).when(serverInfoService).getBlockchainConstants();
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = blockchainConstantsUri;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        BlockchainConstantsDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.genesisAccountId);
        assertEquals(100L, dtoResult.epochBeginning);
        // verify
        verify(serverInfoService, times(1)).getBlockchainConstants();
    }

    @Test
    void blockchainState_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        BlockchainStateDto dto = new BlockchainStateDto(
            new BlockchainStatusDto("1.48.1", "1.2", 123, BlockchainState.UP_TO_DATE.toString()));
        doReturn(dto).when(serverInfoService).getBlockchainState(true);
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);

        // call
        String uri = blockchainStateUri + "?includeCounts=" + Boolean.TRUE + "&adminPassword=1";
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        BlockchainStateDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.application);
        assertNotNull(dtoResult.version);
        assertEquals(123, dtoResult.time);
        assertEquals(BlockchainState.UP_TO_DATE.toString(), dtoResult.blockchainState);
        // verify
        verify(serverInfoService, times(1)).getBlockchainState(true);
    }

    @Test
    void time_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        TimeDto dto = new TimeDto(100);
        doReturn(dto).when(serverInfoService).getTime();
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = timeUri;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        TimeDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertEquals(100, dtoResult.time);
        // verify
        verify(serverInfoService, times(1)).getTime();
    }

    @Test
    void supply_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        TotalSupplyDto dto = new TotalSupplyDto(100);
        doReturn(dto).when(serverInfoService).getTotalSupply();
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = supplyUri;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        TotalSupplyDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertEquals(100, dtoResult.totalAmount);
        // verify
        verify(serverInfoService, times(1)).getTotalSupply();
    }

    @Test
    void properties_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        Map<String, Object> dto = new HashMap<>(1);
        dto.put("testKey", "testValue");
        doReturn(dto).when(serverInfoService).getProperties();
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = propertiesUri;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Map<String, Object> dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertEquals(1, dtoResult.size());
        assertEquals("testValue", dtoResult.get("testKey"));
        // verify
        verify(serverInfoService, times(1)).getProperties();
    }

}