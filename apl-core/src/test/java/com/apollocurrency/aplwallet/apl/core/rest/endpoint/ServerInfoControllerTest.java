/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;

import com.apollocurrency.aplwallet.api.dto.info.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainConstantsDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStateDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.api.dto.info.TimeDto;
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
import org.jboss.resteasy.plugins.server.servlet.ServletSecurityContext;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

    private static String accountCountUri = "/server/info/count";
    private static String blockchainStatusUri = "/server/blockchain/status";
    private static String blockchainConstantsUri = "/server/blockchain/constants";
    private static String blockchainStateUri = "/server/blockchain/state";
    private static String timeUri = "/server/blockchain/time";

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
    }

    @Test
    void counts_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        AccountsCountDto dto = new AccountsCountDto(112L, 113L, 1, 124L);
        AccountEffectiveBalanceDto balanceDto = new AccountEffectiveBalanceDto(
            100L, 200L, 100L, 200L, 200L, "123", "RS-ADVB");
        dto.topHolders.add(balanceDto);
        doReturn(dto).when(serverInfoService).getAccountsStatistic(Constants.MIN_TOP_ACCOUNTS_NUMBER);
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = accountCountUri + "?numberOfAccounts=" + Constants.MIN_TOP_ACCOUNTS_NUMBER;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        AccountsCountDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.topHolders);
        assertEquals(112L, dtoResult.totalSupply);
        assertEquals(1, dtoResult.topHolders.size());

        // call 2
        uri = accountCountUri + "?numberOfAccounts="; // empty value
        request = MockHttpRequest.get(uri);
        response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        respondJson = response.getContentAsString();
        dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.topHolders);
        assertEquals(112L, dtoResult.totalSupply);
        assertEquals(1, dtoResult.topHolders.size());
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
    }


    public static class MockSecurityContext extends ServletSecurityContext {
        public MockSecurityContext() {
            super(null);
        }
        @Override
        public boolean isUserInRole(String role) {
            return role.equals("admin");
        }
    }

//    @Test
    @Disabled
    void blockchainState_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        BlockchainStateDto dto = new BlockchainStateDto(
            new BlockchainStatusDto("1.48.1", "1.2", 123, BlockchainState.UP_TO_DATE.toString()));
        doReturn(dto).when(serverInfoService).getBlockchainState(true, true);
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        Annotation[] annotations = new Annotation[0];
        dispatcher.getRegistry().addSingletonResource(controller);
        ResteasyConfiguration configuration = dispatcher.getProviderFactory().getContextData(
            org.jboss.resteasy.spi.ResteasyConfiguration.class, org.jboss.resteasy.spi.ResteasyConfiguration.class, annotations, false);//.context = new MockSecurityContext();
/*
        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(controller);
        ResteasyProviderFactory.getInstance().getContextData(org.jboss.resteasy.spi.ResteasyConfiguration.class,
            org.jboss.resteasy.spi.ResteasyConfiguration.class, annotations, false).put(SecurityContext.class, new FakeSecurityContext());
*/


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
    }


}