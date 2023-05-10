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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyFounders;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncreaseAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Increase the value of currency units by paying APL
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>amountPerUnitATM - the ATM amount invested into increasing the value of a single currency unit.<br>
 * This value is multiplied by the currency total supply and the result is deducted from the sender's account balance.
 * </ul>
 * <p>
 * Constraints
 * <p>
 * This API is allowed only when the currency is {@link CurrencyType#RESERVABLE} and is not yet active.
 * <p>
 * The sender account is registered as a founder. Once the currency becomes active
 * the total supply is distributed between the founders based on their proportional investment<br>
 * The list of founders and their ATM investment can be obtained using the {@link GetCurrencyFounders} API.
 */
@Vetoed
public final class CurrencyReserveIncrease extends CreateTransactionHandler {

    public CurrencyReserveIncrease() {
        super(new APITag[]{APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "amountPerUnitATM");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Currency currency = HttpParameterParserUtil.getCurrency(req);
        long amountPerUnitATM = HttpParameterParserUtil.getLong(req, "amountPerUnitATM", 1L,
                CDI.current().select(BlockchainConfig.class).get().getCurrentConfig().getMaxBalanceATM(), true);
        Account account = HttpParameterParserUtil.getSenderAccount(req);
        Attachment attachment = new MonetarySystemReserveIncreaseAttachment(currency.getId(), amountPerUnitATM);
        return createTransaction(req, account, attachment);

    }

}
