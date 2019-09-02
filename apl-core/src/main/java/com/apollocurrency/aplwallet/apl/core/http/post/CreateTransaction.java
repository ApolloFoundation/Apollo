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

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DEADLINE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_EC_BLOCK;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_DEADLINE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SECRET_PHRASE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NOT_ENOUGH_FUNDS;

public abstract class CreateTransaction extends AbstractAPIRequestHandler {
    private static TransactionValidator validator = CDI.current().select(TransactionValidator.class).get();
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    protected TimeService timeService = CDI.current().select(TimeService.class).get();
    private static FeeCalculator feeCalculator = CDI.current().select(FeeCalculator.class).get();
    private static final String[] commonParameters = new String[]{"secretPhrase", "publicKey", "feeATM",
            "deadline", "referencedTransactionFullHash", "broadcast",
            "message", "messageIsText", "messageIsPrunable",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf",
            "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel",
            "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted",
            "phasingLinkedFullHash", "phasingLinkedFullHash", "phasingLinkedFullHash",
            "phasingHashedSecret", "phasingHashedSecretAlgorithm",
            "recipientPublicKey",
            "ecBlockId", "ecBlockHeight"};

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    public CreateTransaction(APITag[] apiTags, String... parameters) {
        super(apiTags, addCommonParameters(parameters));
        if (!getAPITags().contains(APITag.CREATE_TRANSACTION)) {
            throw new RuntimeException("CreateTransaction API " + getClass().getName() + " is missing APITag.CREATE_TRANSACTION tag");
        }
    }

    public CreateTransaction(String fileParameter, APITag[] apiTags, String... parameters) {
        super(fileParameter, apiTags, addCommonParameters(parameters));
        if (!getAPITags().contains(APITag.CREATE_TRANSACTION)) {
            throw new RuntimeException("CreateTransaction API " + getClass().getName() + " is missing APITag.CREATE_TRANSACTION tag");
        }
    }

    public JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
            throws AplException {
        return createTransaction(req, senderAccount, 0, 0, attachment);
    }

    public JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountATM)
            throws AplException {
        return createTransaction(req, senderAccount, recipientId, amountATM, Attachment.ORDINARY_PAYMENT);
    }
    public JSONStreamAware createPrivateTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountATM)
            throws AplException {
        return createTransaction(req, senderAccount, recipientId, amountATM, Attachment.PRIVATE_PAYMENT);
    }


    public JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountATM, Attachment attachment) throws AplException.ValidationException, ParameterException {
        CreateTransactionRequest createTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                .convert(req, senderAccount, recipientId, amountATM, attachment);


        JSONObject response = new JSONObject();
        Transaction transaction = createTransaction(createTransactionRequest);


        JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
        response.put("transactionJSON", transactionJSON);
        try {
            response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
        } catch (AplException.NotYetEncryptedException ignore) {}
        if (createTransactionRequest.getKeySeed() != null) {
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transactionJSON.get("fullHash"));
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            response.put("signatureHash", transactionJSON.get("signatureHash"));
        }
        if (createTransactionRequest.isBroadcast()) {
            response.put("broadcasted", true);
        } else {
            response.put("broadcasted", false);
        }

        return response;
    }

    public Transaction createTransaction(CreateTransactionRequest txRequest) throws AplException.ValidationException, ParameterException {
        EncryptedMessageAppendix encryptedMessage = null;
        PrunableEncryptedMessageAppendix prunableEncryptedMessage = null;

        if (txRequest.getAttachment().getTransactionType().canHaveRecipient() && txRequest.getRecipientId() != 0) {
            if (txRequest.isEncryptedMessageIsPrunable()) {
                prunableEncryptedMessage = (PrunableEncryptedMessageAppendix) txRequest.getAppendix();
            } else {
                encryptedMessage = (EncryptedMessageAppendix) txRequest.getAppendix();
            }
        }

        MessageAppendix message = txRequest.isMessageIsPrunable() ? null : (MessageAppendix) txRequest.getMessage();
        PrunablePlainMessageAppendix prunablePlainMessage = txRequest.isMessageIsPrunable() ? (PrunablePlainMessageAppendix) txRequest.getMessage() : null;

        PublicKeyAnnouncementAppendix publicKeyAnnouncement = null;
        if (txRequest.getRecipientPublicKey() != null) {
            publicKeyAnnouncement = new PublicKeyAnnouncementAppendix(Convert.parseHexString(txRequest.getRecipientPublicKey()));
        }

        if (txRequest.getKeySeed() == null && txRequest.getPublicKeyValue() == null) {
            throw new AplException.NotValidException(MISSING_SECRET_PHRASE);
        } else if (txRequest.getDeadlineValue() == null) {
            throw new AplException.NotValidException(MISSING_DEADLINE);
        }

        short deadline;
        try {
            deadline = Short.parseShort(txRequest.getDeadlineValue());
            if (deadline < 1) {
                throw new AplException.NotValidException(INCORRECT_DEADLINE);
            }
        } catch (NumberFormatException e) {
            throw new AplException.NotValidException(INCORRECT_DEADLINE);
        }

        Blockchain blockchain = lookupBlockchain();
        if (txRequest.getEcBlockId() != 0 && txRequest.getEcBlockId() != blockchain.getBlockIdAtHeight(txRequest.getEcBlockHeight())) {
            throw new AplException.NotValidException(INCORRECT_EC_BLOCK);
        }
        if (txRequest.getEcBlockId() == 0 && txRequest.getEcBlockHeight() > 0) {
            txRequest.setEcBlockId(blockchain.getBlockIdAtHeight(txRequest.getEcBlockHeight()));
        }

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        int timestamp = timeService.getEpochTime();
        Transaction transaction;
        try {
            Transaction.Builder builder = Transaction.newTransactionBuilder(txRequest.getPublicKey(), txRequest.getAmountATM(), txRequest.getFeeATM(),
                    deadline, txRequest.getAttachment(), timestamp).referencedTransactionFullHash(txRequest.getReferencedTransactionFullHash());
            if (txRequest.getAttachment().getTransactionType().canHaveRecipient()) {
                builder.recipientId(txRequest.getRecipientId());
            }
            builder.appendix(encryptedMessage);
            builder.appendix(message);
            builder.appendix(publicKeyAnnouncement);
            builder.appendix(txRequest.getEncryptToSelfMessage());
            builder.appendix(txRequest.getPhasing());
            builder.appendix(prunablePlainMessage);
            builder.appendix(prunableEncryptedMessage);
            if (txRequest.getEcBlockId() != 0) {
                builder.ecBlockId(txRequest.getEcBlockId());
                builder.ecBlockHeight(txRequest.getEcBlockHeight());
            }
            transaction = builder.build(txRequest.getKeySeed());
            if (txRequest.getFeeATM() <= 0 || (propertiesHolder.correctInvalidFees() && txRequest.getKeySeed() == null)) {
                int effectiveHeight = blockchain.getHeight();
                long minFee = feeCalculator.getMinimumFeeATM(transaction, effectiveHeight);
                txRequest.setFeeATM(Math.max(minFee, txRequest.getFeeATM()));
                transaction.setFeeATM(txRequest.getFeeATM());
            }

            try {
                if (Math.addExact(txRequest.getAmountATM(), transaction.getFeeATM()) > txRequest.getSenderAccount().getUnconfirmedBalanceATM()) {
                    throw new AplException.NotValidException(NOT_ENOUGH_FUNDS);
                }
            } catch (ArithmeticException e) {
                throw new AplException.NotValidException(NOT_ENOUGH_FUNDS);
            }

            if (txRequest.isBroadcast()) {
                lookupTransactionProcessor().broadcast(transaction);
            } else {
                validator.validate(transaction);
            }
        } catch (AplException.NotYetEnabledException e) {
            throw new AplException.NotValidException(FEATURE_NOT_AVAILABLE);
        } catch (AplException.InsufficientBalanceException e) {
            throw e;
        }

        return transaction;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected String vaultAccountName() {
        return "sender";
    }

    @Override
    protected boolean is2FAProtected() {
        return true;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

}
