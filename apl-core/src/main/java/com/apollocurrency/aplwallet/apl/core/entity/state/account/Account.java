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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The account entity
 */
@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Account extends VersionedDeletableEntity {
    private static final AddressScope DEFAULT_SCOPE = AddressScope.from((byte) 0);

    private final long id;
    @Setter
    private long parentId;
    @Setter
    private boolean multiSig;
    @Setter
    private AddressScope addrScope;

    @Setter
    private PublicKey publicKey;
    @Setter
    private long balanceATM;
    @Setter
    private long unconfirmedBalanceATM;

    private long forgedBalanceATM;
    @Setter
    private long activeLesseeId;
    @Setter
    private Set<AccountControlType> controls;

    public Account(long id, DbKey dbKey) {
        this(id, DEFAULT_HEIGHT);
        setDbKey(dbKey);
    }

    public Account(long id, int height) {
        super(null, height);
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            log.info("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.controls = Collections.emptySet();
        this.addrScope = DEFAULT_SCOPE;
    }

    public Account(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.multiSig = rs.getBoolean("is_multi_sig");
        this.parentId = rs.getLong("parent");
        if(!rs.wasNull()){
            //TODO: The parent account is NOT NULL.
            // Is it a sufficient condition for setting the multi_sig field to TRUE value?
            this.multiSig = true;
        }
        this.addrScope = AddressScope.from(rs.getByte("addr_scope"));
        this.balanceATM = rs.getLong("balance");
        this.unconfirmedBalanceATM = rs.getLong("unconfirmed_balance");
        this.forgedBalanceATM = rs.getLong("forged_balance");
        this.activeLesseeId = rs.getLong("active_lessee_id");

        if (rs.getBoolean("has_control_phasing")) {
            this.setControls(Collections.unmodifiableSet(EnumSet.of(AccountControlType.PHASING_ONLY)));
        } else {
            this.setControls(Collections.emptySet());
        }
        setDbKey(dbKey);
    }

    public Account(long id, long balanceATM, long unconfirmedBalanceATM, long forgedBalanceATM, long activeLesseeId, int height) {
        this(id, height);
        this.balanceATM = balanceATM;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.activeLesseeId = activeLesseeId;
    }

    /**
     * Returns true if current account is a child account
     * @return true if current account is a child account, it means that {@code parentId != 0}
     */
    public boolean isChild(){
        return !isParent();
    }

    /**
     * Returns true if current account is a parent
     * @return true if current account is a parent, it means that {@code parentId == 0}
     */
    public boolean isParent(){
        return parentId == 0;
    }

    public boolean addToForgedBalanceATM(long amountATM) {
        if (amountATM == 0) {
            return false;
        }
        this.forgedBalanceATM = Math.addExact(this.forgedBalanceATM, amountATM);
        return true;
    }

    public Set<AccountControlType> getControls() {
        return controls;
    }

    /**
     * Add control into account
     *
     * @param control - the control type
     * @return true if a control was added successfully and false otherwise.
     * In case of success, the account needs to be updated in the repository.
     */
    public boolean addControl(AccountControlType control) {
        if (controls.contains(control)) {
            return false;
        }
        EnumSet<AccountControlType> newControls = EnumSet.of(control);
        newControls.addAll(controls);
        controls = Collections.unmodifiableSet(newControls);
        return true;
    }

    /**
     * Remove control from account
     *
     * @param control - the control type
     * @return true if a control was removed successfully and false otherwise.
     * In case of success, the account needs to be updated in the repository.
     */
    public boolean removeControl(AccountControlType control) {
        if (!controls.contains(control)) {
            return false;
        }
        EnumSet<AccountControlType> newControls = EnumSet.copyOf(controls);
        newControls.remove(control);
        controls = Collections.unmodifiableSet(newControls);
        return true;
    }

}
