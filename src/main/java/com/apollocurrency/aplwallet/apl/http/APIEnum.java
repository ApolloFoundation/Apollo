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

import java.util.*;

public enum APIEnum {
    //To preserve compatibility, please add new APIs to the end of the enum.
    //When an API is deleted, set its name to empty string and handler to null.
    APPROVE_TRANSACTION("approveTransaction", ApproveTransaction.getInstance()),
    BROADCAST_TRANSACTION("broadcastTransaction", BroadcastTransaction.getInstance()),
    CALCULATE_FULL_HASH("calculateFullHash", CalculateFullHash.getInstance()),
    CANCEL_ASK_ORDER("cancelAskOrder", CancelAskOrder.getInstance()),
    CANCEL_BID_ORDER("cancelBidOrder", CancelBidOrder.getInstance()),
    CAST_VOTE("castVote", CastVote.getInstance()),
    CREATE_POLL("createPoll", CreatePoll.getInstance()),
    CURRENCY_BUY("currencyBuy", CurrencyBuy.getInstance()),
    CURRENCY_SELL("currencySell", CurrencySell.getInstance()),
    CURRENCY_RESERVE_INCREASE("currencyReserveIncrease", CurrencyReserveIncrease.getInstance()),
    CURRENCY_RESERVE_CLAIM("currencyReserveClaim", CurrencyReserveClaim.getInstance()),
    CURRENCY_MINT("currencyMint", CurrencyMint.getInstance()),
    DECRYPT_FROM("decryptFrom", DecryptFrom.getInstance()),
    DELETE_ASSET_SHARES("deleteAssetShares", DeleteAssetShares.getInstance()),
    DGS_LISTING("dgsListing", DGSListing.getInstance()),
    DGS_DELISTING("dgsDelisting", DGSDelisting.getInstance()),
    DGS_DELIVERY("dgsDelivery", DGSDelivery.getInstance()),
    DGS_FEEDBACK("dgsFeedback", DGSFeedback.getInstance()),
    DGS_PRICE_CHANGE("dgsPriceChange", DGSPriceChange.getInstance()),
    DGS_PURCHASE("dgsPurchase", DGSPurchase.getInstance()),
    DGS_QUANTITY_CHANGE("dgsQuantityChange", DGSQuantityChange.getInstance()),
    DGS_REFUND("dgsRefund", DGSRefund.getInstance()),
    DECODE_HALLMARK("decodeHallmark", DecodeHallmark.getInstance()),
    DECODE_TOKEN("decodeToken", DecodeToken.getInstance()),
    DECODE_FILE_TOKEN("decodeFileToken", DecodeFileToken.getInstance()),
    DECODE_Q_R_CODE("decodeQRCode", DecodeQRCode.getInstance()),
    ENCODE_Q_R_CODE("encodeQRCode", EncodeQRCode.getInstance()),
    ENCRYPT_TO("encryptTo", EncryptTo.getInstance()),
    EVENT_REGISTER("eventRegister", EventRegister.getInstance()),
    EVENT_WAIT("eventWait", EventWait.getInstance()),
    GENERATE_TOKEN("generateToken", GenerateToken.getInstance()),
    GENERATE_FILE_TOKEN("generateFileToken", GenerateFileToken.getInstance()),
    GET_ACCOUNT("getAccount", GetAccount.getInstance()),
    GET_ACCOUNT_BLOCK_COUNT("getAccountBlockCount", GetAccountBlockCount.getInstance()),
    GET_ACCOUNT_BLOCK_IDS("getAccountBlockIds", GetAccountBlockIds.getInstance()),
    GET_ACCOUNT_BLOCKS("getAccountBlocks", GetAccountBlocks.getInstance()),
    GET_ACCOUNT_ID("getAccountId", GetAccountId.getInstance()),
    GET_ACCOUNT_LEDGER("getAccountLedger", GetAccountLedger.getInstance()),
    GET_ACCOUNT_LEDGER_ENTRY("getAccountLedgerEntry", GetAccountLedgerEntry.getInstance()),
    GET_VOTER_PHASED_TRANSACTIONS("getVoterPhasedTransactions", GetVoterPhasedTransactions.getInstance()),
    GET_LINKED_PHASED_TRANSACTIONS("getLinkedPhasedTransactions", GetLinkedPhasedTransactions.getInstance()),
    GET_POLLS("getPolls", GetPolls.getInstance()),
    GET_ACCOUNT_PHASED_TRANSACTIONS("getAccountPhasedTransactions", GetAccountPhasedTransactions.getInstance()),
    GET_ACCOUNT_PHASED_TRANSACTION_COUNT("getAccountPhasedTransactionCount", GetAccountPhasedTransactionCount.getInstance()),
    GET_ACCOUNT_PUBLIC_KEY("getAccountPublicKey", GetAccountPublicKey.getInstance()),
    GET_ACCOUNT_LESSORS("getAccountLessors", GetAccountLessors.getInstance()),
    GET_ACCOUNT_ASSETS("getAccountAssets", GetAccountAssets.getInstance()),
    GET_ACCOUNT_CURRENCIES("getAccountCurrencies", GetAccountCurrencies.getInstance()),
    GET_ACCOUNT_CURRENCY_COUNT("getAccountCurrencyCount", GetAccountCurrencyCount.getInstance()),
    GET_ACCOUNT_ASSET_COUNT("getAccountAssetCount", GetAccountAssetCount.getInstance()),
    GET_ACCOUNT_PROPERTIES("getAccountProperties", GetAccountProperties.getInstance()),
    SELL_ALIAS("sellAlias", SellAlias.getInstance()),
    BUY_ALIAS("buyAlias", BuyAlias.getInstance()),
    GET_ALIAS("getAlias", GetAlias.getInstance()),
    GET_ALIAS_COUNT("getAliasCount", GetAliasCount.getInstance()),
    GET_ALIASES("getAliases", GetAliases.getInstance()),
    GET_ALIASES_LIKE("getAliasesLike", GetAliasesLike.getInstance()),
    GET_ALL_ASSETS("getAllAssets", GetAllAssets.getInstance()),
    GET_ALL_CURRENCIES("getAllCurrencies", GetAllCurrencies.getInstance()),
    GET_ASSET("getAsset", GetAsset.getInstance()),
    GET_ASSETS("getAssets", GetAssets.getInstance()),
    GET_ASSET_IDS("getAssetIds", GetAssetIds.getInstance()),
    GET_ASSETS_BY_ISSUER("getAssetsByIssuer", GetAssetsByIssuer.getInstance()),
    GET_ASSET_ACCOUNTS("getAssetAccounts", GetAssetAccounts.getInstance()),
    GET_ASSET_ACCOUNT_COUNT("getAssetAccountCount", GetAssetAccountCount.getInstance()),
    GET_ASSET_PHASED_TRANSACTIONS("getAssetPhasedTransactions", GetAssetPhasedTransactions.getInstance()),
    GET_BALANCE("getBalance", GetBalance.getInstance()),
    GET_BLOCK("getBlock", GetBlock.getInstance()),
    GET_BLOCK_ID("getBlockId", GetBlockId.getInstance()),
    GET_BLOCKS("getBlocks", GetBlocks.getInstance()),
    GET_BLOCKCHAIN_STATUS("getBlockchainStatus", GetBlockchainStatus.getInstance()),
    GET_BLOCKCHAIN_TRANSACTIONS("getBlockchainTransactions", GetBlockchainTransactions.getInstance()),
    GET_REFERENCING_TRANSACTIONS("getReferencingTransactions", GetReferencingTransactions.getInstance()),
    GET_CONSTANTS("getConstants", GetConstants.getInstance()),
    GET_CURRENCY("getCurrency", GetCurrency.getInstance()),
    GET_CURRENCIES("getCurrencies", GetCurrencies.getInstance()),
    GET_CURRENCY_FOUNDERS("getCurrencyFounders", GetCurrencyFounders.getInstance()),
    GET_CURRENCY_IDS("getCurrencyIds", GetCurrencyIds.getInstance()),
    GET_CURRENCIES_BY_ISSUER("getCurrenciesByIssuer", GetCurrenciesByIssuer.getInstance()),
    GET_CURRENCY_ACCOUNTS("getCurrencyAccounts", GetCurrencyAccounts.getInstance()),
    GET_CURRENCY_ACCOUNT_COUNT("getCurrencyAccountCount", GetCurrencyAccountCount.getInstance()),
    GET_CURRENCY_PHASED_TRANSACTIONS("getCurrencyPhasedTransactions", GetCurrencyPhasedTransactions.getInstance()),
    GET_DGS_GOODS("getDGSGoods", GetDGSGoods.getInstance()),
    GET_DGS_GOODS_COUNT("getDGSGoodsCount", GetDGSGoodsCount.getInstance()),
    GET_DGS_GOOD("getDGSGood", GetDGSGood.getInstance()),
    GET_DGS_GOODS_PURCHASES("getDGSGoodsPurchases", GetDGSGoodsPurchases.getInstance()),
    GET_DGS_GOODS_PURCHASE_COUNT("getDGSGoodsPurchaseCount", GetDGSGoodsPurchaseCount.getInstance()),
    GET_DGS_PURCHASES("getDGSPurchases", GetDGSPurchases.getInstance()),
    GET_DGS_PURCHASE("getDGSPurchase", GetDGSPurchase.getInstance()),
    GET_DGS_PURCHASE_COUNT("getDGSPurchaseCount", GetDGSPurchaseCount.getInstance()),
    GET_DGS_PENDING_PURCHASES("getDGSPendingPurchases", GetDGSPendingPurchases.getInstance()),
    GET_DGS_EXPIRED_PURCHASES("getDGSExpiredPurchases", GetDGSExpiredPurchases.getInstance()),
    GET_DGS_TAGS("getDGSTags", GetDGSTags.getInstance()),
    GET_DGS_TAG_COUNT("getDGSTagCount", GetDGSTagCount.getInstance()),
    GET_DGS_TAGS_LIKE("getDGSTagsLike", GetDGSTagsLike.getInstance()),
    GET_GUARANTEED_BALANCE("getGuaranteedBalance", GetGuaranteedBalance.getInstance()),
    GET_E_C_BLOCK("getECBlock", GetECBlock.getInstance()),
    GET_INBOUND_PEERS("getInboundPeers", GetInboundPeers.getInstance()),
    GET_PLUGINS("getPlugins", GetPlugins.getInstance()),
    GET_MY_INFO("getMyInfo", GetMyInfo.getInstance()),
    GET_PEER("getPeer", GetPeer.getInstance()),
    GET_PEERS("getPeers", GetPeers.getInstance()),
    GET_PHASING_POLL("getPhasingPoll", GetPhasingPoll.getInstance()),
    GET_PHASING_POLLS("getPhasingPolls", GetPhasingPolls.getInstance()),
    GET_PHASING_POLL_VOTES("getPhasingPollVotes", GetPhasingPollVotes.getInstance()),
    GET_PHASING_POLL_VOTE("getPhasingPollVote", GetPhasingPollVote.getInstance()),
    GET_POLL("getPoll", GetPoll.getInstance()),
    GET_POLL_RESULT("getPollResult", GetPollResult.getInstance()),
    GET_POLL_VOTES("getPollVotes", GetPollVotes.getInstance()),
    GET_POLL_VOTE("getPollVote", GetPollVote.getInstance()),
    GET_STATE("getState", GetState.getInstance()),
    GET_TIME("getTime", GetTime.getInstance()),
    GET_TRADES("getTrades", GetTrades.getInstance()),
    GET_LAST_TRADES("getLastTrades", GetLastTrades.getInstance()),
    GET_EXCHANGES("getExchanges", GetExchanges.getInstance()),
    GET_EXCHANGES_BY_EXCHANGE_REQUEST("getExchangesByExchangeRequest", GetExchangesByExchangeRequest.getInstance()),
    GET_EXCHANGES_BY_OFFER("getExchangesByOffer", GetExchangesByOffer.getInstance()),
    GET_LAST_EXCHANGES("getLastExchanges", GetLastExchanges.getInstance()),
    GET_ALL_TRADES("getAllTrades", GetAllTrades.getInstance()),
    GET_ALL_EXCHANGES("getAllExchanges", GetAllExchanges.getInstance()),
    GET_ASSET_TRANSFERS("getAssetTransfers", GetAssetTransfers.getInstance()),
    GET_ASSET_DELETES("getAssetDeletes", GetAssetDeletes.getInstance()),
    GET_EXPECTED_ASSET_TRANSFERS("getExpectedAssetTransfers", GetExpectedAssetTransfers.getInstance()),
    GET_EXPECTED_ASSET_DELETES("getExpectedAssetDeletes", GetExpectedAssetDeletes.getInstance()),
    GET_CURRENCY_TRANSFERS("getCurrencyTransfers", GetCurrencyTransfers.getInstance()),
    GET_EXPECTED_CURRENCY_TRANSFERS("getExpectedCurrencyTransfers", GetExpectedCurrencyTransfers.getInstance()),
    GET_TRANSACTION("getTransaction", GetTransaction.getInstance()),
    GET_TRANSACTION_BYTES("getTransactionBytes", GetTransactionBytes.getInstance()),
    GET_UNCONFIRMED_TRANSACTION_IDS("getUnconfirmedTransactionIds", GetUnconfirmedTransactionIds.getInstance()),
    GET_UNCONFIRMED_TRANSACTIONS("getUnconfirmedTransactions", GetUnconfirmedTransactions.getInstance()),
    GET_EXPECTED_TRANSACTIONS("getExpectedTransactions", GetExpectedTransactions.getInstance()),
    GET_ACCOUNT_CURRENT_ASK_ORDER_IDS("getAccountCurrentAskOrderIds", GetAccountCurrentAskOrderIds.getInstance()),
    GET_ACCOUNT_CURRENT_BID_ORDER_IDS("getAccountCurrentBidOrderIds", GetAccountCurrentBidOrderIds.getInstance()),
    GET_ACCOUNT_CURRENT_ASK_ORDERS("getAccountCurrentAskOrders", GetAccountCurrentAskOrders.getInstance()),
    GET_ACCOUNT_CURRENT_BID_ORDERS("getAccountCurrentBidOrders", GetAccountCurrentBidOrders.getInstance()),
    GET_ALL_OPEN_ASK_ORDERS("getAllOpenAskOrders", GetAllOpenAskOrders.getInstance()),
    GET_ALL_OPEN_BID_ORDERS("getAllOpenBidOrders", GetAllOpenBidOrders.getInstance()),
    GET_BUY_OFFERS("getBuyOffers", GetBuyOffers.getInstance()),
    GET_EXPECTED_BUY_OFFERS("getExpectedBuyOffers", GetExpectedBuyOffers.getInstance()),
    GET_SELL_OFFERS("getSellOffers", GetSellOffers.getInstance()),
    GET_EXPECTED_SELL_OFFERS("getExpectedSellOffers", GetExpectedSellOffers.getInstance()),
    GET_OFFER("getOffer", GetOffer.getInstance()),
    GET_AVAILABLE_TO_BUY("getAvailableToBuy", GetAvailableToBuy.getInstance()),
    GET_AVAILABLE_TO_SELL("getAvailableToSell", GetAvailableToSell.getInstance()),
    GET_ASK_ORDER("getAskOrder", GetAskOrder.getInstance()),
    GET_ASK_ORDER_IDS("getAskOrderIds", GetAskOrderIds.getInstance()),
    GET_ASK_ORDERS("getAskOrders", GetAskOrders.getInstance()),
    GET_BID_ORDER("getBidOrder", GetBidOrder.getInstance()),
    GET_BID_ORDER_IDS("getBidOrderIds", GetBidOrderIds.getInstance()),
    GET_BID_ORDERS("getBidOrders", GetBidOrders.getInstance()),
    GET_EXPECTED_ASK_ORDERS("getExpectedAskOrders", GetExpectedAskOrders.getInstance()),
    GET_EXPECTED_BID_ORDERS("getExpectedBidOrders", GetExpectedBidOrders.getInstance()),
    GET_EXPECTED_ORDER_CANCELLATIONS("getExpectedOrderCancellations", GetExpectedOrderCancellations.getInstance()),
    GET_ORDER_TRADES("getOrderTrades", GetOrderTrades.getInstance()),
    GET_ACCOUNT_EXCHANGE_REQUESTS("getAccountExchangeRequests", GetAccountExchangeRequests.getInstance()),
    GET_EXPECTED_EXCHANGE_REQUESTS("getExpectedExchangeRequests", GetExpectedExchangeRequests.getInstance()),
    GET_MINTING_TARGET("getMintingTarget", GetMintingTarget.getInstance()),
    GET_ALL_SHUFFLINGS("getAllShufflings", GetAllShufflings.getInstance()),
    GET_ACCOUNT_SHUFFLINGS("getAccountShufflings", GetAccountShufflings.getInstance()),
    GET_ASSIGNED_SHUFFLINGS("getAssignedShufflings", GetAssignedShufflings.getInstance()),
    GET_HOLDING_SHUFFLINGS("getHoldingShufflings", GetHoldingShufflings.getInstance()),
    GET_SHUFFLING("getShuffling", GetShuffling.getInstance()),
    GET_SHUFFLING_PARTICIPANTS("getShufflingParticipants", GetShufflingParticipants.getInstance()),
    GET_PRUNABLE_MESSAGE("getPrunableMessage", GetPrunableMessage.getInstance()),
    GET_PRUNABLE_MESSAGES("getPrunableMessages", GetPrunableMessages.getInstance()),
    GET_ALL_PRUNABLE_MESSAGES("getAllPrunableMessages", GetAllPrunableMessages.getInstance()),
    VERIFY_PRUNABLE_MESSAGE("verifyPrunableMessage", VerifyPrunableMessage.getInstance()),
    ISSUE_ASSET("issueAsset", IssueAsset.getInstance()),
    ISSUE_CURRENCY("issueCurrency", IssueCurrency.getInstance()),
    LEASE_BALANCE("leaseBalance", LeaseBalance.getInstance()),
    LONG_CONVERT("longConvert", LongConvert.getInstance()),
    HEX_CONVERT("hexConvert", HexConvert.getInstance()),
    MARK_HOST("markHost", MarkHost.getInstance()),
    PARSE_TRANSACTION("parseTransaction", ParseTransaction.getInstance()),
    PLACE_ASK_ORDER("placeAskOrder", PlaceAskOrder.getInstance()),
    PLACE_BID_ORDER("placeBidOrder", PlaceBidOrder.getInstance()),
    PUBLISH_EXCHANGE_OFFER("publishExchangeOffer", PublishExchangeOffer.getInstance()),
    RS_CONVERT("rsConvert", RSConvert.getInstance()),
    READ_MESSAGE("readMessage", ReadMessage.getInstance()),
    SEND_MESSAGE("sendMessage", SendMessage.getInstance()),
    SEND_MONEY("sendMoney", SendMoney.getInstance()),
    SET_ACCOUNT_INFO("setAccountInfo", SetAccountInfo.getInstance()),
    SET_ACCOUNT_PROPERTY("setAccountProperty", SetAccountProperty.getInstance()),
    DELETE_ACCOUNT_PROPERTY("deleteAccountProperty", DeleteAccountProperty.getInstance()),
    SET_ALIAS("setAlias", SetAlias.getInstance()),
    SHUFFLING_CREATE("shufflingCreate", ShufflingCreate.getInstance()),
    SHUFFLING_REGISTER("shufflingRegister", ShufflingRegister.getInstance()),
    SHUFFLING_PROCESS("shufflingProcess", ShufflingProcess.getInstance()),
    SHUFFLING_VERIFY("shufflingVerify", ShufflingVerify.getInstance()),
    SHUFFLING_CANCEL("shufflingCancel", ShufflingCancel.getInstance()),
    START_SHUFFLER("startShuffler", StartShuffler.getInstance()),
    STOP_SHUFFLER("stopShuffler", StopShuffler.getInstance()),
    GET_SHUFFLERS("getShufflers", GetShufflers.getInstance()),
    DELETE_ALIAS("deleteAlias", DeleteAlias.getInstance()),
    SIGN_TRANSACTION("signTransaction", SignTransaction.getInstance()),
    START_FORGING("startForging", StartForging.getInstance()),
    STOP_FORGING("stopForging", StopForging.getInstance()),
    GET_FORGING("getForging", GetForging.getInstance()),
    TRANSFER_ASSET("transferAsset", TransferAsset.getInstance()),
    TRANSFER_CURRENCY("transferCurrency", TransferCurrency.getInstance()),
    CAN_DELETE_CURRENCY("canDeleteCurrency", CanDeleteCurrency.getInstance()),
    DELETE_CURRENCY("deleteCurrency", DeleteCurrency.getInstance()),
    DIVIDEND_PAYMENT("dividendPayment", DividendPayment.getInstance()),
    SEARCH_DGS_GOODS("searchDGSGoods", SearchDGSGoods.getInstance()),
    SEARCH_ASSETS("searchAssets", SearchAssets.getInstance()),
    SEARCH_CURRENCIES("searchCurrencies", SearchCurrencies.getInstance()),
    SEARCH_POLLS("searchPolls", SearchPolls.getInstance()),
    SEARCH_ACCOUNTS("searchAccounts", SearchAccounts.getInstance()),
    SEARCH_TAGGED_DATA("searchTaggedData", SearchTaggedData.getInstance()),
    UPLOAD_TAGGED_DATA("uploadTaggedData", UploadTaggedData.getInstance()),
    EXTEND_TAGGED_DATA("extendTaggedData", ExtendTaggedData.getInstance()),
    GET_ACCOUNT_TAGGED_DATA("getAccountTaggedData", GetAccountTaggedData.getInstance()),
    GET_ALL_TAGGED_DATA("getAllTaggedData", GetAllTaggedData.getInstance()),
    GET_CHANNEL_TAGGED_DATA("getChannelTaggedData", GetChannelTaggedData.getInstance()),
    GET_TAGGED_DATA("getTaggedData", GetTaggedData.getInstance()),
    DOWNLOAD_TAGGED_DATA("downloadTaggedData", DownloadTaggedData.getInstance()),
    GET_DATA_TAGS("getDataTags", GetDataTags.getInstance()),
    GET_DATA_TAG_COUNT("getDataTagCount", GetDataTagCount.getInstance()),
    GET_DATA_TAGS_LIKE("getDataTagsLike", GetDataTagsLike.getInstance()),
    VERIFY_TAGGED_DATA("verifyTaggedData", VerifyTaggedData.getInstance()),
    GET_TAGGED_DATA_EXTEND_TRANSACTIONS("getTaggedDataExtendTransactions", GetTaggedDataExtendTransactions.getInstance()),
    CLEAR_UNCONFIRMED_TRANSACTIONS("clearUnconfirmedTransactions", ClearUnconfirmedTransactions.getInstance()),
    REQUEUE_UNCONFIRMED_TRANSACTIONS("requeueUnconfirmedTransactions", RequeueUnconfirmedTransactions.getInstance()),
    REBROADCAST_UNCONFIRMED_TRANSACTIONS("rebroadcastUnconfirmedTransactions", RebroadcastUnconfirmedTransactions.getInstance()),
    GET_ALL_WAITING_TRANSACTIONS("getAllWaitingTransactions", GetAllWaitingTransactions.getInstance()),
    GET_ALL_BROADCASTED_TRANSACTIONS("getAllBroadcastedTransactions", GetAllBroadcastedTransactions.getInstance()),
    FULL_RESET("fullReset", FullReset.getInstance()),
    POP_OFF("popOff", PopOff.getInstance()),
    SCAN("scan", Scan.getInstance()),
    LUCENE_REINDEX("luceneReindex", LuceneReindex.getInstance()),
    ADD_PEER("addPeer", AddPeer.getInstance()),
    BLACKLIST_PEER("blacklistPeer", BlacklistPeer.getInstance()),
    DUMP_PEERS("dumpPeers", DumpPeers.getInstance()),
    GET_LOG("getLog", GetLog.getInstance()),
    GET_STACK_TRACES("getStackTraces", GetStackTraces.getInstance()),
    RETRIEVE_PRUNED_DATA("retrievePrunedData", RetrievePrunedData.getInstance()),
    RETRIEVE_PRUNED_TRANSACTION("retrievePrunedTransaction", RetrievePrunedTransaction.getInstance()),
    SET_LOGGING("setLogging", SetLogging.getInstance()),
    SHUTDOWN("shutdown", Shutdown.getInstance()),
    TRIM_DERIVED_TABLES("trimDerivedTables", TrimDerivedTables.getInstance()),
    HASH("hash", Hash.getInstance()),
    FULL_HASH_TO_ID("fullHashToId", FullHashToId.getInstance()),
    SET_PHASING_ONLY_CONTROL("setPhasingOnlyControl", SetPhasingOnlyControl.getInstance()),
    GET_PHASING_ONLY_CONTROL("getPhasingOnlyControl", GetPhasingOnlyControl.getInstance()),
    GET_ALL_PHASING_ONLY_CONTROLS("getAllPhasingOnlyControls", GetAllPhasingOnlyControls.getInstance()),
    DETECT_MIME_TYPE("detectMimeType", DetectMimeType.getInstance()),
    START_FUNDING_MONITOR("startFundingMonitor", StartFundingMonitor.getInstance()),
    STOP_FUNDING_MONITOR("stopFundingMonitor", StopFundingMonitor.getInstance()),
    GET_FUNDING_MONITOR("getFundingMonitor", GetFundingMonitor.getInstance()),
    DOWNLOAD_PRUNABLE_MESSAGE("downloadPrunableMessage", DownloadPrunableMessage.getInstance()),
    GET_SHARED_KEY("getSharedKey", GetSharedKey.getInstance()),
    SET_API_PROXY_PEER("setAPIProxyPeer", SetAPIProxyPeer.getInstance()),
    SEND_TRANSACTION("sendTransaction", SendTransaction.getInstance()),
    GET_ASSET_DIVIDENDS("getAssetDividends", GetAssetDividends.getInstance()),
    BLACKLIST_API_PROXY_PEER("blacklistAPIProxyPeer", BlacklistAPIProxyPeer.getInstance()),
    GET_NEXT_BLOCK_GENERATORS("getNextBlockGenerators", GetNextBlockGeneratorsTemp.getInstance()),
    GET_SCHEDULED_TRANSACTIONS("getScheduledTransactions", GetScheduledTransactions.getInstance()),
    SCHEDULE_CURRENCY_BUY("scheduleCurrencyBuy", ScheduleCurrencyBuy.getInstance()),
    DELETE_SCHEDULED_TRANSACTION("deleteScheduledTransaction", DeleteScheduledTransaction.getInstance()),
    SEND_MONEY_PRIVATE("sendMoneyPrivate", SendMoneyPrivate.getInstance()),
    GET_PRIVATE_BLOCKCHAIN_TRANSACTIONS("getPrivateBlockchainTransactions", GetPrivateBlockchainTransactions.getInstance()),
    GET_PRIVATE_TRANSACTION("getPrivateTransaction", GetPrivateTransaction.getInstance()),
    GET_PRIVATE_ACCOUNT_LEDGER("getPrivateAccountLedger", GetPrivateAccountLedger.getInstance()),
    GET_PRIVATE_UNCONFIRMED_TRANSACTIONS("getPrivateUnconfirmedTransactions", GetPrivateUnconfirmedTransactions.getInstance()),
    GET_PRIVATE_ACCOUNT_LEDGER_ENTRY("getPrivateAccountLedgerEntry", GetPrivateAccountLedgerEntry.getInstance()),
    SEND_UPDATE_TRANSACTION("sendUpdateTransaction", SendUpdateTransaction.getInstance()),
    GET_UPDATE_STATUS("getUpdateStatus", GetUpdateStatus.getInstance()),
    START_MINOR_UPDATE("startMinorUpdate", StartMinorUpdate.getInstance()),
    GET_ALL_TRANSACTIONS("getAllTransactions", GetAllTransactions.getInstance()),
    GET_VOTED_ACCOUNT_POLLS("getVotedAccountPolls", GetVotedAccountPolls.getInstance()),
    GET_CHATS("getChats", GetChats.getInstance()),
    GET_CHAT_HISTORY("getChatHistory", GetChatHistory.getInstance()),
    GET_TOTAL_SUPPLY("getTotalSupply", GetTotalSupply.getInstance()),
    GET_ACCOUNTS("getAccounts", GetAccounts.getInstance()),
    GENERATE_ACCOUNT("generateAccount", GenerateAccount.getInstance()),
    EXPORT_KEY("exportKey", ExportKey.getInstance()),
    IMPORT_KEY("importKey", ImportKey.getInstance()),
    ENABLE_2FA("enable2FA", Enable2FA.getInstance()),
    DISABLE_2FA("disable2FA", Disable2FA.getInstance()),
    CONFIRM_2FA("confirm2FA", Confirm2FA.getInstance()),
    GET_GENESIS_BALANCES("getGenesisBalances", GetGenesisBalances.getInstance()),
    DELETE_KEY("deleteKey", DeleteKey.getInstance()),
    ;
    private static final Map<String, APIEnum> apiByName = new HashMap<>();

    static {
        final EnumSet<APITag> tagsNotRequiringBlockchain = EnumSet.of(APITag.UTILS);
        for (APIEnum api : values()) {
            if (apiByName.put(api.getName(), api) != null) {
                AssertionError assertionError = new AssertionError("Duplicate API name: " + api.getName());
                assertionError.printStackTrace();
                throw assertionError;
            }

            final APIServlet.APIRequestHandler handler = api.getHandler();
            if (!Collections.disjoint(handler.getAPITags(), tagsNotRequiringBlockchain)
                    && handler.requireBlockchain()) {
                AssertionError assertionError = new AssertionError("API " + api.getName()
                        + " is not supposed to require blockchain");
                assertionError.printStackTrace();
                throw assertionError;
            }
        }
    }

    public static APIEnum fromName(String name) {
        return apiByName.get(name);
    }

    private final String name;
    private final APIServlet.APIRequestHandler handler;

    APIEnum(String name, APIServlet.APIRequestHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public APIServlet.APIRequestHandler getHandler() {
        return handler;
    }

    public static EnumSet<APIEnum> base64StringToEnumSet(String apiSetBase64) {
        byte[] decoded = Base64.getDecoder().decode(apiSetBase64);
        BitSet bs = BitSet.valueOf(decoded);
        EnumSet<APIEnum> result = EnumSet.noneOf(APIEnum.class);
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
            result.add(APIEnum.values()[i]);
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        return result;
    }

    public static String enumSetToBase64String(EnumSet<APIEnum> apiSet) {
        BitSet bitSet = new BitSet();
        for (APIEnum api: apiSet) {
            bitSet.set(api.ordinal());
        }
        return Base64.getEncoder().encodeToString(bitSet.toByteArray());
    }
}
