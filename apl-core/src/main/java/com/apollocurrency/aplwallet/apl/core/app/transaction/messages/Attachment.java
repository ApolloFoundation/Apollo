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

package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.HoldingType;
import com.apollocurrency.aplwallet.apl.core.app.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.app.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.app.transaction.AccountControl;
import com.apollocurrency.aplwallet.apl.core.app.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Data;
import com.apollocurrency.aplwallet.apl.core.app.transaction.DigitalGoods;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Update;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Platform;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface Attachment extends Appendix {

    public TransactionType getTransactionType();



    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Payment.ORDINARY;
        }

    };

    EmptyAttachment PRIVATE_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Payment.PRIVATE;
        }

    };



    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Messaging.ARBITRARY_MESSAGE;
        }

    };





    interface MonetarySystemAttachment {

        long getCurrencyId();

    }

    final class MonetarySystemCurrencyIssuance extends AbstractAttachment {

        final String name;
        final String code;
        final String description;
        final byte type;
        final long initialSupply;
        final long reserveSupply;
        final long maxSupply;
        final int issuanceHeight;
        final long minReservePerUnitATM;
        final int minDifficulty;
        final int maxDifficulty;
        final byte ruleset;
        final byte algorithm;
        final byte decimals;

        public MonetarySystemCurrencyIssuance(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            try {
                this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_NAME_LENGTH);
                this.code = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_CODE_LENGTH);
                this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_CURRENCY_DESCRIPTION_LENGTH);
                this.type = buffer.get();
                this.initialSupply = buffer.getLong();
                this.reserveSupply = buffer.getLong();
                this.maxSupply = buffer.getLong();
                this.issuanceHeight = buffer.getInt();
                this.minReservePerUnitATM = buffer.getLong();
                this.minDifficulty = buffer.get() & 0xFF;
                this.maxDifficulty = buffer.get() & 0xFF;
                this.ruleset = buffer.get();
                this.algorithm = buffer.get();
                this.decimals = buffer.get();
            } catch (NotValidException ex) {
                throw new AplException.NotValidException(ex.getMessage());
            }
        }

        public MonetarySystemCurrencyIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String)attachmentData.get("name");
            this.code = (String)attachmentData.get("code");
            this.description = (String)attachmentData.get("description");
            this.type = ((Long)attachmentData.get("type")).byteValue();
            this.initialSupply = Convert.parseLong(attachmentData.get("initialSupply"));
            this.reserveSupply = Convert.parseLong(attachmentData.get("reserveSupply"));
            this.maxSupply = Convert.parseLong(attachmentData.get("maxSupply"));
            this.issuanceHeight = ((Long)attachmentData.get("issuanceHeight")).intValue();
            this.minReservePerUnitATM = attachmentData.containsKey("minReservePerUnitATM") ? Convert.parseLong(attachmentData.get("minReservePerUnitATM")) : Convert.parseLong(attachmentData.get("minReservePerUnitNQT"));
            this.minDifficulty = ((Long)attachmentData.get("minDifficulty")).intValue();
            this.maxDifficulty = ((Long)attachmentData.get("maxDifficulty")).intValue();
            this.ruleset = ((Long)attachmentData.get("ruleset")).byteValue();
            this.algorithm = ((Long)attachmentData.get("algorithm")).byteValue();
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long initialSupply, long reserveSupply,
                                              long maxSupply, int issuanceHeight, long minReservePerUnitATM, int minDifficulty, int maxDifficulty,
                                              byte ruleset, byte algorithm, byte decimals) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.initialSupply = initialSupply;
            this.reserveSupply = reserveSupply;
            this.maxSupply = maxSupply;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitATM = minReservePerUnitATM;
            this.minDifficulty = minDifficulty;
            this.maxDifficulty = maxDifficulty;
            this.ruleset = ruleset;
            this.algorithm = algorithm;
            this.decimals = decimals;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 1 + Convert.toBytes(code).length + 2 +
                    Convert.toBytes(description).length + 1 + 8 + 8 + 8 + 4 + 8 + 1 + 1 + 1 + 1 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] code = Convert.toBytes(this.code);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.put((byte)code.length);
            buffer.put(code);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put(type);
            buffer.putLong(initialSupply);
            buffer.putLong(reserveSupply);
            buffer.putLong(maxSupply);
            buffer.putInt(issuanceHeight);
            buffer.putLong(minReservePerUnitATM);
            buffer.put((byte)minDifficulty);
            buffer.put((byte)maxDifficulty);
            buffer.put(ruleset);
            buffer.put(algorithm);
            buffer.put(decimals);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("initialSupply", initialSupply);
            attachment.put("reserveSupply", reserveSupply);
            attachment.put("maxSupply", maxSupply);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitATM", minReservePerUnitATM);
            attachment.put("minDifficulty", minDifficulty);
            attachment.put("maxDifficulty", maxDifficulty);
            attachment.put("ruleset", ruleset);
            attachment.put("algorithm", algorithm);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public byte getType() {
            return type;
        }

        public long getInitialSupply() {
            return initialSupply;
        }

        public long getReserveSupply() {
            return reserveSupply;
        }

        public long getMaxSupply() {
            return maxSupply;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitATM() {
            return minReservePerUnitATM;
        }

        public int getMinDifficulty() {
            return minDifficulty;
        }

        public int getMaxDifficulty() {
            return maxDifficulty;
        }

        public byte getRuleset() {
            return ruleset;
        }

        public byte getAlgorithm() {
            return algorithm;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    final class MonetarySystemReserveIncrease extends AbstractAttachment implements MonetarySystemAttachment {

        final long currencyId;
        final long amountPerUnitATM;

        public MonetarySystemReserveIncrease(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.amountPerUnitATM = buffer.getLong();
        }

        public MonetarySystemReserveIncrease(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.amountPerUnitATM = attachmentData.containsKey("amountPerUnitATM") ? Convert.parseLong(attachmentData.get("amountPerUnitATM")) : Convert.parseLong(attachmentData.get("amountPerUnitNQT"));
        }

        public MonetarySystemReserveIncrease(long currencyId, long amountPerUnitATM) {
            this.currencyId = currencyId;
            this.amountPerUnitATM = amountPerUnitATM;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(amountPerUnitATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("amountPerUnitATM", amountPerUnitATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_INCREASE;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getAmountPerUnitATM() {
            return amountPerUnitATM;
        }

    }

    final class MonetarySystemReserveClaim extends AbstractAttachment implements MonetarySystemAttachment {

        final long currencyId;
        final long units;

        public MonetarySystemReserveClaim(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        public MonetarySystemReserveClaim(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemReserveClaim(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_CLAIM;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemCurrencyTransfer extends AbstractAttachment implements MonetarySystemAttachment {

        final long currencyId;
        final long units;

        public MonetarySystemCurrencyTransfer(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        public MonetarySystemCurrencyTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemCurrencyTransfer(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_TRANSFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }
    }

    final class MonetarySystemPublishExchangeOffer extends AbstractAttachment implements MonetarySystemAttachment {

        final long currencyId;
        final long buyRateATM;
        final long sellRateATM;
        final long totalBuyLimit;
        final long totalSellLimit;
        final long initialBuySupply;
        final long initialSellSupply;
        final int expirationHeight;

        public MonetarySystemPublishExchangeOffer(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.buyRateATM = buffer.getLong();
            this.sellRateATM = buffer.getLong();
            this.totalBuyLimit = buffer.getLong();
            this.totalSellLimit = buffer.getLong();
            this.initialBuySupply = buffer.getLong();
            this.initialSellSupply = buffer.getLong();
            this.expirationHeight = buffer.getInt();
        }

        public MonetarySystemPublishExchangeOffer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.buyRateATM = attachmentData.containsKey("buyRateATM") ? Convert.parseLong(attachmentData.get("buyRateATM")) : Convert.parseLong(attachmentData.get("buyRateNQT"));
            this.sellRateATM = attachmentData.containsKey("sellRateATM") ? Convert.parseLong(attachmentData.get("sellRateATM")) : Convert.parseLong(attachmentData.get("sellRateNQT"));
            this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
            this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
            this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
            this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
            this.expirationHeight = ((Long)attachmentData.get("expirationHeight")).intValue();
        }

        public MonetarySystemPublishExchangeOffer(long currencyId, long buyRateATM, long sellRateATM, long totalBuyLimit,
                                                  long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyRateATM = buyRateATM;
            this.sellRateATM = sellRateATM;
            this.totalBuyLimit = totalBuyLimit;
            this.totalSellLimit = totalSellLimit;
            this.initialBuySupply = initialBuySupply;
            this.initialSellSupply = initialSellSupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(buyRateATM);
            buffer.putLong(sellRateATM);
            buffer.putLong(totalBuyLimit);
            buffer.putLong(totalSellLimit);
            buffer.putLong(initialBuySupply);
            buffer.putLong(initialSellSupply);
            buffer.putInt(expirationHeight);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("buyRateATM", buyRateATM);
            attachment.put("sellRateATM", sellRateATM);
            attachment.put("totalBuyLimit", totalBuyLimit);
            attachment.put("totalSellLimit", totalSellLimit);
            attachment.put("initialBuySupply", initialBuySupply);
            attachment.put("initialSellSupply", initialSellSupply);
            attachment.put("expirationHeight", expirationHeight);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getBuyRateATM() {
            return buyRateATM;
        }

        public long getSellRateATM() {
            return sellRateATM;
        }

        public long getTotalBuyLimit() {
            return totalBuyLimit;
        }

        public long getTotalSellLimit() {
            return totalSellLimit;
        }

        public long getInitialBuySupply() {
            return initialBuySupply;
        }

        public long getInitialSellSupply() {
            return initialSellSupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    abstract class MonetarySystemExchange extends AbstractAttachment implements MonetarySystemAttachment {

        final long currencyId;
        final long rateATM;
        final long units;

        public MonetarySystemExchange(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.rateATM = buffer.getLong();
            this.units = buffer.getLong();
        }

        public MonetarySystemExchange(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.rateATM = attachmentData.containsKey("rateATM") ? Convert.parseLong(attachmentData.get("rateATM")) : Convert.parseLong(attachmentData.get("rateNQT"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemExchange(long currencyId, long rateATM, long units) {
            this.currencyId = currencyId;
            this.rateATM = rateATM;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(rateATM);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("rateATM", rateATM);
            attachment.put("units", units);
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getRateATM() {
            return rateATM;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemExchangeBuy extends MonetarySystemExchange {

        public MonetarySystemExchangeBuy(ByteBuffer buffer) {
            super(buffer);
        }

        public MonetarySystemExchangeBuy(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeBuy(long currencyId, long rateATM, long units) {
            super(currencyId, rateATM, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_BUY;
        }

    }

    final class MonetarySystemExchangeSell extends MonetarySystemExchange {

        public MonetarySystemExchangeSell(ByteBuffer buffer) {
            super(buffer);
        }

        public MonetarySystemExchangeSell(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeSell(long currencyId, long rateATM, long units) {
            super(currencyId, rateATM, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_SELL;
        }

    }

    final class MonetarySystemCurrencyMinting extends AbstractAttachment implements MonetarySystemAttachment {

        final long nonce;
        final long currencyId;
        final long units;
        final long counter;

        public MonetarySystemCurrencyMinting(ByteBuffer buffer) {
            super(buffer);
            this.nonce = buffer.getLong();
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
            this.counter = buffer.getLong();
        }

        public MonetarySystemCurrencyMinting(JSONObject attachmentData) {
            super(attachmentData);
            this.nonce = Convert.parseLong(attachmentData.get("nonce"));
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
            this.counter = Convert.parseLong(attachmentData.get("counter"));
        }

        public MonetarySystemCurrencyMinting(long nonce, long currencyId, long units, long counter) {
            this.nonce = nonce;
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(nonce);
            buffer.putLong(currencyId);
            buffer.putLong(units);
            buffer.putLong(counter);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("nonce", nonce);
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
            attachment.put("counter", counter);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_MINTING;
        }

        public long getNonce() {
            return nonce;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getCounter() {
            return counter;
        }

    }

    final class MonetarySystemCurrencyDeletion extends AbstractAttachment implements MonetarySystemAttachment {

        final long currencyId;

        public MonetarySystemCurrencyDeletion(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
        }

        public MonetarySystemCurrencyDeletion(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
        }

        public MonetarySystemCurrencyDeletion(long currencyId) {
            this.currencyId = currencyId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_DELETION;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }
    }

    final class ShufflingCreation extends AbstractAttachment {

        final long holdingId;
        final HoldingType holdingType;
        final long amount;
        final byte participantCount;
        final short registrationPeriod;

        public ShufflingCreation(ByteBuffer buffer) {
            super(buffer);
            this.holdingId = buffer.getLong();
            this.holdingType = HoldingType.get(buffer.get());
            this.amount = buffer.getLong();
            this.participantCount = buffer.get();
            this.registrationPeriod = buffer.getShort();
        }

        public ShufflingCreation(JSONObject attachmentData) {
            super(attachmentData);
            this.holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
            this.holdingType = HoldingType.get(((Long)attachmentData.get("holdingType")).byteValue());
            this.amount = Convert.parseLong(attachmentData.get("amount"));
            this.participantCount = ((Long)attachmentData.get("participantCount")).byteValue();
            this.registrationPeriod = ((Long)attachmentData.get("registrationPeriod")).shortValue();
        }

        public ShufflingCreation(long holdingId, HoldingType holdingType, long amount, byte participantCount, short registrationPeriod) {
            this.holdingId = holdingId;
            this.holdingType = holdingType;
            this.amount = amount;
            this.participantCount = participantCount;
            this.registrationPeriod = registrationPeriod;
        }

        @Override
        int getMySize() {
            return 8 + 1 + 8 + 1 + 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(holdingId);
            buffer.put(holdingType.getCode());
            buffer.putLong(amount);
            buffer.put(participantCount);
            buffer.putShort(registrationPeriod);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("holding", Long.toUnsignedString(holdingId));
            attachment.put("holdingType", holdingType.getCode());
            attachment.put("amount", amount);
            attachment.put("participantCount", participantCount);
            attachment.put("registrationPeriod", registrationPeriod);
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_CREATION;
        }

        public long getHoldingId() {
            return holdingId;
        }

        public HoldingType getHoldingType() {
            return holdingType;
        }

        public long getAmount() {
            return amount;
        }

        public byte getParticipantCount() {
            return participantCount;
        }

        public short getRegistrationPeriod() {
            return registrationPeriod;
        }
    }

    interface ShufflingAttachment extends Attachment {

        long getShufflingId();

        byte[] getShufflingStateHash();

    }

    abstract class AbstractShufflingAttachment extends AbstractAttachment implements ShufflingAttachment {

        final long shufflingId;
        final byte[] shufflingStateHash;

        public AbstractShufflingAttachment(ByteBuffer buffer) {
            super(buffer);
            this.shufflingId = buffer.getLong();
            this.shufflingStateHash = new byte[32];
            buffer.get(this.shufflingStateHash);
        }

        public AbstractShufflingAttachment(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingId = Convert.parseUnsignedLong((String) attachmentData.get("shuffling"));
            this.shufflingStateHash = Convert.parseHexString((String) attachmentData.get("shufflingStateHash"));
        }

        public AbstractShufflingAttachment(long shufflingId, byte[] shufflingStateHash) {
            this.shufflingId = shufflingId;
            this.shufflingStateHash = shufflingStateHash;
        }

        @Override
        int getMySize() {
            return 8 + 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(shufflingId);
            buffer.put(shufflingStateHash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shuffling", Long.toUnsignedString(shufflingId));
            attachment.put("shufflingStateHash", Convert.toHexString(shufflingStateHash));
        }

        @Override
        public final long getShufflingId() {
            return shufflingId;
        }

        @Override
        public final byte[] getShufflingStateHash() {
            return shufflingStateHash;
        }

    }

    final class ShufflingRegistration extends AbstractAttachment implements ShufflingAttachment {

        final byte[] shufflingFullHash;

        public ShufflingRegistration(ByteBuffer buffer) {
            super(buffer);
            this.shufflingFullHash = new byte[32];
            buffer.get(this.shufflingFullHash);
        }

        public ShufflingRegistration(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingFullHash = Convert.parseHexString((String) attachmentData.get("shufflingFullHash"));
        }

        public ShufflingRegistration(byte[] shufflingFullHash) {
            this.shufflingFullHash = shufflingFullHash;
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_REGISTRATION;
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put(shufflingFullHash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shufflingFullHash", Convert.toHexString(shufflingFullHash));
        }

        @Override
        public long getShufflingId() {
            return Convert.fullHashToId(shufflingFullHash);
        }

        @Override
        public byte[] getShufflingStateHash() {
            return shufflingFullHash;
        }

    }

    final class ShufflingProcessing extends AbstractShufflingAttachment implements Prunable {

        private static final byte[] emptyDataHash = Crypto.sha256().digest();

        public static ShufflingProcessing parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(ShufflingTransaction.SHUFFLING_PROCESSING.getName(), attachmentData)) {
                return null;
            }
            return new ShufflingProcessing(attachmentData);
        }

        volatile byte[][] data;
        final byte[] hash;

        public ShufflingProcessing(ByteBuffer buffer) {
            super(buffer);
            this.hash = new byte[32];
            buffer.get(hash);
            this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
        }

        public ShufflingProcessing(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray)attachmentData.get("data");
            if (jsonArray != null) {
                this.data = new byte[jsonArray.size()][];
                for (int i = 0; i < this.data.length; i++) {
                    this.data[i] = Convert.parseHexString((String) jsonArray.get(i));
                }
                this.hash = null;
            } else {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String)attachmentData.get("hash")));
                this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
            }
        }

        public ShufflingProcessing(long shufflingId, byte[][] data, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
            this.data = data;
            this.hash = null;
        }

        @Override
        int getMyFullSize() {
            int size = super.getMySize();
            if (data != null) {
                size += 1;
                for (byte[] bytes : data) {
                    size += 4;
                    size += bytes.length;
                }
            }
            return size / 2; // just lie
        }

        @Override
        int getMySize() {
            return super.getMySize() + 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put(getHash());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            if (data != null) {
                JSONArray jsonArray = new JSONArray();
                attachment.put("data", jsonArray);
                for (byte[] bytes : data) {
                    jsonArray.add(Convert.toHexString(bytes));
                }
            }
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_PROCESSING;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            } else if (data != null) {
                MessageDigest digest = Crypto.sha256();
                for (byte[] bytes : data) {
                    digest.update(bytes);
                }
                return digest.digest();
            } else {
                throw new IllegalStateException("Both hash and data are null");
            }
        }

        public byte[][] getData() {
            return data;
        }

        @Override
        public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                data = ShufflingParticipant.getData(getShufflingId(), transaction.getSenderId());
            }
        }

        @Override
        public boolean hasPrunableData() {
            return data != null;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            ShufflingParticipant.restoreData(getShufflingId(), transaction.getSenderId(), getData(), transaction.getTimestamp(), height);
        }

    }

    final class ShufflingRecipients extends AbstractShufflingAttachment {

        final byte[][] recipientPublicKeys;

        public ShufflingRecipients(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            int count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count < 0) {
                throw new AplException.NotValidException("Invalid data count " + count);
            }
            this.recipientPublicKeys = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.recipientPublicKeys[i] = new byte[32];
                buffer.get(this.recipientPublicKeys[i]);
            }
        }

        public ShufflingRecipients(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray)attachmentData.get("recipientPublicKeys");
            this.recipientPublicKeys = new byte[jsonArray.size()][];
            for (int i = 0; i < this.recipientPublicKeys.length; i++) {
                this.recipientPublicKeys[i] = Convert.parseHexString((String)jsonArray.get(i));
            }
        }

        public ShufflingRecipients(long shufflingId, byte[][] recipientPublicKeys, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
            this.recipientPublicKeys = recipientPublicKeys;
        }

        @Override
        int getMySize() {
            int size = super.getMySize();
            size += 1;
            size += 32 * recipientPublicKeys.length;
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put((byte)recipientPublicKeys.length);
            for (byte[] bytes : recipientPublicKeys) {
                buffer.put(bytes);
            }
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            JSONArray jsonArray = new JSONArray();
            attachment.put("recipientPublicKeys", jsonArray);
            for (byte[] bytes : recipientPublicKeys) {
                jsonArray.add(Convert.toHexString(bytes));
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_RECIPIENTS;
        }

        public byte[][] getRecipientPublicKeys() {
            return recipientPublicKeys;
        }

    }

    final class ShufflingVerification extends AbstractShufflingAttachment {

        public ShufflingVerification(ByteBuffer buffer) {
            super(buffer);
        }

        public ShufflingVerification(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ShufflingVerification(long shufflingId, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_VERIFICATION;
        }

    }

    final class ShufflingCancellation extends AbstractShufflingAttachment {
        private final BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        final byte[][] blameData;
        final byte[][] keySeeds;
        final long cancellingAccountId;

        public ShufflingCancellation(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            int count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
                throw new AplException.NotValidException("Invalid data count " + count);
            }
            this.blameData = new byte[count][];
            for (int i = 0; i < count; i++) {
                int size = buffer.getInt();
                if (size > blockchainConfig.getCurrentConfig().getMaxPayloadLength()) {
                    throw new AplException.NotValidException("Invalid data size " + size);
                }
                this.blameData[i] = new byte[size];
                buffer.get(this.blameData[i]);
            }
            count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
                throw new AplException.NotValidException("Invalid keySeeds count " + count);
            }
            this.keySeeds = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.keySeeds[i] = new byte[32];
                buffer.get(this.keySeeds[i]);
            }
            this.cancellingAccountId = buffer.getLong();
        }

        public ShufflingCancellation(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray)attachmentData.get("blameData");
            this.blameData = new byte[jsonArray.size()][];
            for (int i = 0; i < this.blameData.length; i++) {
                this.blameData[i] = Convert.parseHexString((String)jsonArray.get(i));
            }
            jsonArray = (JSONArray)attachmentData.get("keySeeds");
            this.keySeeds = new byte[jsonArray.size()][];
            for (int i = 0; i < this.keySeeds.length; i++) {
                this.keySeeds[i] = Convert.parseHexString((String)jsonArray.get(i));
            }
            this.cancellingAccountId = Convert.parseUnsignedLong((String) attachmentData.get("cancellingAccount"));
        }

        public ShufflingCancellation(long shufflingId, byte[][] blameData, byte[][] keySeeds, byte[] shufflingStateHash, long cancellingAccountId) {
            super(shufflingId, shufflingStateHash);
            this.blameData = blameData;
            this.keySeeds = keySeeds;
            this.cancellingAccountId = cancellingAccountId;
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_CANCELLATION;
        }

        @Override
        int getMySize() {
            int size = super.getMySize();
            size += 1;
            for (byte[] bytes : blameData) {
                size += 4;
                size += bytes.length;
            }
            size += 1;
            size += 32 * keySeeds.length;
            size += 8;
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put((byte) blameData.length);
            for (byte[] bytes : blameData) {
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            }
            buffer.put((byte) keySeeds.length);
            for (byte[] bytes : keySeeds) {
                buffer.put(bytes);
            }
            buffer.putLong(cancellingAccountId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            JSONArray jsonArray = new JSONArray();
            attachment.put("blameData", jsonArray);
            for (byte[] bytes : blameData) {
                jsonArray.add(Convert.toHexString(bytes));
            }
            jsonArray = new JSONArray();
            attachment.put("keySeeds", jsonArray);
            for (byte[] bytes : keySeeds) {
                jsonArray.add(Convert.toHexString(bytes));
            }
            if (cancellingAccountId != 0) {
                attachment.put("cancellingAccount", Long.toUnsignedString(cancellingAccountId));
            }
        }

        public byte[][] getBlameData() {
            return blameData;
        }

        public byte[][] getKeySeeds() {
            return keySeeds;
        }

        public long getCancellingAccountId() {
            return cancellingAccountId;
        }

        public byte[] getHash() {
            MessageDigest digest = Crypto.sha256();
            for (byte[] bytes : blameData) {
                digest.update(bytes);
            }
            return digest.digest();
        }

    }

    abstract class TaggedDataAttachment extends AbstractAttachment implements Prunable {

        final String name;
        final String description;
        final String tags;
        final String type;
        final String channel;
        final boolean isText;
        final String filename;
        final byte[] data;
        private volatile TaggedData taggedData;

        public TaggedDataAttachment(ByteBuffer buffer) {
            super(buffer);
            this.name = null;
            this.description = null;
            this.tags = null;
            this.type = null;
            this.channel = null;
            this.isText = false;
            this.filename = null;
            this.data = null;
        }

        public TaggedDataAttachment(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON != null) {
                this.name = (String) attachmentData.get("name");
                this.description = (String) attachmentData.get("description");
                this.tags = (String) attachmentData.get("tags");
                this.type = (String) attachmentData.get("type");
                this.channel = Convert.nullToEmpty((String) attachmentData.get("channel"));
                this.isText = Boolean.TRUE.equals(attachmentData.get("isText"));
                this.data = isText ? Convert.toBytes(dataJSON) : Convert.parseHexString(dataJSON);
                this.filename = (String) attachmentData.get("filename");
            } else {
                this.name = null;
                this.description = null;
                this.tags = null;
                this.type = null;
                this.channel = null;
                this.isText = false;
                this.filename = null;
                this.data = null;
            }

        }

        public TaggedDataAttachment(String name, String description, String tags, String type, String channel, boolean isText, String filename, byte[] data) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.type = type;
            this.channel = channel;
            this.isText = isText;
            this.data = data;
            this.filename = filename;
        }

        @Override
        final int getMyFullSize() {
            if (getData() == null) {
                return 0;
            }
            return Convert.toBytes(getName()).length + Convert.toBytes(getDescription()).length + Convert.toBytes(getType()).length
                    + Convert.toBytes(getChannel()).length + Convert.toBytes(getTags()).length + Convert.toBytes(getFilename()).length + getData().length;
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            if (taggedData != null) {
                attachment.put("name", taggedData.getName());
                attachment.put("description", taggedData.getDescription());
                attachment.put("tags", taggedData.getTags());
                attachment.put("type", taggedData.getType());
                attachment.put("channel", taggedData.getChannel());
                attachment.put("isText", taggedData.isText());
                attachment.put("filename", taggedData.getFilename());
                attachment.put("data", taggedData.isText() ? Convert.toString(taggedData.getData()) : Convert.toHexString(taggedData.getData()));
            } else if (data != null) {
                attachment.put("name", name);
                attachment.put("description", description);
                attachment.put("tags", tags);
                attachment.put("type", type);
                attachment.put("channel", channel);
                attachment.put("isText", isText);
                attachment.put("filename", filename);
                attachment.put("data", isText ? Convert.toString(data) : Convert.toHexString(data));
            }
        }

        @Override
        public byte[] getHash() {
            if (data == null) {
                return null;
            }
            MessageDigest digest = Crypto.sha256();
            digest.update(Convert.toBytes(name));
            digest.update(Convert.toBytes(description));
            digest.update(Convert.toBytes(tags));
            digest.update(Convert.toBytes(type));
            digest.update(Convert.toBytes(channel));
            digest.update((byte)(isText ? 1 : 0));
            digest.update(Convert.toBytes(filename));
            digest.update(data);
            return digest.digest();
        }

        public final String getName() {
            if (taggedData != null) {
                return taggedData.getName();
            }
            return name;
        }

        public final String getDescription() {
            if (taggedData != null) {
                return taggedData.getDescription();
            }
            return description;
        }

        public final String getTags() {
            if (taggedData != null) {
                return taggedData.getTags();
            }
            return tags;
        }

        public final String getType() {
            if (taggedData != null) {
                return taggedData.getType();
            }
            return type;
        }

        public final String getChannel() {
            if (taggedData != null) {
                return taggedData.getChannel();
            }
            return channel;
        }

        public final boolean isText() {
            if (taggedData != null) {
                return taggedData.isText();
            }
            return isText;
        }

        public final String getFilename() {
            if (taggedData != null) {
                return taggedData.getFilename();
            }
            return filename;
        }

        public final byte[] getData() {
            if (taggedData != null) {
                return taggedData.getData();
            }
            return data;
        }

        @Override
        public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && taggedData == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                taggedData = TaggedData.getData(getTaggedDataId(transaction));
            }
        }

        @Override
        public boolean hasPrunableData() {
            return (taggedData != null || data != null);
        }

        abstract long getTaggedDataId(Transaction transaction);

    }

    final class TaggedDataUpload extends TaggedDataAttachment {

        public static TaggedDataUpload parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(Data.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataUpload(attachmentData);
        }

        final byte[] hash;

        public TaggedDataUpload(ByteBuffer buffer) {
            super(buffer);
            this.hash = new byte[32];
            buffer.get(hash);
        }

        public TaggedDataUpload(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON == null) {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String)attachmentData.get("hash")));
            } else {
                this.hash = null;
            }
        }

        public TaggedDataUpload(String name, String description, String tags, String type, String channel, boolean isText,
                                String filename, byte[] data) throws AplException.NotValidException {
            super(name, description, tags, type, channel, isText, filename, data);
            this.hash = null;
            if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
                throw new AplException.NotValidException("Data is not UTF-8 text");
            }
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put(getHash());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return Data.TAGGED_DATA_UPLOAD;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            }
            return super.getHash();
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return transaction.getId();
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            TaggedData.restore(transaction, this, blockTimestamp, height);
        }

    }

    final class TaggedDataExtend extends TaggedDataAttachment {
        private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

        public static TaggedDataExtend parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataExtend(attachmentData);
        }

        private volatile byte[] hash;
        final long taggedDataId;
        final boolean jsonIsPruned;

        public TaggedDataExtend(ByteBuffer buffer) {
            super(buffer);
            this.taggedDataId = buffer.getLong();
            this.jsonIsPruned = false;
        }

        public TaggedDataExtend(JSONObject attachmentData) {
            super(attachmentData);
            this.taggedDataId = Convert.parseUnsignedLong((String)attachmentData.get("taggedData"));
            this.jsonIsPruned = attachmentData.get("data") == null;
        }

        public TaggedDataExtend(TaggedData taggedData) {
            super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(),
                    taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
            this.taggedDataId = taggedData.getId();
            this.jsonIsPruned = false;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(taggedDataId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("taggedData", Long.toUnsignedString(taggedDataId));
        }

        @Override
        public TransactionType getTransactionType() {
            return Data.TAGGED_DATA_EXTEND;
        }

        public long getTaggedDataId() {
            return taggedDataId;
        }

        @Override
        public byte[] getHash() {
            if (hash == null) {
                hash = super.getHash();
            }
            if (hash == null) {
                TaggedDataUpload taggedDataUpload = (TaggedDataUpload) blockchain.getTransaction(taggedDataId).getAttachment();
                hash = taggedDataUpload.getHash();
            }
            return hash;
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return taggedDataId;
        }

        public boolean jsonIsPruned() {
            return jsonIsPruned;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        }

    }

    final class SetPhasingOnly extends AbstractAttachment {

        private final PhasingParams phasingParams;
        final long maxFees;
        final short minDuration;
        final short maxDuration;

        public SetPhasingOnly(PhasingParams params, long maxFees, short minDuration, short maxDuration) {
            phasingParams = params;
            this.maxFees = maxFees;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        public SetPhasingOnly(ByteBuffer buffer) {
            super(buffer);
            phasingParams = new PhasingParams(buffer);
            maxFees = buffer.getLong();
            minDuration = buffer.getShort();
            maxDuration = buffer.getShort();
        }

        public SetPhasingOnly(JSONObject attachmentData) {
            super(attachmentData);
            JSONObject phasingControlParams = (JSONObject) attachmentData.get("phasingControlParams");
            phasingParams = new PhasingParams(phasingControlParams);
            maxFees = Convert.parseLong(attachmentData.get("controlMaxFees"));
            minDuration = ((Long)attachmentData.get("controlMinDuration")).shortValue();
            maxDuration = ((Long)attachmentData.get("controlMaxDuration")).shortValue();
        }

        @Override
        public TransactionType getTransactionType() {
            return AccountControl.SET_PHASING_ONLY;
        }

        @Override
        int getMySize() {
            return phasingParams.getMySize() + 8 + 2 + 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            phasingParams.putMyBytes(buffer);
            buffer.putLong(maxFees);
            buffer.putShort(minDuration);
            buffer.putShort(maxDuration);
        }

        @Override
        void putMyJSON(JSONObject json) {
            JSONObject phasingControlParams = new JSONObject();
            phasingParams.putMyJSON(phasingControlParams);
            json.put("phasingControlParams", phasingControlParams);
            json.put("controlMaxFees", maxFees);
            json.put("controlMinDuration", minDuration);
            json.put("controlMaxDuration", maxDuration);
        }

        public PhasingParams getPhasingParams() {
            return phasingParams;
        }

        public long getMaxFees() {
            return maxFees;
        }

        public short getMinDuration() {
            return minDuration;
        }

        public short getMaxDuration() {
            return maxDuration;
        }

    }

    abstract class UpdateAttachment extends AbstractAttachment {

        final Platform platform;
        final Architecture architecture;
        final DoubleByteArrayTuple url;
        final Version version;
        final byte[] hash;

        public UpdateAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            try {
                platform = Platform.valueOf(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_PLATFORM_LENGTH).trim());
                architecture = Architecture.valueOf(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_ARCHITECTURE_LENGTH).trim());
                int firstUrlPartLength = buffer.getShort();
                byte[] firstUrlPart = new byte[firstUrlPartLength];
                buffer.get(firstUrlPart);
                int secondUrlPartLength = buffer.getShort();
                byte[] secondUrlPart = new byte[secondUrlPartLength];
                buffer.get(secondUrlPart);
                url = new DoubleByteArrayTuple(firstUrlPart, secondUrlPart);
                version = new Version(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_VERSION_LENGTH).trim());
                int hashLength = buffer.getShort();
                hash = new byte[hashLength];
                buffer.get(hash);
            } catch (NotValidException ex) {
                throw new AplException.NotValidException(ex.getMessage());
            }
        }

        public UpdateAttachment(JSONObject attachmentData) {
            super(attachmentData);
            platform = Platform.valueOf(Convert.nullToEmpty((String) attachmentData.get("platform")).trim());
            architecture = Architecture.valueOf(Convert.nullToEmpty((String) attachmentData.get("architecture")).trim());
            JSONObject urlJson = (JSONObject) attachmentData.get("url");
            byte[] firstUrlPart = Convert.parseHexString(Convert.nullToEmpty(((String) urlJson.get("first")).trim()));
            byte[] secondUrlPart = Convert.parseHexString(Convert.nullToEmpty(((String) urlJson.get("second")).trim()));
            url = new DoubleByteArrayTuple(firstUrlPart, secondUrlPart);
            version = new Version(Convert.nullToEmpty((String) attachmentData.get("version")).trim());
            hash = Convert.parseHexString(Convert.nullToEmpty((String) attachmentData.get("hash")).trim());
        }

        public UpdateAttachment(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            this.platform = platform;
            this.architecture = architecture;
            this.url = url;
            this.version = version;
            this.hash = hash;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(platform.name()).length + 1 + Convert.toBytes(architecture.name()).length
                    + 2 + url.getFirst().length + 2 + url.getSecond().length + 1 + Convert.toBytes(version.toString()).length + 2+ hash.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] platform = Convert.toBytes(this.platform.toString());
            byte[] architecture = Convert.toBytes(this.architecture.toString());
            byte[] version = Convert.toBytes(this.version.toString());
            buffer.put((byte) platform.length);
            buffer.put(platform);
            buffer.put((byte) architecture.length);
            buffer.put(architecture);
            buffer.putShort((short) url.getFirst().length);
            buffer.put(url.getFirst());
            buffer.putShort((short) url.getSecond().length);
            buffer.put(url.getSecond());
            buffer.put((byte) version.length);
            buffer.put(version);
            buffer.putShort((short) hash.length);
            buffer.put(hash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("platform", platform.toString());
            attachment.put("architecture", architecture.toString());
            JSONObject urlJson = new JSONObject();
            urlJson.put("first", Convert.toHexString(url.getFirst()));
            urlJson.put("second", Convert.toHexString(url.getSecond()));
            attachment.put("url", urlJson);
            attachment.put("version", version.toString());
            attachment.put("hash", Convert.toHexString(hash));
        }

        public Platform getPlatform() {
            return platform;
        }

        public Architecture getArchitecture() {
            return architecture;
        }

        public DoubleByteArrayTuple getUrl() {
            return url;
        }

        public Version getAppVersion() {
            return version;
        }

        public byte[] getHash() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UpdateAttachment)) return false;
            UpdateAttachment that = (UpdateAttachment) o;
            return platform == that.platform &&
                    architecture == that.architecture &&
                    Objects.equals(url, that.url) &&
                    Objects.equals(version, that.version) &&
                    Arrays.equals(hash, that.hash);
        }

        @Override
        public int hashCode() {

            int result = Objects.hash(platform, architecture, url, version);
            result = 31 * result + Arrays.hashCode(hash);
            return result;
        }

        public static Attachment.UpdateAttachment getAttachment(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[]
                hash, byte level) {
            if (level == Update.CRITICAL.getSubtype()) {
                return new Attachment.CriticalUpdate(platform, architecture, url, version, hash);
            } else if (level == Update.IMPORTANT.getSubtype()) {
                return new Attachment.ImportantUpdate(platform, architecture, url, version, hash);
            } else if (level == Update.MINOR.getSubtype()) {
                return new Attachment.MinorUpdate(platform, architecture, url, version, hash);
            }
            return null;
        }
    }

    final class CriticalUpdate extends UpdateAttachment {
        public CriticalUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        public CriticalUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public CriticalUpdate(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public TransactionType getTransactionType() {
            return Update.CRITICAL;
        }
    }

    final class ImportantUpdate extends UpdateAttachment {
        public ImportantUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        public ImportantUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ImportantUpdate(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public TransactionType getTransactionType() {
            return Update.IMPORTANT;
        }
    }

    final class MinorUpdate extends UpdateAttachment {
        public MinorUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        public MinorUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MinorUpdate(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public TransactionType getTransactionType() {
            return Update.MINOR;
        }
    }

}
