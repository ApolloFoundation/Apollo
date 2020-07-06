/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class Currency extends VersionedDeletableEntity {

    private long id;
    private long accountId;
    private String name;
    private String nameLower;
    private String code;
    private String description;
    private int type;
    private long initialSupply;
    private long reserveSupply;
    private long maxSupply;
    private int creationHeight;
    private int issuanceHeight;
    private long minReservePerUnitATM;
    private int minDifficulty;
    private int maxDifficulty;
    private byte ruleset;
    private byte algorithm;
    private byte decimals;
    private CurrencySupply currencySupply;

    public Currency(Transaction transaction, MonetarySystemCurrencyIssuance attachment, int height) {
        super(null, height);
        this.id = transaction.getId();
        super.setDbKey(CurrencyTable.currencyDbKeyFactory.newKey(this.id));
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.nameLower = attachment.getName().toLowerCase();
        this.code = attachment.getCode();
        this.description = attachment.getDescription();
        this.type = attachment.getType();
        this.initialSupply = attachment.getInitialSupply();
        this.reserveSupply = attachment.getReserveSupply();
        this.maxSupply = attachment.getMaxSupply();
        this.creationHeight = height;
        this.issuanceHeight = attachment.getIssuanceHeight();
        this.minReservePerUnitATM = attachment.getMinReservePerUnitATM();
        this.minDifficulty = attachment.getMinDifficulty();
        this.maxDifficulty = attachment.getMaxDifficulty();
        this.ruleset = attachment.getRuleset();
        this.algorithm = attachment.getAlgorithm();
        this.decimals = attachment.getDecimals();
    }

    public Currency(long id, long accountId, String name, String nameLower, String code, String description,
                    int type, long initialSupply, long reserveSupply, long maxSupply, int creationHeight,
                    int issuanceHeight, long minReservePerUnitATM, int minDifficulty, int maxDifficulty,
                    byte ruleset, byte algorithm, byte decimals, CurrencySupply currencySupply, int height,
                    boolean latest, boolean deleted) {
        super(null, height);
        this.id = id;
        this.accountId = accountId;
        this.name = name;
        this.name = nameLower;
        this.code = code;
        this.description = description;
        this.type = type;
        this.initialSupply = initialSupply;
        this.reserveSupply = reserveSupply;
        this.maxSupply = maxSupply;
        this.creationHeight = creationHeight;
        this.issuanceHeight = issuanceHeight;
        this.minReservePerUnitATM = minReservePerUnitATM;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
        this.ruleset = ruleset;
        this.algorithm = algorithm;
        this.decimals = decimals;
        this.currencySupply = currencySupply;
        this.setLatest(latest);
        this.setDeleted(deleted);
    }

    public Currency(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        super.setDbKey(dbKey);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.nameLower = rs.getString("name_lower");
        this.code = rs.getString("code");
        this.description = rs.getString("description");
        this.type = rs.getInt("type");
        this.initialSupply = rs.getLong("initial_supply");
        this.reserveSupply = rs.getLong("reserve_supply");
        this.maxSupply = rs.getLong("max_supply");
        this.creationHeight = rs.getInt("creation_height");
        this.issuanceHeight = rs.getInt("issuance_height");
        this.minReservePerUnitATM = rs.getLong("min_reserve_per_unit_atm");
        this.minDifficulty = rs.getByte("min_difficulty") & 0xFF;
        this.maxDifficulty = rs.getByte("max_difficulty") & 0xFF;
        this.ruleset = rs.getByte("ruleset");
        this.algorithm = rs.getByte("algorithm");
        this.decimals = rs.getByte("decimals");
    }

    public boolean is(CurrencyType type) {
        return (this.type & type.getCode()) != 0;
    }

}
