/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NO_MESSAGE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.PRUNED_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_TRANSACTION;
import static org.slf4j.LoggerFactory.getLogger;

@Vetoed
public final class ReadMessage extends AbstractAPIRequestHandler {
    private static final Logger LOG = getLogger(ReadMessage.class);

    public ReadMessage() {
        super(new APITag[]{APITag.MESSAGES}, "transaction", "secretPhrase", "sharedKey", "retrieve");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long transactionId = HttpParameterParserUtil.getUnsignedLong(req, "transaction", true);
        boolean retrieve = "true".equalsIgnoreCase(req.getParameter("retrieve"));
        Transaction transaction = lookupBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }
        PrunableMessage prunableMessage = prunableMessageService.get(transactionId);
        if (prunableMessage == null && (transaction.getPrunablePlainMessage() != null || transaction.getPrunableEncryptedMessage() != null) && retrieve) {
            if (prunableRestorationService.restorePrunedTransaction(transactionId) == null) {
                return PRUNED_TRANSACTION;
            }
            prunableMessage = prunableMessageService.get(transactionId);
        }

        JSONObject response = new JSONObject();
        MessageAppendix message = transaction.getMessage();
        EncryptedMessageAppendix encryptedMessage = transaction.getEncryptedMessage();
        EncryptToSelfMessageAppendix encryptToSelfMessage = transaction.getEncryptToSelfMessage();
        if (message == null && encryptedMessage == null && encryptToSelfMessage == null && prunableMessage == null) {
            return NO_MESSAGE;
        }
        if (message != null) {
            response.put("message", Convert.toString(message.getMessage(), message.isText()));
            response.put("messageIsPrunable", false);
        } else if (prunableMessage != null && prunableMessage.getMessage() != null) {
            response.put("message", Convert.toString(prunableMessage.getMessage(), prunableMessage.messageIsText()));
            response.put("messageIsPrunable", true);
        }
        long accountId = HttpParameterParserUtil.getAccountId(req, false);

        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, false);
        byte[] sharedKey = HttpParameterParserUtil.getBytes(req, "sharedKey", false);
        if (sharedKey.length != 0 && keySeed != null) {
            return JSONResponses.either("secretPhrase", "sharedKey");
        }
        if (keySeed != null || sharedKey.length > 0) {
            EncryptedData encryptedData = null;
            boolean isText = false;
            boolean uncompress = true;
            if (encryptedMessage != null) {
                encryptedData = encryptedMessage.getEncryptedData();
                isText = encryptedMessage.isText();
                uncompress = encryptedMessage.isCompressed();
                response.put("encryptedMessageIsPrunable", false);
            } else if (prunableMessage != null && prunableMessage.getEncryptedData() != null) {
                encryptedData = prunableMessage.getEncryptedData();
                isText = prunableMessage.encryptedMessageIsText();
                uncompress = prunableMessage.isCompressed();
                response.put("encryptedMessageIsPrunable", true);
            }
            if (encryptedData != null) {
                try {
                    byte[] decrypted = null;
                    if (keySeed != null) {
                        byte[] readerPublicKey = Crypto.getPublicKey(keySeed);
                        byte[] senderPublicKey = lookupAccountService().getPublicKeyByteArray(transaction.getSenderId());
                        byte[] recipientPublicKey = lookupAccountService().getPublicKeyByteArray(transaction.getRecipientId());
                        byte[] publicKey = Arrays.equals(senderPublicKey, readerPublicKey) ? recipientPublicKey : senderPublicKey;
                        if (publicKey != null) {
                            decrypted = EncryptedDataUtil.decryptFrom(publicKey, encryptedData, keySeed, uncompress);
                        }
                    } else {
                        decrypted = Crypto.aesDecrypt(encryptedData.getData(), sharedKey);
                        if (uncompress) {
                            decrypted = Convert.uncompress(decrypted);
                        }
                    }
                    response.put("decryptedMessage", Convert.toString(decrypted, isText));
                } catch (RuntimeException e) {
                    LOG.debug("Decryption of message to recipient failed: " + e.toString());
                    JSONData.putException(response, e, "Wrong secretPhrase or sharedKey");
                }
            }
            if (encryptToSelfMessage != null && keySeed != null) {
                byte[] publicKey = Crypto.getPublicKey(keySeed);
                try {
                    byte[] decrypted = EncryptedDataUtil.decryptFrom(publicKey, encryptToSelfMessage.getEncryptedData(), keySeed,
                        encryptToSelfMessage.isCompressed());
                    response.put("decryptedMessageToSelf", Convert.toString(decrypted, encryptToSelfMessage.isText()));
                } catch (RuntimeException e) {
                    LOG.debug("Decryption of message to self failed: " + e.toString());
                }
            }
        }
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
