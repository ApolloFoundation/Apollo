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

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.app.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class GetBlockchainStatus extends AbstractAPIRequestHandler {

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
    public JSONObject processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        response.put("application", Constants.APPLICATION);
        response.put("version", Constants.VERSION.toString());
        response.put("time", AplCore.getEpochTime());
        Block lastBlock = AplCore.getBlockchain().getLastBlock();
        response.put("lastBlock", lastBlock.getStringId());
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("numberOfBlocks", lastBlock.getHeight() + 1);
        BlockchainProcessor blockchainProcessor = AplCore.getBlockchainProcessor();
        Peer lastBlockchainFeeder = blockchainProcessor.getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", blockchainProcessor.getLastBlockchainFeederHeight());
        response.put("isScanning", blockchainProcessor.isScanning());
        response.put("isDownloading", blockchainProcessor.isDownloading());
        response.put("maxRollback", Constants.MAX_ROLLBACK);
        response.put("currentMinRollbackHeight", AplCore.getBlockchainProcessor().getMinRollbackHeight());
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        response.put("isTestnet", blockchainConfig.isTestnet());
        response.put("maxPrunableLifetime", blockchainConfig.getMaxPrunableLifetime());
        response.put("includeExpiredPrunable", Constants.INCLUDE_EXPIRED_PRUNABLE);
        response.put("correctInvalidFees", Constants.correctInvalidFees);
        response.put("ledgerTrimKeep", AccountLedger.trimKeep);
        response.put("chainId", blockchainConfig.getChain().getChainId());
        response.put("chainName", blockchainConfig.getChain().getName());
        response.put("chainDescription", blockchainConfig.getChain().getDescription());
        response.put("blockTime", blockchainConfig.getCurrentConfig().getBlockTime());
        response.put("adaptiveForging", blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled());
        response.put("emptyBlockTime", blockchainConfig.getCurrentConfig().getAdaptiveBlockTime());
        response.put("consensus", blockchainConfig.getCurrentConfig().getConsensusType());
        response.put("maxBlockPayloadLength", blockchainConfig.getCurrentConfig().getMaxPayloadLength());
        response.put("initialBaseTarget", Long.toUnsignedString(blockchainConfig.getCurrentConfig().getInitialBaseTarget()));
        response.put("coinSymbol", blockchainConfig.getCoinSymbol());
        response.put("accountPrefix", blockchainConfig.getAccountPrefix());
        response.put("projectName", blockchainConfig.getProjectName());
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
