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

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.http.APIServlet;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.JSON;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetConstants extends AbstractAPIRequestHandler {
    private static final Logger LOG = getLogger(GetConstants.class);

    private static final class Holder {

        private static final JSONStreamAware CONSTANTS;

        static {
            try {
                JSONObject response = new JSONObject();
                BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();;
                response.put("genesisBlockId", Long.toUnsignedString(blockchainProcessor.getGenesisBlockId()));
                response.put("genesisAccountId", Long.toUnsignedString(Genesis.CREATOR_ID));
                response.put("epochBeginning", Genesis.EPOCH_BEGINNING);
                response.put("maxArbitraryMessageLength", Constants.MAX_ARBITRARY_MESSAGE_LENGTH);
                response.put("maxPrunableMessageLength", Constants.MAX_PRUNABLE_MESSAGE_LENGTH);
                BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
                response.put("coinSymbol", blockchainConfig.getCoinSymbol());
                response.put("accountPrefix", blockchainConfig.getAccountPrefix());
                response.put("projectName", blockchainConfig.getProjectName());
                JSONObject transactionJSON = new JSONObject();
                JSONObject transactionSubTypesJSON = new JSONObject();
                outer:
                for (int type = 0; ; type++) {
                    JSONObject typeJSON = new JSONObject();
                    JSONObject subtypesJSON = new JSONObject();
                    for (int subtype = 0; ; subtype++) {
                        TransactionType transactionType;
                        try {
                            transactionType = TransactionType.findTransactionType((byte) type, (byte) subtype);
                        } catch (IllegalArgumentException ignore) {
                            continue;
                        }
                        if (transactionType == null) {
                            if (subtype == 0) {
                                break outer;
                            } else {
                                break;
                            }
                        }
                        JSONObject subtypeJSON = new JSONObject();
                        subtypeJSON.put("name", transactionType.getName());
                        subtypeJSON.put("canHaveRecipient", transactionType.canHaveRecipient());
                        subtypeJSON.put("mustHaveRecipient", transactionType.mustHaveRecipient());
                        subtypeJSON.put("isPhasingSafe", transactionType.isPhasingSafe());
                        subtypeJSON.put("isPhasable", transactionType.isPhasable());
                        subtypeJSON.put("type", type);
                        subtypeJSON.put("subtype", subtype);
                        subtypesJSON.put(subtype, subtypeJSON);
                        transactionSubTypesJSON.put(transactionType.getName(), subtypeJSON);
                    }
                    typeJSON.put("subtypes", subtypesJSON);
                    transactionJSON.put(type, typeJSON);
                }
                response.put("transactionTypes", transactionJSON);
                response.put("transactionSubTypes", transactionSubTypesJSON);

                JSONObject currencyTypes = new JSONObject();
                for (CurrencyType currencyType : CurrencyType.values()) {
                    currencyTypes.put(currencyType.toString(), currencyType.getCode());
                }
                response.put("currencyTypes", currencyTypes);

                JSONObject votingModels = new JSONObject();
                for (VoteWeighting.VotingModel votingModel : VoteWeighting.VotingModel.values()) {
                    votingModels.put(votingModel.toString(), votingModel.getCode());
                }
                response.put("votingModels", votingModels);

                JSONObject minBalanceModels = new JSONObject();
                for (VoteWeighting.MinBalanceModel minBalanceModel : VoteWeighting.MinBalanceModel.values()) {
                    minBalanceModels.put(minBalanceModel.toString(), minBalanceModel.getCode());
                }
                response.put("minBalanceModels", minBalanceModels);

                JSONObject hashFunctions = new JSONObject();
                for (HashFunction hashFunction : HashFunction.values()) {
                    hashFunctions.put(hashFunction.toString(), hashFunction.getId());
                }
                response.put("hashAlgorithms", hashFunctions);

                JSONObject phasingHashFunctions = new JSONObject();
                for (HashFunction hashFunction : PhasingPollService.HASH_FUNCTIONS) {
                    phasingHashFunctions.put(hashFunction.toString(), hashFunction.getId());
                }
                response.put("phasingHashAlgorithms", phasingHashFunctions);

                response.put("maxPhasingDuration", Constants.MAX_PHASING_DURATION);

                JSONObject mintingHashFunctions = new JSONObject();
                for (HashFunction hashFunction : CurrencyMinting.acceptedHashFunctions) {
                    mintingHashFunctions.put(hashFunction.toString(), hashFunction.getId());
                }
                response.put("mintingHashAlgorithms", mintingHashFunctions);

                JSONObject peerStates = new JSONObject();
                for (Peer.State peerState : Peer.State.values()) {
                    peerStates.put(peerState.toString(), peerState.ordinal());
                }
                response.put("peerStates", peerStates);
                response.put("maxTaggedDataDataLength", Constants.MAX_TAGGED_DATA_DATA_LENGTH);

                JSONObject requestTypes = new JSONObject();
                for (Map.Entry<String, AbstractAPIRequestHandler> handlerEntry : APIServlet.apiRequestHandlers.entrySet()) {
                    JSONObject handlerJSON = JSONData.apiRequestHandler(handlerEntry.getValue());
                    handlerJSON.put("enabled", true);
                    requestTypes.put(handlerEntry.getKey(), handlerJSON);
                }
                for (Map.Entry<String, AbstractAPIRequestHandler> handlerEntry : APIServlet.disabledRequestHandlers.entrySet()) {
                    JSONObject handlerJSON = JSONData.apiRequestHandler(handlerEntry.getValue());
                    handlerJSON.put("enabled", false);
                    requestTypes.put(handlerEntry.getKey(), handlerJSON);
                }
                response.put("requestTypes", requestTypes);

                JSONObject holdingTypes = new JSONObject();
                for (HoldingType holdingType : HoldingType.values()) {
                    holdingTypes.put(holdingType.toString(), holdingType.getCode());
                }
                response.put("holdingTypes", holdingTypes);

                JSONObject shufflingStages = new JSONObject();
                for (Shuffling.Stage stage : Shuffling.Stage.values()) {
                    shufflingStages.put(stage.toString(), stage.getCode());
                }
                response.put("shufflingStages", shufflingStages);

                JSONObject shufflingParticipantStates = new JSONObject();
                for (ShufflingParticipant.State state : ShufflingParticipant.State.values()) {
                    shufflingParticipantStates.put(state.toString(), state.getCode());
                }
                response.put("shufflingParticipantStates", shufflingParticipantStates);

                JSONObject apiTags = new JSONObject();
                for (APITag apiTag : APITag.values()) {
                    JSONObject tagJSON = new JSONObject();
                    tagJSON.put("name", apiTag.getDisplayName());
                    tagJSON.put("enabled", !API.disabledAPITags.contains(apiTag));
                    apiTags.put(apiTag.name(), tagJSON);
                }
                response.put("apiTags", apiTags);

                JSONArray disabledAPIs = new JSONArray();
                Collections.addAll(disabledAPIs, API.disabledAPIs);
                response.put("disabledAPIs", disabledAPIs);

                JSONArray disabledAPITags = new JSONArray();
                API.disabledAPITags.forEach(apiTag -> disabledAPITags.add(apiTag.getDisplayName()));
                response.put("disabledAPITags", disabledAPITags);

                JSONArray notForwardedRequests = new JSONArray();
                notForwardedRequests.addAll(APIProxy.NOT_FORWARDED_REQUESTS);
                response.put("proxyNotForwardedRequests", notForwardedRequests);
                CONSTANTS = JSON.prepare(response);
            } catch (Exception e) {
                LOG.error(e.toString(), e);
                throw e;
            }
        }
    }

    public GetConstants() {
        super(new APITag[] {APITag.INFO});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        return Holder.CONSTANTS;

    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    public static JSONStreamAware getConstants() {
        return Holder.CONSTANTS;
    }
}
