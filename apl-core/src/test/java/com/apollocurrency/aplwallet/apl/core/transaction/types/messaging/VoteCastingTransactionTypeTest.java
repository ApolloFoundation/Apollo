package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class VoteCastingTransactionTypeTest {
    private static final String voteCastingAttachmentJson = "{\"version.VoteCasting\":1,\"poll\":\"5337968112669619420\",\"vote\":[1,-128,1]}";

    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private AccountService accountService;
    @Mock
    private PollService pollService;
    @Mock
    private TransactionValidator transactionValidator;

    private VoteCastingTransactionType voteCastingTransactionType =
        new VoteCastingTransactionType(blockchainConfig, accountService, pollService, transactionValidator);


    @Test
    void parseAttachment() throws ParseException, AplException.NotValidException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(voteCastingAttachmentJson);
        MessagingVoteCasting messagingVoteCasting = voteCastingTransactionType.parseAttachment(jsonObject);


        assertEquals(1, messagingVoteCasting.getVersion());
        assertEquals(5337968112669619420L, messagingVoteCasting.getPollId());
        assertEquals(1, messagingVoteCasting.getPollVote()[0]);
        assertEquals(-128, messagingVoteCasting.getPollVote()[1]);
        assertEquals(1, messagingVoteCasting.getPollVote()[2]);
    }
}