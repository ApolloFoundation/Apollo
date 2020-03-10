package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.response.AccountBlocksResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Blocks")
@Epic(value = "Blocks")
public class TestBlock extends TestBaseNew {

    @Test
    @DisplayName("Get Block")
    public void getBlockTest(){
        GetBlockIdResponse blockID = getBlockId(String.valueOf(getBlock().getHeight()));
        BlockDTO block = getBlock(blockID.getBlock());
        assertAll(
            ()->assertNotNull(block),
            ()->assertNotNull(block.getHeight()),
            ()->assertNotNull(block.getTransactions()),
            ()->assertNotNull(block.getGenerationSignature()),
            ()->assertNotNull(block.getPreviousBlockHash()),
            ()->assertNotNull(block.getPayloadHash()),
            ()->assertNotNull(block.getCumulativeDifficulty()),
            ()->assertNotNull(block.getGeneratorRS())
        );
    }


    @Test
    @DisplayName("Get Block ID")
    public void getBlockID() {
        GetBlockIdResponse block = getBlockId("0");
        assertNotNull(block.getBlock());

    }

    @Test
    @DisplayName("Get Blockchain Status")
    public void getBlockchainStat() throws IOException {
        BlockchainInfoDTO blockchainStatus = getBlockchainStatus();
        assertNotNull(blockchainStatus.getChainId());
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
        ECBlockDTO blockchainStatus = getECBlock();
        assertNotNull(blockchainStatus);
        assertNotNull(blockchainStatus.getEcBlockHeight());
        assertNotNull(blockchainStatus.getEcBlockId());
    }


}
