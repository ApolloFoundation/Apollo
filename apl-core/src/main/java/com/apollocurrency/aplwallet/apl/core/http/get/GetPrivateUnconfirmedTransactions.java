/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

@Vetoed
public final class GetPrivateUnconfirmedTransactions extends AbstractAPIRequestHandler {

    public GetPrivateUnconfirmedTransactions() {
        super(new APITag[]{APITag.TRANSACTIONS, APITag.ACCOUNTS}, "firstIndex", "lastIndex", "secretPhrase", "publicKey");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        HttpParameterParserUtil.PrivateTransactionsAPIData data = HttpParameterParserUtil.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }
        JSONArray transactions = new JSONArray();
        CollectionUtil.forEach(lookupMemPool()
            .getAllStream()
            .filter(transaction -> data.getAccountId() == transaction.getSenderId() || data.getAccountId() == transaction.getRecipientId())
            .skip(firstIndex)
            .limit(limit), e-> {
            if (data.isEncrypt() && ((Transaction) e).getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT) {
                    transactions.add(JSONData.encryptedUnconfirmedTransaction(e, data.getSharedKey()));
                } else {
                    transactions.add(JSONData.unconfirmedTransaction(e));
                }
        });
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactions);
        response.put("serverPublicKey", Convert.toHexString(elGamal.getServerPublicKey()));
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}

