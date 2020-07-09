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

import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.DECRYPTION_FAILED;
import static org.slf4j.LoggerFactory.getLogger;

@Vetoed
public final class GetDGSPurchase extends AbstractAPIRequestHandler {
    private static final Logger LOG = getLogger(GetDGSPurchase.class);
    private DGSService service = CDI.current().select(DGSService.class).get();

    public GetDGSPurchase() {
        super(new APITag[]{APITag.DGS}, "purchase", "secretPhrase", "sharedKey", "account", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        DGSPurchase purchase = HttpParameterParserUtil.getPurchase(service, req);
        JSONObject response = JSONData.purchase(service, purchase);

        byte[] sharedKey = HttpParameterParserUtil.getBytes(req, "sharedKey", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, false);
        if (sharedKey.length != 0 && keySeed != null) {
            return JSONResponses.either("secretPhrase", "sharedKey", "passphrase & account");
        }
        if (sharedKey.length == 0 && keySeed == null) {
            return response;
        }
        if (purchase.getEncryptedGoods() != null) {
            byte[] data = purchase.getEncryptedGoods().getData();
            try {
                byte[] decrypted = Convert.EMPTY_BYTE;
                if (data.length != 0) {
                    if (keySeed != null) {
                        byte[] readerPublicKey = Crypto.getPublicKey(keySeed);
                        byte[] sellerPublicKey = lookupAccountService().getPublicKeyByteArray(purchase.getSellerId());
                        byte[] buyerPublicKey = lookupAccountService().getPublicKeyByteArray(purchase.getBuyerId());
                        byte[] publicKey = Arrays.equals(sellerPublicKey, readerPublicKey) ? buyerPublicKey : sellerPublicKey;
                        if (publicKey != null) {
                            decrypted = EncryptedDataUtil.decryptFrom(publicKey, purchase.getEncryptedGoods(), keySeed, true);
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
