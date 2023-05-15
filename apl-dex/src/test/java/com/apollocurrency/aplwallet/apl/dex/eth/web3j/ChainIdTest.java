/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dex.eth.web3j;

import com.apollocurrency.aplwallet.apl.dex.eth.service.DexBeanProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.NetVersion;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChainIdTest {
    @Mock
    DexBeanProducer dexBeanProducer;
    @Mock
    Web3j web3j;


    @BeforeEach
    void setUp() {
        lenient().doReturn(web3j).when(dexBeanProducer).web3j();
    }

    @Test
    void get_notInitialized() {
        ChainId chainId = new ChainId(dexBeanProducer);
        assertThrows(IllegalStateException.class, () -> chainId.validate());
        assertThrows(IllegalStateException.class, () -> chainId.get());
    }

    @Test
    void get_OK() throws IOException {
        ChainId chainId = new ChainId(dexBeanProducer);
        Request request = mock(Request.class);
        doReturn(request).when(web3j).netVersion();
        NetVersion netVersion = new NetVersion();
        netVersion.setResult("1");
        doReturn(netVersion).when(request).send();

        chainId.init();
        chainId.validate();

        assertEquals(1, chainId.get());
        assertEquals(1, chainId.getValid());
        assertTrue(chainId.isInitialized());
        assertFalse(chainId.isFailed());
    }

    @Test
    void get_errorReturned() throws IOException {
        ChainId chainId = new ChainId(dexBeanProducer);
        Request request = mock(Request.class);
        doReturn(request).when(web3j).netVersion();
        NetVersion netVersion = new NetVersion();
        netVersion.setError(new Response.Error(1, "Test error"));
        doReturn(netVersion).when(request).send();

        chainId.init();

        assertThrows(IllegalStateException.class, chainId::validate);
        assertEquals(-1, chainId.get());
        assertTrue(chainId.isInitialized());
        assertTrue(chainId.isFailed());
        assertThrows(IllegalStateException.class, chainId::getValid);
    }

    @Test
    void get_IOerror() throws IOException {
        ChainId chainId = new ChainId(dexBeanProducer);
        Request request = mock(Request.class);
        doReturn(request).when(web3j).netVersion();
        doThrow(new IOException("Test IO error")).when(request).send();

        chainId.init();

        assertThrows(IllegalStateException.class, chainId::validate);
        assertEquals(-1, chainId.get());
        assertTrue(chainId.isInitialized());
        assertTrue(chainId.isFailed());
        assertThrows(IllegalStateException.class, chainId::getValid);
    }
}