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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DEADLINE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_EC_BLOCK;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_LINKED_FULL_HASH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_WHITELIST;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_DEADLINE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_SECRET_PHRASE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.NOT_ENOUGH_FUNDS;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Appendix;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.PhasingParams;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[]{"secretPhrase", "publicKey", "feeATM",
            "deadline", "referencedTransactionFullHash", "broadcast",
            "message","messageSize", "messageIsText", "messageIsPrunable",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf",
            "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel",
            "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted",
            "phasingLinkedFullHash", "phasingLinkedFullHash", "phasingLinkedFullHash",
            "phasingHashedSecret", "phasingHashedSecretAlgorithm",
            "recipientPublicKey",
            "ecBlockId", "ecBlockHeight", "calculateFee"};

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    CreateTransaction(APITag[] apiTags, String... parameters) {
        super(apiTags, addCommonParameters(parameters));
        if (!getAPITags().contains(APITag.CREATE_TRANSACTION)) {
            throw new RuntimeException("CreateTransaction API " + getClass().getName() + " is missing APITag.CREATE_TRANSACTION tag");
        }
    }

    CreateTransaction(String fileParameter, APITag[] apiTags, String... parameters) {
        super(fileParameter, apiTags, addCommonParameters(parameters));
        if (!getAPITags().contains(APITag.CREATE_TRANSACTION)) {
            throw new RuntimeException("CreateTransaction API " + getClass().getName() + " is missing APITag.CREATE_TRANSACTION tag");
        }
    }
//
//    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
//            throws AplException {
//        return createTransaction(req, senderAccount, 0, 0, attachment);
//    }
//
//    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountATM)
//            throws AplException {
//        return createTransaction(req, senderAccount, recipientId, amountATM, Attachment.ORDINARY_PAYMENT);
//    }
//    final JSONStreamAware createPrivateTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountATM)
//            throws AplException {
//        return createTransaction(req, senderAccount, recipientId, amountATM, Attachment.PRIVATE_PAYMENT);
//    }

    private Appendix.Phasing parsePhasing(HttpServletRequest req) throws ParameterException {
        int finishHeight = ParameterParser.getInt(req, "phasingFinishHeight",
                Apl.getBlockchain().getHeight() + 1,
                Apl.getBlockchain().getHeight() + Constants.MAX_PHASING_DURATION + 1,
                true);
        
        PhasingParams phasingParams = parsePhasingParams(req, "phasing");
        
        byte[][] linkedFullHashes = null;
        String[] linkedFullHashesValues = req.getParameterValues("phasingLinkedFullHash");
        if (linkedFullHashesValues != null && linkedFullHashesValues.length > 0) {
            linkedFullHashes = new byte[linkedFullHashesValues.length][];
            for (int i = 0; i < linkedFullHashes.length; i++) {
                linkedFullHashes[i] = Convert.parseHexString(linkedFullHashesValues[i]);
                if (Convert.emptyToNull(linkedFullHashes[i]) == null || linkedFullHashes[i].length != 32) {
                    throw new ParameterException(INCORRECT_LINKED_FULL_HASH);
                }
            }
        }

        byte[] hashedSecret = Convert.parseHexString(Convert.emptyToNull(req.getParameter("phasingHashedSecret")));
        byte algorithm = ParameterParser.getByte(req, "phasingHashedSecretAlgorithm", (byte) 0, Byte.MAX_VALUE, false);

        return new Appendix.Phasing(finishHeight, phasingParams, linkedFullHashes, hashedSecret, algorithm);
    }

    final PhasingParams parsePhasingParams(HttpServletRequest req, String parameterPrefix) throws ParameterException {
        byte votingModel = ParameterParser.getByte(req, parameterPrefix + "VotingModel", (byte)-1, (byte)5, true);
        long quorum = ParameterParser.getLong(req, parameterPrefix + "Quorum", 0, Long.MAX_VALUE, false);
        long minBalance = ParameterParser.getLong(req, parameterPrefix + "MinBalance", 0, Long.MAX_VALUE, false);
        byte minBalanceModel = ParameterParser.getByte(req, parameterPrefix + "MinBalanceModel", (byte)0, (byte)3, false);
        long holdingId = ParameterParser.getUnsignedLong(req, parameterPrefix + "Holding", false);
        long[] whitelist = null;
        String[] whitelistValues = req.getParameterValues(parameterPrefix + "Whitelisted");
        if (whitelistValues != null && whitelistValues.length > 0) {
            whitelist = new long[whitelistValues.length];
            for (int i = 0; i < whitelistValues.length; i++) {
                whitelist[i] = Convert.parseAccountId(whitelistValues[i]);
                if (whitelist[i] == 0) {
                    throw new ParameterException(INCORRECT_WHITELIST);
                }
            }
        }
        return new PhasingParams(votingModel, holdingId, quorum, minBalance, minBalanceModel, whitelist);
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        boolean calculateFee = ParameterParser.getBoolean(req, "calculateFee", false);
        CreateTransactionRequestData data = parseRequest(req, !calculateFee);
        if (data.getErrorJson() != null) {
            return data.getErrorJson();
        }
        Attachment attachment = data.getAttachment();
        long recipientId = data.getRecipientId();
        Account senderAccount = data.getSenderAccount();
        long amountATM = data.getAmountATM();
        return processRequest(req, attachment, recipientId, amountATM, senderAccount, calculateFee, data.getInsufficientBalanceErrorJson());
    }
    protected JSONStreamAware processRequest(HttpServletRequest req, Attachment attachment, long recipientId, long amountATM, Account senderAccount
            , boolean calculateFee, JSONStreamAware insufficientBalanceJsonError) throws ParameterException, AplException.InsufficientBalanceException {
        long accountId = senderAccount == null ? 0 : senderAccount.getId();
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter("referencedTransactionFullHash"));
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, false));
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast")) && (secretPhrase != null || passphrase != null);
        Appendix.EncryptedMessage encryptedMessage = null;
        Appendix.PrunableEncryptedMessage prunableEncryptedMessage = null;
        if (attachment.getTransactionType().canHaveRecipient() && (recipientId != 0 || calculateFee)) {
            Account recipient = calculateFee ? null : Account.getAccount(recipientId);
            if ("true".equalsIgnoreCase(req.getParameter("encryptedMessageIsPrunable"))) {
                prunableEncryptedMessage = (Appendix.PrunableEncryptedMessage) (calculateFee ?
                        ParameterParser.getEncryptedMessageFeeAppendix(req, true) :
                        ParameterParser.getEncryptedMessage(req, recipient, accountId,true));
            } else {
                encryptedMessage = (Appendix.EncryptedMessage) (calculateFee ?
                        ParameterParser.getEncryptedMessageFeeAppendix(req, false) :
                        ParameterParser.getEncryptedMessage(req, recipient, accountId,false));
            }
        }
        Appendix.EncryptToSelfMessage encryptToSelfMessage = calculateFee ?
                ParameterParser.getEncryptToSelfMessageFeeAppendix(req) :
                ParameterParser.getEncryptToSelfMessage(req, accountId);
        Appendix.Message message = null;
        Appendix.PrunablePlainMessage prunablePlainMessage = null;
        if ("true".equalsIgnoreCase(req.getParameter("messageIsPrunable"))) {
            prunablePlainMessage = (Appendix.PrunablePlainMessage) ParameterParser.getPlainMessage(req, true, calculateFee);
        } else {
            message = (Appendix.Message) ParameterParser.getPlainMessage(req, false, calculateFee);
        }
        Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
        String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
        if (recipientPublicKey != null) {
            publicKeyAnnouncement = new Appendix.PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey));
        }

        Appendix.Phasing phasing = null;
        boolean phased = "true".equalsIgnoreCase(req.getParameter("phased"));
        if (phased) {
            phasing = parsePhasing(req);
        }

        if (secretPhrase == null && publicKeyValue == null && passphrase == null && !calculateFee) {
            return MISSING_SECRET_PHRASE;
        } else if (deadlineValue == null && !calculateFee) {
            return MISSING_DEADLINE;
        }

        short deadline = 0;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1 && !calculateFee) {
                return INCORRECT_DEADLINE;
            }
        } catch (NumberFormatException e) {
            if (!calculateFee) {
                return INCORRECT_DEADLINE;
            }
        }
        long feeATM = ParameterParser.getFeeATM(req, !calculateFee);
        int ecBlockHeight = ParameterParser.getInt(req, "ecBlockHeight", 0, Integer.MAX_VALUE, false);
        long ecBlockId = Convert.parseUnsignedLong(req.getParameter("ecBlockId"));
        if (ecBlockId != 0 && ecBlockId != Apl.getBlockchain().getBlockIdAtHeight(ecBlockHeight) && !calculateFee) {
            return INCORRECT_EC_BLOCK;
        }
        if (ecBlockId == 0 && ecBlockHeight > 0) {
            ecBlockId = Apl.getBlockchain().getBlockIdAtHeight(ecBlockHeight);
        }

        JSONObject response = new JSONObject();

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = ParameterParser.getPublicKey(req, null, accountId, false);
        try {
            Transaction.Builder builder = Apl.newTransactionBuilder(publicKey, amountATM, calculateFee ? 0 : feeATM,
                    deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
            if (attachment.getTransactionType().canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            builder.appendix(encryptedMessage);
            builder.appendix(message);
            builder.appendix(publicKeyAnnouncement);
            builder.appendix(encryptToSelfMessage);
            builder.appendix(phasing);
            builder.appendix(prunablePlainMessage);
            builder.appendix(prunableEncryptedMessage);
            if (ecBlockId != 0) {
                builder.ecBlockId(ecBlockId);
                builder.ecBlockHeight(ecBlockHeight);
            }
            byte[] keySeed = ParameterParser.getKeySeed(req, accountId, false);
            Transaction transaction = builder.build(keySeed);
            if (calculateFee) {
                response.put("feeATM", transaction.getFeeATM());
                return response;
            }
            try {
                if (Math.addExact(amountATM, transaction.getFeeATM()) > senderAccount.getUnconfirmedBalanceATM()) {
                    return NOT_ENOUGH_FUNDS;
                }
            } catch (ArithmeticException e) {
                return NOT_ENOUGH_FUNDS;
            }
            JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
            response.put("transactionJSON", transactionJSON);
            try {
                response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
            } catch (AplException.NotYetEncryptedException ignore) {}
            if (keySeed != null) {
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transactionJSON.get("fullHash"));
                response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                response.put("signatureHash", transactionJSON.get("signatureHash"));
            }
            if (broadcast) {
                Apl.getTransactionProcessor().broadcast(transaction);
                response.put("broadcasted", true);
            } else {
                transaction.validate();
                response.put("broadcasted", false);
            }
        } catch (AplException.NotYetEnabledException e) {
            return FEATURE_NOT_AVAILABLE;
        } catch (AplException.InsufficientBalanceException e) {;
            if (insufficientBalanceJsonError != null) {
                return insufficientBalanceJsonError;
            }
            throw e;
        } catch (AplException.ValidationException e) {
            if (broadcast) {
                response.clear();
            }
            response.put("broadcasted", false);
            JSONData.putException(response, e);
        }
        return response;
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

    protected abstract CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException;

    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        return parseRequest(req);
    }
}
