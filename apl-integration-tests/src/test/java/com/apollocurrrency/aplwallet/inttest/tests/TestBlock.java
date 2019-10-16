package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrency.aplwallet.api.response.GetBloksResponse;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestBlock extends TestBase {

    @Test
    @DisplayName("Get Block")
    public void getBlock() throws IOException {
        BlockDTO block = getBlock("6401614631656403323");
        assertNotNull(block.height);
        assertNotNull(block.transactions);
        assertNotNull(block.generationSignature);
        assertNotNull(block.previousBlock);
        assertNotNull(block.payloadHash);
        assertNotNull(block.cumulativeDifficulty);
        assertNotNull(block.generatorRS);
    }


    @Test
    @DisplayName("Get Block ID")
    public void getBlockID() throws IOException {
        GetBlockIdResponse block = getBlockId("28731");
        assertEquals("11382032888285605874",block.block);

    }

    @Test
    @DisplayName("Get Blockchain Status")
    public void getBlockchainStat() throws IOException {
        BlockchainInfoDTO blockchainStatus = getBlockchainStatus();
        assertEquals("a2e9b946-290b-48b6-9985-dc2e5a5860a1",blockchainStatus.chainId);
        assertNotEquals(BlockchainState.FORK,blockchainStatus.blockchainState);
        assertEquals("Apollo",blockchainStatus.coinSymbol);
        assertEquals("APL",blockchainStatus.accountPrefix);

    }

    @Test
    @DisplayName("Get All Bloks")
    public void getAllBlocks() throws IOException {
        GetBloksResponse blockchainStatus = getBlocks();
        assertNotNull(blockchainStatus);
        assertNotNull(blockchainStatus.blocks);
        assertTrue(blockchainStatus.blocks.size()>0);

    }

    @Test
    @DisplayName("Get ECB Block")
    public void getECB() throws IOException {
        ECBlock blockchainStatus =  getECBlock();
        assertNotNull(blockchainStatus);
        assertNotNull(blockchainStatus.ecBlockHeight);
        assertNotNull(blockchainStatus.ecBlockId);
    }



}
