/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverterCreator;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest extends AbstractEndpointTest {

    private BlockController endpoint;
    @Mock
    private BlockConverter blockConverter;
    @Mock
    private BlockConverterCreator creator;
    @Mock
    private FirstLastIndexParser indexParser;
    @Mock
    private TimeService timeService;
    private static final String getOneUri = "/block/one";
    private static final String getByIdUri = "/block/id";
    private static final String getBlocksUri = "/block/list";
    private static final String getBlockEcUri = "/block/ec";

//    private TransactionTestData txd;
    private BlockTestData btd;
    private BlockDTO blockDTO;

    @BeforeEach
    void setUp() {
        super.setUp();
        endpoint = new BlockController(blockchain, creator, 100, timeService);
        dispatcher.getRegistry().addSingletonResource(endpoint);
//        txd = new TransactionTestData();
        btd = new BlockTestData();
    }

    @Test
    void getBlock_EMPTY_OK() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_13).when(blockchain).getLastBlock();
        blockDTO = createBlockDTO(btd.BLOCK_13, false, false);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_13);
        doReturn(blockConverter).when(creator).create(false, false);

        MockHttpResponse response = super.sendGetRequest(getOneUri);
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
        doReturn(blockConverter).when(creator).create(false, false);

        MockHttpResponse response = super.sendGetRequest(getOneUri + "?block=" + btd.BLOCK_10.getStringId());
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
        doReturn(blockConverter).when(creator).create(false, false);

        MockHttpResponse response = super.sendGetRequest(getOneUri + "?height=" + btd.BLOCK_10.getHeight());
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
        doReturn(blockConverter).when(creator).create(false, false);

        MockHttpResponse response = super.sendGetRequest(getOneUri + "?timestamp=" + btd.BLOCK_10.getTimestamp());
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

        MockHttpResponse response = super.sendGetRequest(getByIdUri + "?height=" + btd.BLOCK_10.getHeight());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(Long.toUnsignedString(btd.BLOCK_10.getId()), dtoResult.getBlock());
        //verify
        verify(blockchain, times(1)).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
    }

    @Test
    void getBlockId_MISSING_height() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_10.getId()).when(blockchain).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
        blockDTO = createBlockDTO(btd.BLOCK_10, true, false);

        MockHttpResponse response = super.sendGetRequest(getOneUri);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2005, error.getNewErrorCode());

        //verify
        verify(blockConverter, times(0)).convert(btd.BLOCK_10);
        verify(blockchain, times(0)).getBlockIdAtHeight(btd.BLOCK_10.getHeight());
    }

    @Test
    void getBlocks_OK() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_13).when(blockchain).getLastBlock();
        List<Block> blockList = List.of( btd.BLOCK_1, btd.BLOCK_2, btd.BLOCK_3, btd.BLOCK_4, btd.BLOCK_5, btd.BLOCK_6, btd.BLOCK_10 );
        Stream<Block> blockStream3 = Stream.of( btd.BLOCK_1, btd.BLOCK_2, btd.BLOCK_3, btd.BLOCK_4, btd.BLOCK_5, btd.BLOCK_6, btd.BLOCK_10 );
        doReturn(blockList).when(blockchain).getBlocksFromShards(0, 99, -1); // fix
        doReturn(blockConverter).when(creator).create(false, false);

        List<BlockDTO> blockDTOList = createDtoList(blockStream3);
        doReturn(blockDTOList).when(blockConverter).convert(blockList);

        MockHttpResponse response = super.sendGetRequest(getBlocksUri);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        BlocksResponse dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(blockList.size(), blockDTOList.size());
        //verify
        verify(blockConverter, times(1)).convert(blockList);
        verify(blockchain, times(1)).getBlocksFromShards(0, 99, -1);
    }

    @Test
    void getBlockEC_OK() throws URISyntaxException, IOException {
        EcBlockData ecBlockData = new EcBlockData(btd.BLOCK_10.getId(), btd.BLOCK_10.getHeight());
        doReturn(ecBlockData).when(blockchain).getECBlock(btd.BLOCK_10.getTimestamp());
        doReturn(btd.BLOCK_10.getTimestamp()).when(timeService).getEpochTime();

        MockHttpResponse response = super.sendGetRequest(getBlockEcUri+ "?timestamp=-1");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        ECBlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(Long.valueOf(btd.BLOCK_10.getId()).longValue(), dtoResult.getId());
        assertEquals(btd.BLOCK_10.getTimestamp(), dtoResult.getTimestamp());
        //verify
        verify(blockchain, times(1)).getECBlock(btd.BLOCK_10.getTimestamp());
    }

    @Test
    void getBlockEC_OK_EMPTY() throws URISyntaxException, IOException {
        EcBlockData ecBlockData = new EcBlockData(btd.BLOCK_10.getId(), btd.BLOCK_10.getHeight());
        doReturn(ecBlockData).when(blockchain).getECBlock(btd.BLOCK_10.getTimestamp());
        doReturn(btd.BLOCK_10.getTimestamp()).when(timeService).getEpochTime();

        MockHttpResponse response = super.sendGetRequest(getBlockEcUri);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        ECBlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(Long.valueOf(btd.BLOCK_10.getId()).longValue(), dtoResult.getId());
        assertEquals(btd.BLOCK_10.getTimestamp(), dtoResult.getTimestamp());
        //verify
        verify(blockchain, times(1)).getECBlock(btd.BLOCK_10.getTimestamp());
    }

    private List<BlockDTO> createDtoList(Stream<Block> source) {
        return source.map(item -> createBlockDTO(item, true, true)).collect(Collectors.toList());
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
        dto.setNumberOfTransactions(blockchain.getBlockTransactionCount(model.getId()));
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
            model.getTransactions().stream().mapToLong(Transaction::getAmountATM).sum()));
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