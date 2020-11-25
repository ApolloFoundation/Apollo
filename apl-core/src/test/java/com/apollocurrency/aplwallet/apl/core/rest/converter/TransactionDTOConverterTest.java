package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.VoteCastingTransactionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class TransactionDTOConverterTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private String json = "{\n" +
        "  \"requestProcessingTime\": 0,\n" +
        "  \"type\": 1,\n" +
        "  \"subtype\": 3,\n" +
        "  \"phased\": false,\n" +
        "  \"timestamp\": 90302629,\n" +
        "  \"deadline\": 1440,\n" +
        "  \"senderPublicKey\": \"f02dec3177ccb3df2a8b3ee252c30475b48e477a02fb948caeb11bfb1a055124\",\n" +
        "  \"amountATM\": \"0\",\n" +
        "  \"feeATM\": \"100000000\",\n" +
        "  \"signature\": \"e65871c960db0a990d100e76188026d040e2469990dff8eca3d2565673b7d704e11161d1db56ea2263ad4dcb48c13659f9051092fb844b3d77ed16ab72d7dc6e\",\n" +
        "  \"signatureHash\": \"d1b20314e2ea3689abd82bd5bac9ea859b4eeaf914b20f6b9ea81727864f0cd2\",\n" +
        "  \"fullHash\": \"1e0d368996f271ee3d10fe11d27bb7ae7248e572c8be21aad4724d9a13d36ec8\",\n" +
        "  \"transaction\": \"17181780781756189982\",\n" +
        "  \"attachment\": {\n" +
        "    \"version.VoteCasting\": 1,\n" +
        "    \"poll\": \"9447291856606322846\",\n" +
        "    \"vote\": [\n" +
        "      -128,\n" +
        "      -128,\n" +
        "      1\n" +
        "    ]\n" +
        "  },\n" +
        "  \"sender\": \"2267141429691625712\",\n" +
        "  \"senderRS\": \"APL-GE9J-5TC7-YYXZ-33NXY\",\n" +
        "  \"height\": 2147483647,\n" +
        "  \"version\": 1,\n" +
        "  \"ecBlockId\": \"6063662055728811755\",\n" +
        "  \"ecBlockHeight\": 543274,\n" +
        "  \"block\": \"0\",\n" +
        "  \"confirmations\": -2146939627,\n" +
        "  \"blockTimestamp\": -1,\n" +
        "  \"transactionIndex\": -1\n" +
        "}";

    @Mock
    TransactionTypeFactory transactionTypeFactory;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    PollService pollService;
    @Mock
    TransactionValidator validator;


    @Test
    void applyVoteCastingAttachment() throws JsonProcessingException {
        VoteCastingTransactionType voteCastingTransactionType = new VoteCastingTransactionType(blockchainConfig, accountService, pollService, validator);
        TransactionDTO transactionDTO = objectMapper.readValue(json, TransactionDTO.class);

        doReturn(voteCastingTransactionType).when(transactionTypeFactory).findTransactionType(anyByte(), anyByte());

        TransactionDTOConverter transactionDTOConverter = new TransactionDTOConverter(transactionTypeFactory);

        Transaction transaction = transactionDTOConverter.apply(transactionDTO);


        assertNotNull(transaction);
        assertNotNull(transaction.getAttachment());
        MessagingVoteCasting messagingVoteCasting = (MessagingVoteCasting) transaction.getAttachment();
        assertEquals(Long.parseUnsignedLong("9447291856606322846"), messagingVoteCasting.getPollId());
        assertEquals(3, messagingVoteCasting.getPollVote().length);
    }
}