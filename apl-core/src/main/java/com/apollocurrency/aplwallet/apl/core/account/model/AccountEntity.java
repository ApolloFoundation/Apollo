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

package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The account entity
 */
@Slf4j
@Getter
@ToString
@NoArgsConstructor
public final class AccountEntity {

    private long id;
    private DbKey dbKey;

    @Setter
    private PublicKey publicKey;
    @Setter
    private long balanceATM;
    @Setter
    private long unconfirmedBalanceATM;
    @Setter
    private long forgedBalanceATM;
    @Setter
    private long activeLesseeId;
    @Setter
    private Set<Account.ControlType> controls;

    public AccountEntity(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            log.info("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.controls = Collections.emptySet();
    }

    public AccountEntity(long id, DbKey dbKey) {
        this(id);
        this.dbKey = dbKey;
    }

    public AccountEntity(long id, DbKey dbKey, long balanceATM, long unconfirmedBalanceATM, long forgedBalanceATM, long activeLesseeId) {
        this(id, dbKey);

        this.balanceATM = balanceATM;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.activeLesseeId = activeLesseeId;
    }

    public boolean addToForgedBalanceATM(long amountATM) {
        if (amountATM == 0) {
            return false;
        }
        this.forgedBalanceATM = Math.addExact(this.forgedBalanceATM, amountATM);
        return true;
    }

    public Set<Account.ControlType> getControls() {
        return controls;
    }

    /**
     * Add control into account
     * @param control - the control type
     * @return  true if a control was added successfully and false otherwise.
     * In case of success, the account needs to be updated in the repository.
     */
    public boolean addControl(Account.ControlType control) {
        if (controls.contains(control)) {
            return false;
        }
        EnumSet<Account.ControlType> newControls = EnumSet.of(control);
        newControls.addAll(controls);
        controls = Collections.unmodifiableSet(newControls);
        return true;
    }

    /**
     * Remove control from account
     * @param control - the control type
     * @return  true if a control was removed successfully and false otherwise.
     * In case of success, the account needs to be updated in the repository.
     */
    public boolean removeControl(Account.ControlType control) {
        if (!controls.contains(control)) {
            return false;
        }
        EnumSet<Account.ControlType> newControls = EnumSet.copyOf(controls);
        newControls.remove(control);
        controls = Collections.unmodifiableSet(newControls);
        return true;
    }

}
