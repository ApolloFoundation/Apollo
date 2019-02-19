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

package com.apollocurrency.aplwallet.apl.core.http;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ACCOUNT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ALIAS;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DATA_TOO_LONG;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DATA_ZERO_LENGTH;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_HEIGHT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_MESSAGE_TO_ENCRYPT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PURCHASE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_CHANNEL;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_DESCRIPTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_FILE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_FILENAME;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_NAME;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_TAGS;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_TYPE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_ACCOUNT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_ALIAS_OR_ALIAS_NAME;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_NAME;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_PROPERTY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_RECIPIENT_PUBLIC_KEY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SECRET_PHRASE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ACCOUNT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ALIAS;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ASSET;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_CURRENCY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_GOODS;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_OFFER;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_POLL;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_SHUFFLING;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.either;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.missing;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UnencryptedEncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UnencryptedEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UnencryptedPrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.SecretBytesDetails;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUpload;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Search;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

public final class ParameterParser {
    private static final Logger LOG = getLogger(ParameterParser.class);
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

    public static byte getByte(HttpServletRequest req, String name, byte min, byte max, boolean isMandatory, byte defaultValue) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return defaultValue;
        }
        try {
            byte value = Byte.parseByte(paramValue);
            if (value < min || value > max) {
                throw new ParameterException(incorrect(name, String.format("value %d not in range [%d-%d]", value, min, max)));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name, String.format("value %s is not numeric", paramValue)));
        }
    }

    public static byte getByte(HttpServletRequest req, String name, byte min, byte max, boolean isMandatory) throws ParameterException {
        return getByte(req, name, min, max, isMandatory, (byte) 0);
    }

    public static int getInt(HttpServletRequest req, String name, int min, int max, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            int value = Integer.parseInt(paramValue);
            if (value < min || value > max) {
                throw new ParameterException(incorrect(name, String.format("value %d not in range [%d-%d]", value, min, max)));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name, String.format("value %s is not numeric", paramValue)));
        }
    }

    public static long getLong(HttpServletRequest req, String name, long min, long max,
                        boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
//            using bigInteger to handle all possible numbers
            BigInteger bigIntegerValue = new BigInteger(paramValue);
            if (bigIntegerValue.compareTo(BigInteger.valueOf(min)) < 0 || bigIntegerValue.compareTo(BigInteger.valueOf(max)) > 0) {
                throw new ParameterException(incorrect(name, String.format("value %s not in range [%d-%d]", bigIntegerValue, min, max)));
            }
            return bigIntegerValue.longValue();
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name, String.format("value %s is not numeric", paramValue)));
        }
    }

    public static long getUnsignedLong(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            long value = Convert.parseUnsignedLong(paramValue);
            if (value == 0) { // 0 is not allowed as an id
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    public static long[] getUnsignedLongs(HttpServletRequest req, String name) throws ParameterException {
        String[] paramValues = req.getParameterValues(name);
        if (paramValues == null || paramValues.length == 0) {
            throw new ParameterException(missing(name));
        }
        long[] values = new long[paramValues.length];
        try {
            for (int i = 0; i < paramValues.length; i++) {
                if (paramValues[i] == null || paramValues[i].isEmpty()) {
                    throw new ParameterException(incorrect(name));
                }
                values[i] = Long.parseUnsignedLong(paramValues[i]);
                if (values[i] == 0) {
                    throw new ParameterException(incorrect(name));
                }
            }
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
        return values;
    }

    public static byte[] getBytes(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return Convert.EMPTY_BYTE;
        }
        return Convert.parseHexString(paramValue);
    }
    public static byte getByteOrNegative(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        return getByte(req, name, Byte.MIN_VALUE, Byte.MAX_VALUE, isMandatory, (byte) -1);
    }


    public static long getAccountId(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        return getAccountId(req, "account", isMandatory);
    }

    public static long getAccountId(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            long value = Convert.parseAccountId(paramValue);
            if (value == 0) {
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    public static long[] getAccountIds(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        String[] paramValues = req.getParameterValues("account");
        if (paramValues == null || paramValues.length == 0) {
            if (isMandatory) {
                throw new ParameterException(MISSING_ACCOUNT);
            } else {
                return Convert.EMPTY_LONG;
            }
        }
        long[] values = new long[paramValues.length];
        try {
            for (int i = 0; i < paramValues.length; i++) {
                if (paramValues[i] == null || paramValues[i].isEmpty()) {
                    throw new ParameterException(INCORRECT_ACCOUNT);
                }
                values[i] = Convert.parseAccountId(paramValues[i]);
                if (values[i] == 0) {
                    throw new ParameterException(INCORRECT_ACCOUNT);
                }
            }
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ACCOUNT);
        }
        return values;
    }

    public static Alias getAlias(HttpServletRequest req) throws ParameterException {
        long aliasId;
        try {
            aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter("alias")));
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ALIAS);
        }
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        Alias alias;
        if (aliasId != 0) {
            alias = Alias.getAlias(aliasId);
        } else if (aliasName != null) {
            alias = Alias.getAlias(aliasName);
        } else {
            throw new ParameterException(MISSING_ALIAS_OR_ALIAS_NAME);
        }
        if (alias == null) {
            throw new ParameterException(UNKNOWN_ALIAS);
        }
        return alias;
    }

    public static long getAmountATM(HttpServletRequest req) throws ParameterException {
        return getLong(req, "amountATM", 1L, blockchainConfig.getCurrentConfig().getMaxBalanceATM(), true);
    }

    public static long getFeeATM(HttpServletRequest req) throws ParameterException {
        return getLong(req, "feeATM", 0L, blockchainConfig.getCurrentConfig().getMaxBalanceATM(), true);
    }

    public static long getPriceATM(HttpServletRequest req) throws ParameterException {
        return getLong(req, "priceATM", 1L, blockchainConfig.getCurrentConfig().getMaxBalanceATM(), true);
    }

    public static Poll getPoll(HttpServletRequest req) throws ParameterException {
        Poll poll = Poll.getPoll(getUnsignedLong(req, "poll", true));
        if (poll == null) {
            throw new ParameterException(UNKNOWN_POLL);
        }
        return poll;
    }

    public static Asset getAsset(HttpServletRequest req) throws ParameterException {
        Asset asset = Asset.getAsset(getUnsignedLong(req, "asset", true));
        if (asset == null) {
            throw new ParameterException(UNKNOWN_ASSET);
        }
        return asset;
    }

    public static Currency getCurrency(HttpServletRequest req) throws ParameterException {
        return getCurrency(req, true);
    }

    public static Currency getCurrency(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        Currency currency = Currency.getCurrency(getUnsignedLong(req, "currency", isMandatory));
        if (isMandatory && currency == null) {
            throw new ParameterException(UNKNOWN_CURRENCY);
        }
        return currency;
    }

    public static CurrencyBuyOffer getBuyOffer(HttpServletRequest req) throws ParameterException {
        CurrencyBuyOffer offer = CurrencyBuyOffer.getOffer(getUnsignedLong(req, "offer", true));
        if (offer == null) {
            throw new ParameterException(UNKNOWN_OFFER);
        }
        return offer;
    }

    public static CurrencySellOffer getSellOffer(HttpServletRequest req) throws ParameterException {
        CurrencySellOffer offer = CurrencySellOffer.getOffer(getUnsignedLong(req, "offer", true));
        if (offer == null) {
            throw new ParameterException(UNKNOWN_OFFER);
        }
        return offer;
    }

    public static Shuffling getShuffling(HttpServletRequest req) throws ParameterException {
        Shuffling shuffling = Shuffling.getShuffling(getUnsignedLong(req, "shuffling", true));
        if (shuffling == null) {
            throw new ParameterException(UNKNOWN_SHUFFLING);
        }
        return shuffling;
    }

    public static long getQuantityATU(HttpServletRequest req) throws ParameterException {
        return getLong(req, "quantityATU", 1L, Constants.MAX_ASSET_QUANTITY_ATU, true);
    }

    public static long getAmountATMPerATU(HttpServletRequest req) throws ParameterException {
        return getLong(req, "amountATMPerATU", 1L, blockchainConfig.getCurrentConfig().getMaxBalanceATM(), true);
    }

    public static DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
        DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(getUnsignedLong(req, "goods", true));
        if (goods == null) {
            throw new ParameterException(UNKNOWN_GOODS);
        }
        return goods;
    }

    public static int getGoodsQuantity(HttpServletRequest req) throws ParameterException {
        return getInt(req, "quantity", 0, Constants.MAX_DGS_LISTING_QUANTITY, true);
    }

    public static EncryptedData getEncryptedData(HttpServletRequest req, String messageType) throws ParameterException {
        String dataString = Convert.emptyToNull(req.getParameter(messageType + "Data"));
        String nonceString = Convert.emptyToNull(req.getParameter(messageType + "Nonce"));
        if (nonceString == null) {
            return null;
        }
        byte[] data;
        byte[] nonce;
        try {
            nonce = Convert.parseHexString(nonceString);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.incorrect(messageType + "Nonce"));
        }
        if (dataString != null) {
            try {
                data = Convert.parseHexString(dataString);
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.incorrect(messageType + "Data"));
            }
        } else {
            if (req.getContentType() == null || !req.getContentType().startsWith("multipart/form-data")) {
                return null;
            }
            try {
                Part part = req.getPart(messageType + "File");
                if (part == null) {
                    return null;
                }
                FileData fileData = new FileData(part).invoke();
                data = fileData.getData();
            } catch (IOException | ServletException e) {
                LOG.debug("error in reading file data", e);
                throw new ParameterException(JSONResponses.incorrect(messageType + "File"));
            }
        }
        return new EncryptedData(data, nonce);
    }

    public static EncryptToSelfMessageAppendix getEncryptToSelfMessage(HttpServletRequest req, long senderId) throws ParameterException {
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText"));
        boolean compress = !"false".equalsIgnoreCase(req.getParameter("compressMessageToEncryptToSelf"));
        byte[] plainMessageBytes = null;
        EncryptedData encryptedData = ParameterParser.getEncryptedData(req, "encryptToSelfMessage");
        if (encryptedData == null) {
            String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncryptToSelf"));
            if (plainMessage == null) {
                return null;
            }
            try {
                plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_MESSAGE_TO_ENCRYPT);
            }
            byte[] keySeed = getKeySeed(req, senderId, false);
            if (keySeed != null) {
                byte[] publicKey = Crypto.getPublicKey(keySeed);
                encryptedData = Account.encryptTo(publicKey, plainMessageBytes, keySeed, compress);
            }
        }
        if (encryptedData != null) {
            return new EncryptToSelfMessageAppendix(encryptedData, isText, compress);
        } else {
            return new UnencryptedEncryptToSelfMessageAppendix(plainMessageBytes, isText, compress);
        }
    }

    public static byte[] getKeySeed(HttpServletRequest req, long senderId, boolean isMandatory) throws ParameterException {

        byte[] secretBytes = getSecretBytes(req, senderId, isMandatory);
        return secretBytes == null ? null : Crypto.getKeySeed(secretBytes);
    }
    public static byte[] getSecretBytes(HttpServletRequest req, long senderId, boolean isMandatory) throws ParameterException {
        String secretPhrase = getSecretPhrase(req, false);
        if (secretPhrase != null) {
            return Convert.toBytes(secretPhrase);
        }
        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, false));
        if (passphrase != null) {
            SecretBytesDetails secretBytes = Helper2FA.findSecretBytes(senderId, passphrase, isMandatory);
            return secretBytes == null ? null : secretBytes.getSecretBytes();
        }
        if (isMandatory) {
            throw new ParameterException("Secret phrase or valid passphrase + accountId required", null, JSONResponses.incorrect("secretPhrase",
                    "passphrase"));
        }
        return null;
    }

    public static DigitalGoodsStore.Purchase getPurchase(HttpServletRequest req) throws ParameterException {
        DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(getUnsignedLong(req, "purchase", true));
        if (purchase == null) {
            throw new ParameterException(INCORRECT_PURCHASE);
        }
        return purchase;
    }

    public static String getSecretPhrase(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null && isMandatory) {
            throw new ParameterException(MISSING_SECRET_PHRASE);
        }
        return secretPhrase;
    }

    public static byte[] getPublicKey(HttpServletRequest req) throws ParameterException {
        return getPublicKey(req, null);
    }

    public static byte[] getPublicKey(HttpServletRequest req, String prefix) throws ParameterException {
        return getPublicKey(req, prefix, 0);
    }

    public static byte[] getPublicKey(HttpServletRequest req, long accountId) throws ParameterException {
        return getPublicKey(req, null, accountId);
    }

    public static byte[] getPublicKey(HttpServletRequest request, String prefix, long accountId) throws ParameterException {
        return getPublicKey(request, prefix, accountId, true);
    }
    public static byte[] getPublicKey(HttpServletRequest req, String prefix, long accountId, boolean isMandatory) throws ParameterException {
        String secretPhraseParam = prefix == null ? "secretPhrase" : (prefix + "SecretPhrase");
        String publicKeyParam = prefix == null ? "publicKey" : (prefix + "PublicKey");
        String passphraseParam = prefix == null ? "passphrase" : (prefix + "Passphrase");
        String secretPhrase = Convert.emptyToNull(req.getParameter(secretPhraseParam));
        if (secretPhrase == null) {
            try {
                byte[] publicKey = Convert.parseHexString(Convert.emptyToNull(req.getParameter(publicKeyParam)));
                if (publicKey == null) {
                    String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, passphraseParam,false));
                    if (accountId == 0 || passphrase == null) {
                        if (isMandatory) {
                            throw new ParameterException(missing(secretPhraseParam, publicKeyParam, passphraseParam));
                        }
                    } else {
                        SecretBytesDetails secretBytesDetails = Helper2FA.findSecretBytes(accountId, passphrase, isMandatory);
                        if (secretBytesDetails != null && secretBytesDetails.getSecretBytes() != null) {
                            return Crypto.getPublicKey(Crypto.getKeySeed(secretBytesDetails.getSecretBytes()));
                        }
                    }
                } else {

                    if (!Crypto.isCanonicalPublicKey(publicKey)) {
                        if (isMandatory) {
                            throw new ParameterException(incorrect(publicKeyParam));
                        }
                    } else {
                        return publicKey;
                    }
                }
            } catch (RuntimeException e) {
                if (isMandatory) {
                    throw new ParameterException(incorrect(publicKeyParam));
                }
            }
        } else {
            return Crypto.getPublicKey(secretPhrase);
        }
        return null;
    }

    public static byte[] getPublicKey(HttpServletRequest request, boolean isMandatory) throws ParameterException {
        return getPublicKey(request, null, 0, isMandatory);
    }


    public static Account getSenderAccount(HttpServletRequest req, String accountName) throws ParameterException {
        String accountParam = accountName == null ? "sender" : accountName;
        long accountId = ParameterParser.getAccountId(req, accountParam, false);
        byte[] publicKey = getPublicKey(req, accountId);
        Account account = Account.getAccount(publicKey);
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        return account;
    }
    public static Account getSenderAccount(HttpServletRequest req) throws ParameterException {
        return getSenderAccount(req, null);
    }
    public static Account getAccount(HttpServletRequest req) throws ParameterException {
        return getAccount(req, true);
    }

    public static Account getAccount(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        long accountId = getAccountId(req, "account", isMandatory);
        if (accountId == 0 && !isMandatory) {
            return null;
        }
        Account account = Account.getAccount(accountId);
        if (account == null) {
            throw new ParameterException(JSONResponses.unknownAccount(accountId));
        }
        return account;
    }

    public static String getStringParameter(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String parameter = Convert.emptyToNull(req.getParameter(name));
        if (parameter == null && isMandatory) {
            throw new ParameterException(JSONResponses.missing(name));
        }
        return parameter;
    }

    public static String getPassphrase(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        return getStringParameter(req, "passphrase", isMandatory);
    }
    public static String getPassphrase(HttpServletRequest req,String parameterName, boolean isMandatory) throws ParameterException {
        return getStringParameter(req, parameterName, isMandatory);
    }

    public static byte[] getKeySeed(HttpServletRequest req, String parameterName, boolean isMandatory) throws ParameterException {
        String parameter = Convert.emptyToNull(getStringParameter(req, parameterName, isMandatory));
        if (parameter == null) {
            return null;
        }
        byte[] keySeed = Convert.parseHexString(parameter);
        if (keySeed.length != 32) {
            throw new ParameterException(incorrect(parameterName, "32 bytes in hex format required"));
        }
        return keySeed;
    }

    public static List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
        String[] accountValues = req.getParameterValues("account");
        if (accountValues == null || accountValues.length == 0) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        List<Account> result = new ArrayList<>();
        for (String accountValue : accountValues) {
            if (accountValue == null || accountValue.equals("")) {
                continue;
            }
            try {
                Account account = Account.getAccount(Convert.parseAccountId(accountValue));
                if (account == null) {
                    throw new ParameterException(UNKNOWN_ACCOUNT);
                }
                result.add(account);
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ACCOUNT);
            }
        }
        return result;
    }

    public static int getTimestamp(HttpServletRequest req) throws ParameterException {
        return getInt(req, "timestamp", 0, Integer.MAX_VALUE, false);
    }

    public static int getFirstIndex(HttpServletRequest req) {
        try {
            int firstIndex = Integer.parseInt(req.getParameter("firstIndex"));
            if (firstIndex < 0) {
                return 0;
            }
            return firstIndex;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int getLastIndex(HttpServletRequest req) {
        int lastIndex = Integer.MAX_VALUE;
        try {
            lastIndex = Integer.parseInt(req.getParameter("lastIndex"));
            if (lastIndex < 0) {
                lastIndex = Integer.MAX_VALUE;
            }
        } catch (NumberFormatException ignored) {}
        if (!API.checkPassword(req)) {
            int firstIndex = Math.min(getFirstIndex(req), Integer.MAX_VALUE - API.maxRecords + 1);
            lastIndex = Math.min(lastIndex, firstIndex + API.maxRecords - 1);
        }
        return lastIndex;
    }

    public static int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
        return getInt(req, "numberOfConfirmations", 0, blockchain.getHeight(), false);
    }

    public static int getHeight(HttpServletRequest req) throws ParameterException {
        String heightValue = Convert.emptyToNull(req.getParameter("height"));
        if (heightValue != null) {
            try {
                int height = Integer.parseInt(heightValue);
                if (height < 0 || height > blockchain.getHeight()) {
                    throw new ParameterException(INCORRECT_HEIGHT);
                }
                return height;
            } catch (NumberFormatException e) {
                throw new ParameterException(INCORRECT_HEIGHT);
            }
        }
        return -1;
    }

    public static HoldingType getHoldingType(HttpServletRequest req) throws ParameterException {
        return HoldingType.get(ParameterParser.getByte(req, "holdingType", (byte) 0, (byte) 2, false));
    }

    public static long getHoldingId(HttpServletRequest req, HoldingType holdingType) throws ParameterException {
        long holdingId = ParameterParser.getUnsignedLong(req, "holding", holdingType != HoldingType.APL);
        if (holdingType == HoldingType.APL && holdingId != 0) {
            throw new ParameterException(JSONResponses.incorrect("holding",
                    "holding id should not be specified if holdingType is " + blockchainConfig.getCoinSymbol()));
        }
        return holdingId;
    }

    public static String getAccountProperty(HttpServletRequest req, boolean isMandatory) throws ParameterException {
        String property = Convert.emptyToNull(req.getParameter("property"));
        if (property == null && isMandatory) {
            throw new ParameterException(MISSING_PROPERTY);
        }
        return property;
    }

    public static String getSearchQuery(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query")).trim();
        String tags = Convert.nullToEmpty(req.getParameter("tag")).trim();
        if (query.isEmpty() && tags.isEmpty()) {
            throw new ParameterException(JSONResponses.missing("query", "tag"));
        }
        if (!tags.isEmpty()) {
            StringJoiner stringJoiner = new StringJoiner(" AND TAGS:", "TAGS:", "");
            for (String tag : Search.parseTags(tags, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)) {
                stringJoiner.add(tag);
            }
            query = stringJoiner.toString() + (query.isEmpty() ? "" : (" AND (" + query + ")"));
        }
        return query;
    }

    public static Transaction.Builder parseTransaction(String transactionJSON, String transactionBytes, String prunableAttachmentJSON) throws ParameterException {
        if (transactionBytes == null && transactionJSON == null) {
            throw new ParameterException(MISSING_TRANSACTION_BYTES_OR_JSON);
        }
        if (transactionBytes != null && transactionJSON != null) {
            throw new ParameterException(either("transactionBytes", "transactionJSON"));
        }
        if (prunableAttachmentJSON != null && transactionBytes == null) {
            throw new ParameterException(JSONResponses.missing("transactionBytes"));
        }
        if (transactionJSON != null) {
            try {
                JSONObject json = (JSONObject) JSONValue.parseWithException(transactionJSON);
                return Transaction.newTransactionBuilder(json);
            } catch (AplException.ValidationException | RuntimeException | ParseException e) {
                LOG.debug(e.getMessage(), e);
                JSONObject response = new JSONObject();
                JSONData.putException(response, e, "Incorrect transactionJSON");
                throw new ParameterException(response);
            }
        } else {
            try {
                byte[] bytes = Convert.parseHexString(transactionBytes);
                JSONObject prunableAttachments = prunableAttachmentJSON == null ? null : (JSONObject)JSONValue.parseWithException(prunableAttachmentJSON);
                return Transaction.newTransactionBuilder(bytes, prunableAttachments);
            } catch (AplException.ValidationException|RuntimeException | ParseException e) {
                LOG.debug(e.getMessage(), e);
                JSONObject response = new JSONObject();
                JSONData.putException(response, e, "Incorrect transactionBytes");
                throw new ParameterException(response);
            }
        }
    }

    public static Appendix getPlainMessage(HttpServletRequest req, boolean prunable) throws ParameterException {
        String messageValue = Convert.emptyToNull(req.getParameter("message"));
        boolean messageIsText = !"false".equalsIgnoreCase(req.getParameter("messageIsText"));
        if (messageValue != null) {
            try {
                if (prunable) {
                    return new PrunablePlainMessageAppendix(messageValue, messageIsText);
                } else {
                    return new MessageAppendix(messageValue, messageIsText);
                }
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
            }
        }
        if (req.getContentType() == null || !req.getContentType().startsWith("multipart/form-data")) {
            return null;
        }
        try {
            Part part = req.getPart("messageFile");
            if (part == null) {
                return null;
            }
            FileData fileData = new FileData(part).invoke();
            byte[] message = fileData.getData();
            String detectedMimeType = Search.detectMimeType(message);
            if (detectedMimeType != null) {
                messageIsText = detectedMimeType.startsWith("text/");
            }
            if (messageIsText && !Arrays.equals(message, Convert.toBytes(Convert.toString(message)))) {
                messageIsText = false;
            }
            if (prunable) {
                return new PrunablePlainMessageAppendix(message, messageIsText);
            } else {
                return new MessageAppendix(message, messageIsText);
            }
        } catch (IOException | ServletException e) {
            LOG.debug("error in reading file data", e);
            throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
        }
    }

    public static Appendix getEncryptedMessage(HttpServletRequest req, Account recipient, long senderId, boolean prunable) throws ParameterException {
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText"));
        boolean compress = !"false".equalsIgnoreCase(req.getParameter("compressMessageToEncrypt"));
        byte[] plainMessageBytes = null;
        byte[] recipientPublicKey = null;
        EncryptedData encryptedData = ParameterParser.getEncryptedData(req, "encryptedMessage");
        if (encryptedData == null) {
            String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncrypt"));
            if (plainMessage == null) {
                if (req.getContentType() == null || !req.getContentType().startsWith("multipart/form-data")) {
                    return null;
                }
                try {
                    Part part = req.getPart("messageToEncryptFile");
                    if (part == null) {
                        return null;
                    }
                    FileData fileData = new FileData(part).invoke();
                    plainMessageBytes = fileData.getData();
                    String detectedMimeType = Search.detectMimeType(plainMessageBytes);
                    if (detectedMimeType != null) {
                        isText = detectedMimeType.startsWith("text/");
                    }
                    if (isText && !Arrays.equals(plainMessageBytes, Convert.toBytes(Convert.toString(plainMessageBytes)))) {
                        isText = false;
                    }
                } catch (IOException | ServletException e) {
                    LOG.debug("error in reading file data", e);
                    throw new ParameterException(INCORRECT_MESSAGE_TO_ENCRYPT);
                }
            } else {
                try {
                    plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
                } catch (RuntimeException e) {
                    throw new ParameterException(INCORRECT_MESSAGE_TO_ENCRYPT);
                }
            }
            if (recipient != null) {
                recipientPublicKey = Account.getPublicKey(recipient.getId());
            }
            if (recipientPublicKey == null) {
                recipientPublicKey = Convert.parseHexString(Convert.emptyToNull(req.getParameter("recipientPublicKey")));
            }
            if (recipientPublicKey == null) {
                throw new ParameterException(MISSING_RECIPIENT_PUBLIC_KEY);
            }
            byte[] keySeed = getKeySeed(req, senderId, false);
            if (keySeed != null) {
                encryptedData = Account.encryptTo(recipientPublicKey, plainMessageBytes, keySeed, compress);
            }
        }
        if (encryptedData != null) {
            if (prunable) {
                return new PrunableEncryptedMessageAppendix(encryptedData, isText, compress);
            } else {
                return new EncryptedMessageAppendix(encryptedData, isText, compress);
            }
        } else {
            if (prunable) {
                return new UnencryptedPrunableEncryptedMessageAppendix(plainMessageBytes, isText, compress, recipientPublicKey);
            } else {
                return new UnencryptedEncryptedMessageAppendix(plainMessageBytes, isText, compress, recipientPublicKey);
            }
        }
    }

    public static TaggedDataUpload getTaggedData(HttpServletRequest req) throws ParameterException, AplException.NotValidException {
        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String tags = Convert.nullToEmpty(req.getParameter("tags"));
        String type = Convert.nullToEmpty(req.getParameter("type")).trim();
        String channel = Convert.nullToEmpty(req.getParameter("channel"));
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
        String filename = Convert.nullToEmpty(req.getParameter("filename")).trim();
        String dataValue = Convert.emptyToNull(req.getParameter("data"));
        byte[] data;
        if (dataValue == null) {
            try {
                Part part = req.getPart("file");
                if (part == null) {
                    throw new ParameterException(INCORRECT_TAGGED_DATA_FILE);
                }
                FileData fileData = new FileData(part).invoke();
                data = fileData.getData();
                // Depending on how the client submits the form, the filename, can be a regular parameter
                // or encoded in the multipart form. If its not a parameter we take from the form
                if (filename.isEmpty() && fileData.getFilename() != null) {
                    filename = fileData.getFilename().trim();
                }
                if (name == null) {
                    name = filename;
                }
            } catch (IOException | ServletException e) {
                LOG.debug("error in reading file data", e);
                throw new ParameterException(INCORRECT_TAGGED_DATA_FILE);
            }
        } else {
            data = isText ? Convert.toBytes(dataValue) : Convert.parseHexString(dataValue);
        }

        String detectedMimeType = Search.detectMimeType(data, filename);
        if (detectedMimeType != null) {
            isText = detectedMimeType.startsWith("text/");
            if (type.isEmpty()) {
                type = detectedMimeType.substring(0, Math.min(detectedMimeType.length(), Constants.MAX_TAGGED_DATA_TYPE_LENGTH));
            }
        }

        if (name == null) {
            throw new ParameterException(MISSING_NAME);
        }
        name = name.trim();
        if (name.length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_NAME);
        }

        if (description.length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_DESCRIPTION);
        }

        if (tags.length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_TAGS);
        }

        type = type.trim();
        if (type.length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_TYPE);
        }

        channel = channel.trim();
        if (channel.length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_CHANNEL);
        }

        if (data.length == 0) {
            throw new ParameterException(INCORRECT_DATA_ZERO_LENGTH);
        }

        if (data.length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
            throw new ParameterException(INCORRECT_DATA_TOO_LONG);
        }

        if (filename.length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_FILENAME);
        }
        return new TaggedDataUpload(name, description, tags, type, channel, isText, filename, data);
    }

    public static PrivateTransactionsAPIData parsePrivateTransactionRequest(HttpServletRequest req) throws ParameterException {
        byte[] publicKey = Convert.emptyToNull(ParameterParser.getBytes(req, "publicKey", false));
        long account = ParameterParser.getAccountId(req, false);
        byte[] keySeed = ParameterParser.getKeySeed(req, account, false);
        if (keySeed == null && publicKey == null) {
            return null;
        }
        boolean encrypt = publicKey != null;
        // if public key specified in request -> encrypt private data
        // if keySeed is not null -> do not encrypt private data
        if (!encrypt) {
            publicKey = Crypto.getPublicKey(keySeed);
        }
        long accountId = Account.getId(publicKey);
        byte[] sharedKey = Crypto.getSharedKey(API.getServerPrivateKey(), publicKey);
        return new PrivateTransactionsAPIData(encrypt, publicKey, sharedKey, accountId);
    }

    public static TwoFactorAuthParameters parse2FARequest(HttpServletRequest req, String accountName, boolean isMandatory) throws ParameterException {
        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, false));
        String secretPhrase = Convert.emptyToNull(ParameterParser.getSecretPhrase(req, false));

        if (isMandatory && secretPhrase == null && passphrase == null) {
            throw new ParameterException(JSONResponses.missing("secretPhrase", "passphrase"));
        }
        if (secretPhrase != null && passphrase != null) {
            throw new ParameterException(JSONResponses.either("secretPhrase", "passphrase"));
        }
        long accountId = 0;
        if (passphrase != null) {
            accountId = ParameterParser.getAccountId(req, accountName, true);
        } else if (secretPhrase != null){
            accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        }
        return new TwoFactorAuthParameters(accountId, passphrase, secretPhrase);

    }

    public static TwoFactorAuthParameters parse2FARequest(HttpServletRequest req) throws ParameterException {
        return parse2FARequest(req, "account", true);

    }

    private ParameterParser() {} // never

    public static class PrivateTransactionsAPIData {
        private boolean encrypt;
        private byte[] publicKey;
        private byte[] sharedKey;
        private long accountId;

        public PrivateTransactionsAPIData() {
        }

        public PrivateTransactionsAPIData(boolean encrypt, byte[] publicKey, byte[] sharedKey, long accountId) {
            this.encrypt = encrypt;
            this.publicKey = publicKey;
            this.sharedKey = sharedKey;
            this.accountId = accountId;
        }

        public boolean isEncrypt() {
            return encrypt;
        }

        public void setEncrypt(boolean encrypt) {
            this.encrypt = encrypt;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(byte[] publicKey) {
            this.publicKey = publicKey;
        }

        public byte[] getSharedKey() {
            return sharedKey;
        }

        public void setSharedKey(byte[] sharedKey) {
            this.sharedKey = sharedKey;
        }

        public long getAccountId() {
            return accountId;
        }

        public void setAccountId(long accountId) {
            this.accountId = accountId;
        }

        public boolean isValid() {
            return publicKey != null;
        }
    }

    public static class FileData {
        private final Part part;
        private String filename;
        private byte[] data;

        public FileData(Part part) {
            this.part = part;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getData() {
            return data;
        }

        public FileData invoke() throws IOException {
            try (InputStream is = part.getInputStream()) {
                int nRead;
                byte[] bytes = new byte[1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((nRead = is.read(bytes, 0, bytes.length)) != -1) {
                    baos.write(bytes, 0, nRead);
                }
                data = baos.toByteArray();
                filename = part.getSubmittedFileName();
            }
            return this;
        }
    }
}
