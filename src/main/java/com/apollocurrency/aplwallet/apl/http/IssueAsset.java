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

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ASSET_NAME;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DECIMALS;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_NAME;

public final class IssueAsset extends CreateTransaction {

    private static class IssueAssetHolder {
        private static final IssueAsset INSTANCE = new IssueAsset();
    }

    public static IssueAsset getInstance() {
        return IssueAssetHolder.INSTANCE;
    }

    private IssueAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "name", "description", "quantityATU", "decimals");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String decimalsValue = Convert.emptyToNull(req.getParameter("decimals"));

        if (name == null) {
            return MISSING_NAME;
        }

        name = name.trim();
        if (name.length() < Constants.MIN_ASSET_NAME_LENGTH || name.length() > Constants.MAX_ASSET_NAME_LENGTH) {
            return INCORRECT_ASSET_NAME_LENGTH;
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return INCORRECT_ASSET_NAME;
            }
        }

        if (description != null && description.length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
            return INCORRECT_ASSET_DESCRIPTION;
        }

        byte decimals = 0;
        if (decimalsValue != null) {
            try {
                decimals = Byte.parseByte(decimalsValue);
                if (decimals < 0 || decimals > 8) {
                    return INCORRECT_DECIMALS;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_DECIMALS;
            }
        }

        long quantityATU = ParameterParser.getQuantityATU(req);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantityATU, decimals);
        return createTransaction(req, account, attachment);

    }

}
