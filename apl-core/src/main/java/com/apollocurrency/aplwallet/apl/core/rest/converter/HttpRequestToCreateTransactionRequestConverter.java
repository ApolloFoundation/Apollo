package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestToCreateTransactionRequestConverter {


    public static CreateTransactionRequest convert(HttpServletRequest req, Account senderAccount, long recipientId, long amountATM, Attachment attachment) throws AplException.ValidationException, ParameterException {
        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, false));
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        Boolean encryptedMessageIsPrunable = Boolean.valueOf(req.getParameter("encryptedMessageIsPrunable"));
        Boolean messageIsPrunable = Boolean.valueOf(req.getParameter("messageIsPrunable"));
        Boolean isPhased = Boolean.valueOf(req.getParameter("phased"));
        Account recipient = null;

        if (recipientId != 0) {
            recipient = Account.getAccount(recipientId);
        }

        CreateTransactionRequest createTransactionRequest = CreateTransactionRequest.builder()
                .broadcast(!"false".equalsIgnoreCase(req.getParameter("broadcast")) && (secretPhrase != null || passphrase != null))
                .secretPhrase(secretPhrase)
                .passphrase(passphrase)
                .deadlineValue(req.getParameter("deadline"))
                .referencedTransactionFullHash(Convert.emptyToNull(req.getParameter("referencedTransactionFullHash")))
                .publicKeyValue(Convert.emptyToNull(req.getParameter("publicKey")))
                .publicKey(ParameterParser.getPublicKey(req, senderAccount.getId()))
                .keySeed(ParameterParser.getKeySeed(req, senderAccount.getId(), false))

                .amountATM(amountATM)
                .feeATM(ParameterParser.getFeeATM(req))
                .senderAccount(senderAccount)
                .recipientId(recipientId)
                .recipientPublicKey(Convert.emptyToNull(req.getParameter("recipientPublicKey")))
                .phased(isPhased)
                .phasing(isPhased ? ParameterParser.parsePhasing(req) : null)

                .attachment(attachment)
                .encryptToSelfMessage(ParameterParser.getEncryptToSelfMessage(req, senderAccount.getId()))
                .encryptedMessageIsPrunable(encryptedMessageIsPrunable)
                .appendix(ParameterParser.getEncryptedMessage(req, recipient, senderAccount.getId(), encryptedMessageIsPrunable))
                .messageIsPrunable(messageIsPrunable)
                .message(ParameterParser.getPlainMessage(req, messageIsPrunable))

                .ecBlockHeight(ParameterParser.getInt(req, "ecBlockHeight", 0, Integer.MAX_VALUE, false))
                .ecBlockId(Convert.parseUnsignedLong(req.getParameter("ecBlockId")))

                .build();

        return createTransactionRequest;
    }


}
