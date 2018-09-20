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
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.DECRYPTION_FAILED;
import static org.slf4j.LoggerFactory.getLogger;

public final class GetDGSPurchase extends APIServlet.APIRequestHandler {
    private static final Logger LOG = getLogger(GetDGSPurchase.class);


    private static class GetDGSPurchaseHolder {
        private static final GetDGSPurchase INSTANCE = new GetDGSPurchase();
    }

    public static GetDGSPurchase getInstance() {
        return GetDGSPurchaseHolder.INSTANCE;
    }

    private GetDGSPurchase() {
        super(new APITag[] {APITag.DGS}, "purchase", "secretPhrase", "sharedKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        JSONObject response = JSONData.purchase(purchase);

        byte[] sharedKey = ParameterParser.getBytes(req, "sharedKey", false);
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        if (sharedKey.length != 0 && secretPhrase != null) {
            return JSONResponses.either("secretPhrase", "sharedKey");
        }
        if (sharedKey.length == 0 && secretPhrase == null) {
            return response;
        }
        if (purchase.getEncryptedGoods() != null) {
            byte[] data = purchase.getEncryptedGoods().getData();
            try {
                byte[] decrypted = Convert.EMPTY_BYTE;
                if (data.length != 0) {
                    if (secretPhrase != null) {
                        byte[] readerPublicKey = Crypto.getPublicKey(secretPhrase);
                        byte[] sellerPublicKey = Account.getPublicKey(purchase.getSellerId());
                        byte[] buyerPublicKey = Account.getPublicKey(purchase.getBuyerId());
                        byte[] publicKey = Arrays.equals(sellerPublicKey, readerPublicKey) ? buyerPublicKey : sellerPublicKey;
                        if (publicKey != null) {
                            decrypted = Account.decryptFrom(publicKey, purchase.getEncryptedGoods(), secretPhrase, true);
                        }
                    } else {
                        decrypted = Crypto.aesDecrypt(purchase.getEncryptedGoods().getData(), sharedKey);
                        decrypted = Convert.uncompress(decrypted);
                    }
                }
                response.put("decryptedGoods", Convert.toString(decrypted, purchase.goodsIsText()));
            } catch (RuntimeException e) {
                LOG.debug(e.toString());
                return DECRYPTION_FAILED;
            }
        }
        return response;
    }

}
