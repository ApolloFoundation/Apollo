/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for implemented endpoints of new API.
 * Should be removed along with ApiSplitFilter after
 * @author alukin@gmail.com
 */
public class NewApiRegistry {
    private static Map<String,String> apis = new HashMap<>();
    static{
        apis.put("getServerInfo", "/rest/serverinfo");
        apis.put("importKeyStore", "/rest/keyStore/upload");
        apis.put("exportKeyStore", "/rest/keyStore/download");
        apis.put("getAccountInfo", "/rest/keyStore/accountInfo");

        apis.put("getEthWalletAmount", "/rest/wallet/eth");
        apis.put("getEthWalletTransfer", "/rest/wallet/eth/transfer");


        apis.put("getDexHistory", "/rest/dex/history");
        apis.put("getDexOffers", "/rest/dex/offers");
        apis.put("getDexOrders", "/rest/dex/order");
        apis.put("getDexWidthraw", "/rest/dex/widthraw");

        apis.put("getMyInfo", "/rest/networking/peer/mypeerinfo"); //GET
        apis.put("getPeer", "/rest/networking/peer"); //GET
        apis.put("addPeer", "/rest/networking/peer"); //POST
        apis.put("getPeers", "/rest/networking/peer/all"); //GET
        apis.put("getInboundPeers",  "/rest/networking/peer/inbound"); //GET
        apis.put("blacklistPeer", "/rest/networking/peer/blacklist"); //POST
        apis.put("blacklistAPIProxyPeer", "/rest/networking/peer/proxyblacklist"); //POST
        apis.put("setAPIProxyPeer", "/rest/networking/peer/setproxy"); //POST

        apis.put("getAccount", "/rest/accounts/account"); //GET
        apis.put("generateAccount", "/rest/accounts/account"); //POST
        apis.put("enable2FA", "/rest/accounts/enable2FA"); //POST
        apis.put("disable2FA", "/rest/accounts/disable2FA"); //POST
        apis.put("confirm2FA", "/rest/accounts/confirm2FA"); //POST
        apis.put("deleteKey", "/rest/accounts/deleteKey"); //POST
        apis.put("exportKey", "/rest/accounts/exportKey"); //POST
        apis.put("getAccountAssetCount", "/rest/accounts/account/assetCount"); //GET
        apis.put("getAccountAssets", "/rest/accounts/account/assets"); //GET
        apis.put("getAccountBlockCount", "/rest/accounts/account/blockCount"); //GET
        apis.put("getAccountBlockIds", "/rest/accounts/account/blockIds"); //GET
        apis.put("getAccountBlocks", "/rest/accounts/account/blocks"); //GET
        apis.put("getAccountCurrencyCount", "/rest/accounts/account/currencyCount"); //GET
        apis.put("getAccountCurrencies", "/rest/accounts/account/currencies"); //GET
        apis.put("getAccountCurrentAskOrderIds", "/rest/accounts/account/currentAskOrderIds"); //GET



        //TODO: add new implemented endpoints
    }
    public static String getRestPath(String rqType) {
        if(rqType==null || rqType.isEmpty()){
            return null;
        }
        return apis.get(rqType);
    }

}
