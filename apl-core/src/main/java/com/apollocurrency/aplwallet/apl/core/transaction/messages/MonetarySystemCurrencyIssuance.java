/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MonetarySystemCurrencyIssuance extends AbstractAttachment {
    
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
        this.name = (String) attachmentData.get("name");
        this.code = (String) attachmentData.get("code");
        this.description = (String) attachmentData.get("description");
        this.type = ((Long) attachmentData.get("type")).byteValue();
        this.initialSupply = Convert.parseLong(attachmentData.get("initialSupply"));
        this.reserveSupply = Convert.parseLong(attachmentData.get("reserveSupply"));
        this.maxSupply = Convert.parseLong(attachmentData.get("maxSupply"));
        this.issuanceHeight = ((Long) attachmentData.get("issuanceHeight")).intValue();
        this.minReservePerUnitATM = attachmentData.containsKey("minReservePerUnitATM") ? Convert.parseLong(attachmentData.get("minReservePerUnitATM")) : Convert.parseLong(attachmentData.get("minReservePerUnitNQT"));
        this.minDifficulty = ((Long) attachmentData.get("minDifficulty")).intValue();
        this.maxDifficulty = ((Long) attachmentData.get("maxDifficulty")).intValue();
        this.ruleset = ((Long) attachmentData.get("ruleset")).byteValue();
        this.algorithm = ((Long) attachmentData.get("algorithm")).byteValue();
        this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
    }

    public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long initialSupply, long reserveSupply, long maxSupply, int issuanceHeight, long minReservePerUnitATM, int minDifficulty, int maxDifficulty, byte ruleset, byte algorithm, byte decimals) {
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
        return 1 + Convert.toBytes(name).length + 1 + Convert.toBytes(code).length + 2 + Convert.toBytes(description).length + 1 + 8 + 8 + 8 + 4 + 8 + 1 + 1 + 1 + 1 + 1;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] name = Convert.toBytes(this.name);
        byte[] code = Convert.toBytes(this.code);
        byte[] description = Convert.toBytes(this.description);
        buffer.put((byte) name.length);
        buffer.put(name);
        buffer.put((byte) code.length);
        buffer.put(code);
        buffer.putShort((short) description.length);
        buffer.put(description);
        buffer.put(type);
        buffer.putLong(initialSupply);
        buffer.putLong(reserveSupply);
        buffer.putLong(maxSupply);
        buffer.putInt(issuanceHeight);
        buffer.putLong(minReservePerUnitATM);
        buffer.put((byte) minDifficulty);
        buffer.put((byte) maxDifficulty);
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
