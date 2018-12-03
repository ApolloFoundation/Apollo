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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.AccountLedger;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.peer.Peer;
import com.apollocurrency.aplwallet.apl.peer.Peers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class GetBlockchainStatus extends APIServlet.APIRequestHandler {

    private static class GetBlockchainStatusHolder {
        private static final GetBlockchainStatus INSTANCE = new GetBlockchainStatus();
    }

    public static GetBlockchainStatus getInstance() {
        return GetBlockchainStatusHolder.INSTANCE;
    }

    private GetBlockchainStatus() {
        super(new APITag[] {APITag.BLOCKS, APITag.INFO});
    }

    @Override
    protected JSONObject processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        response.put("application", Apl.APPLICATION);
        response.put("version", Apl.VERSION.toString());
        response.put("time", Apl.getEpochTime());
        Block lastBlock = Apl.getBlockchain().getLastBlock();
        response.put("lastBlock", lastBlock.getStringId());
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("numberOfBlocks", lastBlock.getHeight() + 1);
        BlockchainProcessor blockchainProcessor = Apl.getBlockchainProcessor();
        Peer lastBlockchainFeeder = blockchainProcessor.getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", blockchainProcessor.getLastBlockchainFeederHeight());
        response.put("isScanning", blockchainProcessor.isScanning());
        response.put("isDownloading", blockchainProcessor.isDownloading());
        response.put("maxRollback", Constants.MAX_ROLLBACK);
        response.put("currentMinRollbackHeight", Apl.getBlockchainProcessor().getMinRollbackHeight());
        response.put("isTestnet", AplGlobalObjects.getChainConfig().isTestnet());
        response.put("maxPrunableLifetime", AplGlobalObjects.getChainConfig().getMaxPrunableLifetime());
        response.put("includeExpiredPrunable", Constants.INCLUDE_EXPIRED_PRUNABLE);
        response.put("correctInvalidFees", Constants.correctInvalidFees);
        response.put("ledgerTrimKeep", AccountLedger.trimKeep);
        response.put("chainId", AplGlobalObjects.getChainConfig().getChain().getChainId());
        response.put("chainName", AplGlobalObjects.getChainConfig().getChain().getName());
        response.put("chainDescription", AplGlobalObjects.getChainConfig().getChain().getDescription());
        response.put("blockTime", AplGlobalObjects.getChainConfig().getCurrentConfig().getBlockTime());
        response.put("adaptiveForging", AplGlobalObjects.getChainConfig().getCurrentConfig().isAdaptiveForgingEnabled());
        response.put("emptyBlockTime", AplGlobalObjects.getChainConfig().getCurrentConfig().getAdaptiveBlockTime());
        response.put("consensus", AplGlobalObjects.getChainConfig().getCurrentConfig().getConsensusType());
        response.put("maxBlockPayloadLength", AplGlobalObjects.getChainConfig().getCurrentConfig().getMaxPayloadLength());
        response.put("initialBaseTarget", Long.toUnsignedString(AplGlobalObjects.getChainConfig().getCurrentConfig().getInitialBaseTarget()));
        response.put("coinSymbol", AplGlobalObjects.getChainConfig().getCoinSymbol());
        response.put("accountPrefix", AplGlobalObjects.getChainConfig().getAccountPrefix());
        response.put("projectName", AplGlobalObjects.getChainConfig().getProjectName());
        JSONArray servicesArray = new JSONArray();
        Peers.getServices().forEach(service -> servicesArray.add(service.name()));
        response.put("services", servicesArray);
        if (APIProxy.isActivated()) {
            String servingPeer = APIProxy.getInstance().getMainPeerAnnouncedAddress();
            response.put("apiProxy", true);
            response.put("apiProxyPeer", servingPeer);
        } else {
            response.put("apiProxy", false);
        }
        response.put("isLightClient", Constants.isLightClient);
        response.put("maxAPIRecords", API.maxRecords);
        response.put("blockchainState", Peers.getMyBlockchainState());
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
