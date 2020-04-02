/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.cache.NullCacheProducerForTests;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest extends AbstractEndpointTest {

    private BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    private TimeService timeService = mock(TimeService.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private PhasingPollService phasingPollService = mock(PhasingPollService.class);
    private AccountService accountService = mock(AccountService.class);

    private BlockController endpoint;
    private TransactionConverter transactionConverter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter());
//    private BlockConverter blockConverter = new BlockConverter(blockchain, transactionConverter, phasingPollService);
    private BlockConverter blockConverter = mock(BlockConverter.class);

/*    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(TransactionDaoImpl.class, BlockchainImpl.class, BlockDaoImpl.class,
        TransactionIndexDao.class, DaoConfig.class,
        BlockIndexServiceImpl.class, NullCacheProducerForTests.class)
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(phasingPollService, PhasingPollService.class))
        .addBeans(MockBean.of(accountService, AccountService.class))
//        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .build();*/

    private TransactionTestData txd;
    private BlockTestData btd;
    private BlockDTO blockDTO;

    @BeforeEach
    void setUp() {
        super.setUp();
        endpoint = new BlockController(blockchain, blockConverter);
        dispatcher.getRegistry().addSingletonResource(endpoint);
        txd = new TransactionTestData();
        btd = new BlockTestData();
    }

    @Test
    void getBlockGet_OK() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_13).when(blockchain).getLastBlock();
        blockDTO = createBlockDTO(btd.BLOCK_13);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_13);

        MockHttpResponse response = super.sendGetRequest("/block");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(btd.BLOCK_13.getHeight(), dtoResult.getHeight());
        //verify
        verify(blockConverter, times(1)).convert(btd.BLOCK_13);
        verify(blockchain, times(1)).getLastBlock();
    }

    @Test
    void getBlockPost_OK() throws URISyntaxException, IOException {
        doReturn(btd.BLOCK_13).when(blockchain).getLastBlock();
        blockDTO = createBlockDTO(btd.BLOCK_13);
        doReturn(blockDTO).when(blockConverter).convert(btd.BLOCK_13);

        MockHttpRequest request = MockHttpRequest.post("/block")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("block", ""); // body CAN'T be completely empty in test
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        BlockDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(btd.BLOCK_13.getHeight(), dtoResult.getHeight());
        //verify
        verify(blockConverter, times(1)).convert(btd.BLOCK_13);
        verify(blockchain, times(1)).getLastBlock();
    }

    @Test
    void getBlockPost() {
    }

    @Test
    void getBlocks() {
    }

    private BlockDTO createBlockDTO(Block model) {
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
        dto.setBaseTarget(String.valueOf(model.getBaseTarget()));
        dto.setCumulativeDifficulty(model.getCumulativeDifficulty().toString());
        if (model.getPreviousBlockId() != 0) {
            dto.setPreviousBlock(Long.toUnsignedString(model.getPreviousBlockId()));
        }
        dto.setPreviousBlockHash(Convert.toHexString(model.getPreviousBlockHash()));
        if (model.getNextBlockId() != 0 ){
            dto.setNextBlock(Long.toUnsignedString(model.getNextBlockId()));
        }
        dto.setPayloadHash(Convert.toHexString(model.getPayloadHash()));
        dto.setGenerationSignature(Convert.toHexString(model.getGenerationSignature()));
        dto.setBlockSignature(Convert.toHexString(model.getBlockSignature()));
        dto.setTransactions(Collections.emptyList());
        dto.setTotalAmountATM(String.valueOf(
            model.getOrLoadTransactions().stream().mapToLong(Transaction::getAmountATM).sum()));
        return dto;
    }

}