/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.utils.FullHashToIdDto;
import com.apollocurrency.aplwallet.api.dto.utils.HashDto;
import com.apollocurrency.aplwallet.api.dto.utils.HexConvertDto;
import com.apollocurrency.aplwallet.api.dto.utils.QrDecodeDto;
import com.apollocurrency.aplwallet.api.dto.utils.QrEncodeDto;
import com.apollocurrency.aplwallet.api.dto.utils.RsConvertDto;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

class DebugControllerTest {

    @BeforeEach
    void setup() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        UtilsController controller = new UtilsController(blockchainConfig);
        dispatcher.getRegistry().addSingletonResource(controller);
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
    }

    @Test
}