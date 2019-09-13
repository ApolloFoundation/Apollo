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
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.get.GetMintingTarget;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Generate new currency units
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id of the minted currency</li>
 * <li>nonce - a unique nonce provided by the miner</li>
 * <li>units - number of units minted per this transaction</li>
 * <li>counter - a sequential number counting the ordinal mint operation number per currency/account combination<br>
 * this ever increasing value ensures that the same mint transaction cannot execute more than once</li>
 * </ul>
 *
 * Each minting request triggers a hash calculation based on the submitted data and the currency hash algorithm<br>
 * The resulting hash code is compared to the target value derived from the current difficulty.<br>
 * If the hash code is smaller than the target the currency units are generated into the sender account.<br>
 * It is recommended to calculate the hash value offline before submitting the transaction.<br>
 * Use the {@link GetMintingTarget} transaction to retrieve the current hash target and then calculate the hash offline
 * by following the procedure used in {@link com.apollocurrency.aplwallet.apl.CurrencyMint#mintCurrency}<br>
 */
@Vetoed
public final class CurrencyMint extends CreateTransaction {

    public CurrencyMint() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "nonce", "units", "counter");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Currency currency = ParameterParser.getCurrency(req);
        long nonce = ParameterParser.getLong(req, "nonce", Long.MIN_VALUE, Long.MAX_VALUE, true);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        long counter = ParameterParser.getLong(req, "counter", 0, Integer.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new MonetarySystemCurrencyMinting(nonce, currency.getId(), units, counter);
        return createTransaction(req, account, attachment);
    }

}
