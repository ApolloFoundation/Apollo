/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest extends AbstractEndpointTest {

//    private BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
//    private TimeService timeService = mock(TimeService.class);
//    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
//    private PhasingPollService phasingPollService = mock(PhasingPollService.class);
//    private AccountService accountService = mock(AccountService.class);

    private BlockController endpoint;
//    private TransactionConverter transactionConverter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter());
//    private TransactionConverter transactionConverter = mock(TransactionConverter.class);
//    private BlockConverter blockConverter = new BlockConverter(blockchain, transactionConverter, phasingPollService);
    private BlockConverter blockConverter = mock(BlockConverter.class);

//    private TransactionTestData txd;
    private BlockTestData btd;
    private BlockDTO blockDTO;

    @BeforeEach
    void setUp() {
        super.setUp();
        endpoint = new BlockController(blockchain, blockConverter);
        dispatcher.getRegistry().addSingletonResource(endpoint);
//        txd = new TransactionTestData();
        btd = new BlockTestData();
    }

    @Test
    void getBlock_EMPTY_OK() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_13).when(blockchain).getLastBlock();
        blockDTO = createBlockDTO(btd.BLOCK_13, false, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_13);

        MockHttpResponse response = super.sendGetRequest("/block");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(btd.BLOCK_13.getHeight(), dtoResult.getHeight());
        //verify
        verify(blockConverter, times(1)).convert(btd.BLOCK_13);
        verify(blockchain, times(1)).getLastBlock();
    }

    @Test
    void getBlock_byID() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_10).when(blockchain).getBlock(btd.BLOCK_10.getId());
        blockDTO = createBlockDTO(btd.BLOCK_10, true, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_10);

        MockHttpResponse response = super.sendGetRequest("/block?block=" + btd.BLOCK_10.getStringId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(btd.BLOCK_10.getHeight(), dtoResult.getHeight());
        assertEquals(btd.BLOCK_10.getTransactions().size(), dtoResult.getTransactions().size());
        //verify
        verify(blockConverter, times(1)).convert(btd.BLOCK_10);
        verify(blockchain, times(1)).getBlock(btd.BLOCK_10.getId());
    }

    @Test
    void getBlock_byHeight() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_10).when(blockchain).getBlockAtHeight(btd.BLOCK_10.getHeight());
        blockDTO = createBlockDTO(btd.BLOCK_10, true, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_10);

        MockHttpResponse response = super.sendGetRequest("/block?height=" + btd.BLOCK_10.getHeight());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(btd.BLOCK_10.getHeight(), dtoResult.getHeight());
        assertEquals(btd.BLOCK_10.getTransactions().size(), dtoResult.getTransactions().size());
        //verify
        verify(blockConverter, times(1)).convert(btd.BLOCK_10);
        verify(blockchain, times(1)).getBlockAtHeight(btd.BLOCK_10.getHeight());
    }

    @Test
    void getBlock_byTimestamp() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_10).when(blockchain).getLastBlock(btd.BLOCK_10.getTimestamp());
        blockDTO = createBlockDTO(btd.BLOCK_10, true, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_10);

        MockHttpResponse response = super.sendGetRequest("/block?timestamp=" + btd.BLOCK_10.getTimestamp());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(btd.BLOCK_10.getTimestamp(), dtoResult.getTimestamp());
        assertEquals(btd.BLOCK_10.getTransactions().size(), dtoResult.getTransactions().size());
        //verify
        verify(blockConverter, times(1)).convert(btd.BLOCK_10);
        verify(blockchain, times(1)).getLastBlock(btd.BLOCK_10.getTimestamp());
    }

    @Test
    void getBlockId_OK() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_10.getId()).when(blockchain).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
        blockDTO = createBlockDTO(btd.BLOCK_10, true, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_10);

        MockHttpResponse response = super.sendGetRequest("/block/id?height=" + btd.BLOCK_10.getHeight());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(String.valueOf(btd.BLOCK_10.getId()), dtoResult.getBlock());
        //verify
        verify(blockchain, times(1)).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
    }

    @Test
    void getBlockId_MISSING_height() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_10.getId()).when(blockchain).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
        blockDTO = createBlockDTO(btd.BLOCK_10, true, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_10);

        MockHttpResponse response = super.sendGetRequest("/block/id");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2001, error.getNewErrorCode());

        //verify
        verify(blockConverter, times(0)).convert(btd.BLOCK_10);
        verify(blockchain, times(0)).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
    }

    @Test
    void getBlocks() {
    }

    private BlockDTO createBlockDTO(Block model, boolean includeTrs, boolean includePhased) {
        BlockDTO dto = new BlockDTO();
        dto.setBlock(model.getStringId());
        dto.setHeight(model.getHeight());
        dto.setGenerator(Long.toUnsignedString(model.getGeneratorId()));
        dto.setGeneratorRS(Convert2.rsAccount(model.getGeneratorId()));
//        dto.setGeneratorPublicKey(Convert.toHexString(model.getGeneratorPublicKey())); // fails
        dto.setTimestamp(model.getTimestamp());
        dto.setTimeout(model.getTimeout());
        dto.setNumberOfTransactions(model.getOrLoadTransactions().size());
        dto.setTotalFeeATM(String.valueOf(model.getTotalFeeATM()));
        dto.setPayloadLength(model.getPayloadLength());
        dto.setVersion(model.getVersion());
        dto.setBaseTarget(Long.toUnsignedString(model.getBaseTarget()));
        dto.setCumulativeDifficulty(model.getCumulativeDifficulty().toString());
        if (model.getPreviousBlockId() != 0) {
            dto.setPreviousBlock(Long.toUnsignedString(model.getPreviousBlockId()));
        }
        if (model.getNextBlockId() != 0 ){
            dto.setNextBlock(Long.toUnsignedString(model.getNextBlockId()));
        }
        dto.setPayloadHash(Convert.toHexString(model.getPayloadHash()));
        dto.setGenerationSignature(Convert.toHexString(model.getGenerationSignature()));
        dto.setPreviousBlockHash(Convert.toHexString(model.getPreviousBlockHash()));
        dto.setBlockSignature(Convert.toHexString(model.getBlockSignature()));
        if (includeTrs) {
            dto.setTransactions(convert(model.getTransactions()));
        } else {
            dto.setTransactions(Collections.emptyList());
        }
        dto.setTotalAmountATM(String.valueOf(
            model.getOrLoadTransactions().stream().mapToLong(Transaction::getAmountATM).sum()));
        return dto;
    }

    private List<TransactionDTO> convert(List<Transaction> list) {
        List<TransactionDTO> result = new ArrayList<>(list.size());
        for (Transaction transaction : list) {
            TransactionDTO dto = new TransactionDTO();
//            dto.setBlock(transaction.getBlock());
            result.add(dto);
        }
        return result;
    }
}