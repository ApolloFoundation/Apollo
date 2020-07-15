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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountLease;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Deprecated
@Vetoed
public final class GetAccount extends AbstractAPIRequestHandler {

    private AccountInfoService accountInfoService = CDI.current().select(AccountInfoService.class).get();
    private AccountLeaseService accountLeaseService = CDI.current().select(AccountLeaseService.class).get();
    private AccountAssetService accountAssetService = CDI.current().select(AccountAssetService.class).get();
    private AccountCurrencyService accountCurrencyService = CDI.current().select(AccountCurrencyService.class).get();

    public GetAccount() {
        super(new APITag[]{APITag.ACCOUNTS}, "account", "includeLessors", "includeAssets", "includeCurrencies", "includeEffectiveBalance");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = HttpParameterParserUtil.getAccount(req);
        boolean includeLessors = "true".equalsIgnoreCase(req.getParameter("includeLessors"));
        boolean includeAssets = "true".equalsIgnoreCase(req.getParameter("includeAssets"));
        boolean includeCurrencies = "true".equalsIgnoreCase(req.getParameter("includeCurrencies"));
        boolean includeEffectiveBalance = "true".equalsIgnoreCase(req.getParameter("includeEffectiveBalance"));

        Balances balances = lookupAccountService().getAccountBalances(account, includeEffectiveBalance);

        JSONObject response = balances.balanceToJson();
        JSONData.putAccount(response, "account", account.getId());
        response.put("is2FA", Helper2FA.isEnabled2FA(account.getId()));
        byte[] publicKey = lookupAccountService().getPublicKeyByteArray(account.getId());
        if (publicKey != null) {
            response.put("publicKey", Convert.toHexString(publicKey));
        }
        AccountInfo accountInfo = accountInfoService.getAccountInfo(account);
        if (accountInfo != null) {
            response.put("name", Convert.nullToEmpty(accountInfo.getName()));
            response.put("description", Convert.nullToEmpty(accountInfo.getDescription()));
        }
        AccountLease accountLease = accountLeaseService.getAccountLease(account);
        if (accountLease != null) {
            JSONData.putAccount(response, "currentLessee", accountLease.getCurrentLesseeId());
            response.put("currentLeasingHeightFrom", accountLease.getCurrentLeasingHeightFrom());
            response.put("currentLeasingHeightTo", accountLease.getCurrentLeasingHeightTo());
            if (accountLease.getNextLesseeId() != 0) {
                JSONData.putAccount(response, "nextLessee", accountLease.getNextLesseeId());
                response.put("nextLeasingHeightFrom", accountLease.getNextLeasingHeightFrom());
                response.put("nextLeasingHeightTo", accountLease.getNextLeasingHeightTo());
            }
        }

        if (!account.getControls().isEmpty()) {
            JSONArray accountControlsJson = new JSONArray();
            account.getControls().forEach(accountControl -> accountControlsJson.add(accountControl.toString()));
            response.put("accountControls", accountControlsJson);
        }

        if (includeLessors) {
            try (DbIterator<Account> lessors = lookupAccountService().getLessorsIterator(account)) {
                if (lessors.hasNext()) {
                    JSONArray lessorIds = new JSONArray();
                    JSONArray lessorIdsRS = new JSONArray();
                    JSONArray lessorInfo = new JSONArray();
                    while (lessors.hasNext()) {
                        Account lessor = lessors.next();
                        lessorIds.add(Long.toUnsignedString(lessor.getId()));
                        lessorIdsRS.add(Convert2.rsAccount(lessor.getId()));
                        lessorInfo.add(JSONData.lessor(lessor, includeEffectiveBalance));
                    }
                    response.put("lessors", lessorIds);
                    response.put("lessorsRS", lessorIdsRS);
                    response.put("lessorsInfo", lessorInfo);
                }
            }
        }

        if (includeAssets) {
            List<AccountAsset> assets = accountAssetService.getAssetsByAccount(account, 0, -1);

            JSONArray assetBalances = new JSONArray();
            JSONArray unconfirmedAssetBalances = new JSONArray();
            assets.forEach(accountAsset -> {
                JSONObject assetBalance = new JSONObject();
                assetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                assetBalance.put("balanceATU", String.valueOf(accountAsset.getQuantityATU()));
                assetBalances.add(assetBalance);
                JSONObject unconfirmedAssetBalance = new JSONObject();
                unconfirmedAssetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                unconfirmedAssetBalance.put("unconfirmedBalanceATU", String.valueOf(accountAsset.getUnconfirmedQuantityATU()));
                unconfirmedAssetBalances.add(unconfirmedAssetBalance);
            });

            if (assetBalances.size() > 0) {
                response.put("assetBalances", assetBalances);
            }
            if (unconfirmedAssetBalances.size() > 0) {
                response.put("unconfirmedAssetBalances", unconfirmedAssetBalances);
            }
        }

        if (includeCurrencies) {
            List<AccountCurrency> accountCurrencies = accountCurrencyService.getCurrenciesByAccount(account, 0, -1);
            JSONArray currencyJSON = new JSONArray();
            accountCurrencies.forEach(accountCurrency -> currencyJSON.add(JSONData.accountCurrency(accountCurrency, false, true)));
            if (currencyJSON.size() > 0) {
                response.put("accountCurrencies", currencyJSON);
            }
        }

        return response;

    }

}
