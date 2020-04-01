package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockControllerTest extends AbstractEndpointTest {

    private BlockController endpoint;
    private TransactionConverter transactionConverter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter());
    private BlockConverter blockConverter = new BlockConverter(blockchain, transactionConverter, mock(PhasingPollService.class));

    private Block GENESIS_BLOCK, LAST_BLOCK, NEW_BLOCK;
    private Block BLOCK_0, BLOCK_1, BLOCK_2, BLOCK_3;
    private List<Block> BLOCKS;

    @BeforeEach
    void setUp() {
        super.setUp();
        endpoint = new BlockController(blockchain, blockConverter);
        dispatcher.getRegistry().addSingletonResource(endpoint);
    }

    @Test
    void getBlockGet() {
    }

    @Test
    void getBlockPost() {
    }

    @Test
    void getBlocks() {
    }
}