package com.apollocurrrency.aplwallet.inttest.tests;


import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class Networking extends TestBase {

    @Test
    @DisplayName("Get Peers")
    public void getAllPeers() throws IOException {
        assertTrue(super.getPeers().length >0);
    }

}
