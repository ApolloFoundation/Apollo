/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsRequestParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GetTransactionsTest {
    @Mock
    Blockchain blockchain;
    @Mock
    Peer peer;
    TransactionConverter converter;

    GetTransactions endpoint;

    private TransactionTestData td;

    @BeforeEach
    void setUp() {
        converter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter(mock(PrunableLoadingService.class)));
        endpoint = new GetTransactions(blockchain, converter, new GetTransactionsRequestParser());
        td = new TransactionTestData();
        Convert2.init("APL", 0);
    }

    @Test
    void processRequest() throws ParseException, IOException {
        Set<@NotNull Long> requestedTxIds = Set.of(1L, td.TRANSACTION_11.getId(), td.TRANSACTION_3.getId());
        GetTransactionsRequest request = new GetTransactionsRequest(requestedTxIds, UUID.randomUUID());
        doReturn(List.of(td.TRANSACTION_11, td.TRANSACTION_3)).when(blockchain).getTransactionsByIds(requestedTxIds);


        JSONStreamAware response = endpoint.processRequest(requestToJSONObject(request), peer);

        List<TransactionDTO> transactions = getTransactionDTOS(response);
        assertEquals(transactions.size(), 2);
        assertEquals(transactions.get(0).getTransaction(), td.TRANSACTION_11.getStringId());
        assertEquals(transactions.get(1).getTransaction(), td.TRANSACTION_3.getStringId());

    }

    @Test
    void rejectWhileDownloading() {
        assertFalse(endpoint.rejectWhileDownloading(), "GetTransactions request should not be rejected during blockchain downloading");
    }

    private List<TransactionDTO> getTransactionDTOS(JSONStreamAware response) throws IOException, ParseException {
        StringWriter stringWriter = new StringWriter();
        response.writeJSONString(stringWriter);
        GetTransactionsResponseParser responseParser = new GetTransactionsResponseParser();
        GetTransactionsResponse transactionsResponse = responseParser.parse((JSONObject) new JSONParser().parse(stringWriter.toString()));
        return transactionsResponse.getTransactions();
    }

    private JSONObject requestToJSONObject(GetTransactionsRequest request) throws ParseException, JsonProcessingException {
        String json = JSON.getMapper().writeValueAsString(request);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(json);
    }
}