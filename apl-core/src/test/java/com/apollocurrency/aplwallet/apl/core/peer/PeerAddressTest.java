/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author al
 */
@EnableWeld
public class PeerAddressTest {
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .build();
    @Inject
    PropertiesHolder ph;

    public PeerAddressTest() {
    }

    @Test
    public void testFromString() throws MalformedURLException, UnknownHostException {
        PeerAddress a = new PeerAddress("192.168.0.1");
        int port = a.getPort();
        String hp = a.getAddrWithPort();
        assertEquals(hp, "192.168.0.1" + ":" + Integer.toString(Constants.DEFAULT_PEER_PORT));
        assertEquals(port, Constants.DEFAULT_PEER_PORT);
        a = new PeerAddress("[fe80::d166:519e:5758:d24a]");
        hp = a.getAddrWithPort();
        assertEquals(hp, "[fe80:0:0:0:d166:519e:5758:d24a]" + ":" + Integer.toString(Constants.DEFAULT_PEER_PORT));
    }

}
