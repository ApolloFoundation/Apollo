package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.AccountBlocksResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
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
        assertNotNull(block.getHeight());
        assertNotNull(block.getTransactions());
        assertNotNull(block.getGenerationSignature());
        assertNotNull(block.getPreviousBlockHash());
        assertNotNull(block.getPayloadHash());
        assertNotNull(block.getCumulativeDifficulty());
        assertNotNull(block.getGeneratorRS());
    }


    @Test
    @DisplayName("Get Block ID")
    public void getBlockID() throws IOException {
        GetBlockIdResponse block = getBlockId("28731");
        assertEquals("11382032888285605874",block.getBlock());

    }

    @Test
    @DisplayName("Get Blockchain Status")
    public void getBlockchainStat() throws IOException {
        BlockchainInfoDTO blockchainStatus = getBlockchainStatus();
        assertEquals("a2e9b946-290b-48b6-9985-dc2e5a5860a1",blockchainStatus.getChainId());
        assertNotEquals(BlockchainState.FORK, blockchainStatus.getBlockchainState());
        assertEquals("Apollo", blockchainStatus.getCoinSymbol());
        assertEquals("APL", blockchainStatus.getAccountPrefix());

    }

    @Test
    @DisplayName("Get All Bloks")
    public void getAllBlocks() throws IOException {
        AccountBlocksResponse blockchainStatus = getBlocks();
        assertNotNull(blockchainStatus);
        assertNotNull(blockchainStatus.getBlocks());
        assertTrue(blockchainStatus.getBlocks().size() > 0);

    }

    @Test
    @DisplayName("Get ECB Block")
    public void getECB() throws IOException {
        ECBlockDTO blockchainStatus =  getECBlock();
        assertNotNull(blockchainStatus);
        assertNotNull(blockchainStatus.getEcBlockHeight());
        assertNotNull(blockchainStatus.getEcBlockId());
    }



}
