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

package com.apollocurrency.aplwallet.apl.core.http;

import java.util.*;

import com.apollocurrency.aplwallet.apl.core.http.get.CalculateFullHash;
import com.apollocurrency.aplwallet.apl.core.http.get.CanDeleteCurrency;
import com.apollocurrency.aplwallet.apl.core.http.get.DecodeHallmark;
import com.apollocurrency.aplwallet.apl.core.http.get.DecodeToken;
import com.apollocurrency.aplwallet.apl.core.http.get.DecryptFrom;
import com.apollocurrency.aplwallet.apl.core.http.get.DownloadPrunableMessage;
import com.apollocurrency.aplwallet.apl.core.http.get.DownloadTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.get.EncryptTo;
import com.apollocurrency.aplwallet.apl.core.http.get.FullHashToId;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountAssetCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountAssets;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountBlockCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountBlockIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountBlocks;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountCurrencies;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountCurrencyCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountCurrentAskOrderIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountCurrentAskOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountCurrentBidOrderIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountCurrentBidOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountExchangeRequests;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountId;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountLedger;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountLedgerEntry;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountLessors;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountPhasedTransactionCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountPhasedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountProperties;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountPublicKey;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountShufflings;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccounts;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAlias;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAliasCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAliases;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAliasesLike;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllAssets;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllBroadcastedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllCurrencies;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllExchanges;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllOpenAskOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllOpenBidOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllPhasingOnlyControls;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllPrunableMessages;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllShufflings;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllTrades;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAllWaitingTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAskOrder;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAskOrderIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAskOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAsset;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetAccountCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetAccounts;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetDeletes;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetDividends;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetPhasedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetTransfers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssets;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssetsByIssuer;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAssignedShufflings;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAvailableToBuy;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAvailableToSell;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBalance;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBidOrder;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBidOrderIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBidOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBlock;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBlockId;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBlockchainStatus;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBlockchainTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBlocks;
import com.apollocurrency.aplwallet.apl.core.http.get.GetBuyOffers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetChannelTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.get.GetChatHistory;
import com.apollocurrency.aplwallet.apl.core.http.get.GetChats;
import com.apollocurrency.aplwallet.apl.core.http.get.GetConstants;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencies;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrenciesByIssuer;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrency;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyAccountCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyAccounts;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyFounders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyPhasedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetCurrencyTransfers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSExpiredPurchases;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSGood;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSGoods;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSGoodsCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSGoodsPurchaseCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSGoodsPurchases;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSPendingPurchases;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSPurchase;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSPurchaseCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSPurchases;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSTagCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSTags;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDGSTagsLike;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDataTagCount;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDataTags;
import com.apollocurrency.aplwallet.apl.core.http.get.GetDataTagsLike;
import com.apollocurrency.aplwallet.apl.core.http.get.GetECBlock;
import com.apollocurrency.aplwallet.apl.core.http.get.GetElGamalPublicKey;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExchanges;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExchangesByExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExchangesByOffer;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedAskOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedAssetDeletes;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedAssetTransfers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedBidOrders;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedBuyOffers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedCurrencyTransfers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedExchangeRequests;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedOrderCancellations;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedSellOffers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExpectedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetForging;
import com.apollocurrency.aplwallet.apl.core.http.get.GetFundingMonitor;
import com.apollocurrency.aplwallet.apl.core.http.get.GetGenesisBalances;
import com.apollocurrency.aplwallet.apl.core.http.get.GetGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.http.get.GetHoldingShufflings;
import com.apollocurrency.aplwallet.apl.core.http.get.GetInboundPeers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetLastExchanges;
import com.apollocurrency.aplwallet.apl.core.http.get.GetLastTrades;
import com.apollocurrency.aplwallet.apl.core.http.get.GetLinkedPhasedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetLog;
import com.apollocurrency.aplwallet.apl.core.http.get.GetMintingTarget;
import com.apollocurrency.aplwallet.apl.core.http.get.GetMyInfo;
import com.apollocurrency.aplwallet.apl.core.http.get.GetNextBlockGeneratorsTemp;
import com.apollocurrency.aplwallet.apl.core.http.get.GetOffer;
import com.apollocurrency.aplwallet.apl.core.http.get.GetOrderTrades;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPeer;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPeers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPhasingOnlyControl;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPhasingPoll;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPhasingPollVote;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPhasingPollVotes;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPhasingPolls;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPlugins;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPoll;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPollResult;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPollVote;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPollVotes;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPolls;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrivateAccountLedger;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrivateAccountLedgerEntry;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrivateBlockchainTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrivateTransaction;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrivateUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrunableMessage;
import com.apollocurrency.aplwallet.apl.core.http.get.GetPrunableMessages;
import com.apollocurrency.aplwallet.apl.core.http.get.GetReferencingTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetScheduledTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetSellOffers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetSharedKey;
import com.apollocurrency.aplwallet.apl.core.http.get.GetShufflers;
import com.apollocurrency.aplwallet.apl.core.http.get.GetShuffling;
import com.apollocurrency.aplwallet.apl.core.http.get.GetShufflingParticipants;
import com.apollocurrency.aplwallet.apl.core.http.get.GetStackTraces;
import com.apollocurrency.aplwallet.apl.core.http.get.GetState;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTaggedDataExtendTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTime;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTotalSupply;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTrades;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTransaction;
import com.apollocurrency.aplwallet.apl.core.http.get.GetTransactionBytes;
import com.apollocurrency.aplwallet.apl.core.http.get.GetUnconfirmedTransactionIds;
import com.apollocurrency.aplwallet.apl.core.http.get.GetUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.GetUpdateStatus;
import com.apollocurrency.aplwallet.apl.core.http.get.GetVotedAccountPolls;
import com.apollocurrency.aplwallet.apl.core.http.get.GetVoterPhasedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.get.Hash;
import com.apollocurrency.aplwallet.apl.core.http.get.HexConvert;
import com.apollocurrency.aplwallet.apl.core.http.get.ImportKey;
import com.apollocurrency.aplwallet.apl.core.http.get.LongConvert;
import com.apollocurrency.aplwallet.apl.core.http.get.ParseTransaction;
import com.apollocurrency.aplwallet.apl.core.http.get.RSConvert;
import com.apollocurrency.aplwallet.apl.core.http.get.ReadMessage;
import com.apollocurrency.aplwallet.apl.core.http.get.SearchAccounts;
import com.apollocurrency.aplwallet.apl.core.http.get.SearchAssets;
import com.apollocurrency.aplwallet.apl.core.http.get.SearchCurrencies;
import com.apollocurrency.aplwallet.apl.core.http.get.SearchDGSGoods;
import com.apollocurrency.aplwallet.apl.core.http.get.SearchPolls;
import com.apollocurrency.aplwallet.apl.core.http.get.SearchTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.get.SignTransaction;
import com.apollocurrency.aplwallet.apl.core.http.get.VerifyPrunableMessage;
import com.apollocurrency.aplwallet.apl.core.http.get.VerifyTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.post.AddPeer;
import com.apollocurrency.aplwallet.apl.core.http.post.ApproveTransaction;
import com.apollocurrency.aplwallet.apl.core.http.post.BlacklistAPIProxyPeer;
import com.apollocurrency.aplwallet.apl.core.http.post.BlacklistPeer;
import com.apollocurrency.aplwallet.apl.core.http.post.BroadcastTransaction;
import com.apollocurrency.aplwallet.apl.core.http.post.BuyAlias;
import com.apollocurrency.aplwallet.apl.core.http.post.CancelAskOrder;
import com.apollocurrency.aplwallet.apl.core.http.post.CancelBidOrder;
import com.apollocurrency.aplwallet.apl.core.http.post.CastVote;
import com.apollocurrency.aplwallet.apl.core.http.post.ClearUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.post.Confirm2FA;
import com.apollocurrency.aplwallet.apl.core.http.post.CreatePoll;
import com.apollocurrency.aplwallet.apl.core.http.post.CurrencyBuy;
import com.apollocurrency.aplwallet.apl.core.http.post.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.http.post.CurrencyReserveClaim;
import com.apollocurrency.aplwallet.apl.core.http.post.CurrencyReserveIncrease;
import com.apollocurrency.aplwallet.apl.core.http.post.CurrencySell;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSDelisting;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSDelivery;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSListing;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSPriceChange;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSQuantityChange;
import com.apollocurrency.aplwallet.apl.core.http.post.DGSRefund;
import com.apollocurrency.aplwallet.apl.core.http.post.DecodeFileToken;
import com.apollocurrency.aplwallet.apl.core.http.post.DecodeQRCode;
import com.apollocurrency.aplwallet.apl.core.http.post.DeleteAccountProperty;
import com.apollocurrency.aplwallet.apl.core.http.post.DeleteAlias;
import com.apollocurrency.aplwallet.apl.core.http.post.DeleteAssetShares;
import com.apollocurrency.aplwallet.apl.core.http.post.DeleteCurrency;
import com.apollocurrency.aplwallet.apl.core.http.post.DeleteKey;
import com.apollocurrency.aplwallet.apl.core.http.post.DeleteScheduledTransaction;
import com.apollocurrency.aplwallet.apl.core.http.post.DetectMimeType;
import com.apollocurrency.aplwallet.apl.core.http.post.Disable2FA;
import com.apollocurrency.aplwallet.apl.core.http.post.DividendPayment;
import com.apollocurrency.aplwallet.apl.core.http.post.DumpPeers;
import com.apollocurrency.aplwallet.apl.core.http.post.Enable2FA;
import com.apollocurrency.aplwallet.apl.core.http.post.EncodeQRCode;
import com.apollocurrency.aplwallet.apl.core.http.post.EventRegister;
import com.apollocurrency.aplwallet.apl.core.http.post.EventWait;
import com.apollocurrency.aplwallet.apl.core.http.post.ExportKey;
import com.apollocurrency.aplwallet.apl.core.http.post.ExtendTaggedData;
import com.apollocurrency.aplwallet.apl.core.http.post.FullReset;
import com.apollocurrency.aplwallet.apl.core.http.post.GenerateAccount;
import com.apollocurrency.aplwallet.apl.core.http.post.GenerateFileToken;
import com.apollocurrency.aplwallet.apl.core.http.post.GenerateToken;
import com.apollocurrency.aplwallet.apl.core.http.post.IssueAsset;
import com.apollocurrency.aplwallet.apl.core.http.post.IssueCurrency;
import com.apollocurrency.aplwallet.apl.core.http.post.LeaseBalance;
import com.apollocurrency.aplwallet.apl.core.http.post.LuceneReindex;
import com.apollocurrency.aplwallet.apl.core.http.post.MarkHost;
import com.apollocurrency.aplwallet.apl.core.http.post.PlaceAskOrder;
import com.apollocurrency.aplwallet.apl.core.http.post.PlaceBidOrder;
import com.apollocurrency.aplwallet.apl.core.http.post.PopOff;
import com.apollocurrency.aplwallet.apl.core.http.post.PublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.http.post.RebroadcastUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.post.RequeueUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.http.post.RetrievePrunedData;
import com.apollocurrency.aplwallet.apl.core.http.post.RetrievePrunedTransaction;
import com.apollocurrency.aplwallet.apl.core.http.post.Scan;
import com.apollocurrency.aplwallet.apl.core.http.post.ScheduleCurrencyBuy;
import com.apollocurrency.aplwallet.apl.core.http.post.SellAlias;
import com.apollocurrency.aplwallet.apl.core.http.post.SendMessage;
import com.apollocurrency.aplwallet.apl.core.http.post.SendMoney;
import com.apollocurrency.aplwallet.apl.core.http.post.SendMoneyPrivate;
import com.apollocurrency.aplwallet.apl.core.http.post.SendTransaction;
import com.apollocurrency.aplwallet.apl.core.http.post.SendUpdateTransaction;
import com.apollocurrency.aplwallet.apl.core.http.post.SetAPIProxyPeer;
import com.apollocurrency.aplwallet.apl.core.http.post.SetAccountInfo;
import com.apollocurrency.aplwallet.apl.core.http.post.SetAccountProperty;
import com.apollocurrency.aplwallet.apl.core.http.post.SetAlias;
import com.apollocurrency.aplwallet.apl.core.http.post.SetLogging;
import com.apollocurrency.aplwallet.apl.core.http.post.SetPhasingOnlyControl;
import com.apollocurrency.aplwallet.apl.core.http.post.ShufflingCancel;
import com.apollocurrency.aplwallet.apl.core.http.post.ShufflingCreate;
import com.apollocurrency.aplwallet.apl.core.http.post.ShufflingProcess;
import com.apollocurrency.aplwallet.apl.core.http.post.ShufflingRegister;
import com.apollocurrency.aplwallet.apl.core.http.post.ShufflingVerify;
import com.apollocurrency.aplwallet.apl.core.http.post.Shutdown;
import com.apollocurrency.aplwallet.apl.core.http.post.StartAvailableUpdate;
import com.apollocurrency.aplwallet.apl.core.http.post.StartForging;
import com.apollocurrency.aplwallet.apl.core.http.post.StartFundingMonitor;
import com.apollocurrency.aplwallet.apl.core.http.post.StartShuffler;
import com.apollocurrency.aplwallet.apl.core.http.post.StopForging;
import com.apollocurrency.aplwallet.apl.core.http.post.StopFundingMonitor;
import com.apollocurrency.aplwallet.apl.core.http.post.StopShuffler;
import com.apollocurrency.aplwallet.apl.core.http.post.TransferAsset;
import com.apollocurrency.aplwallet.apl.core.http.post.TransferCurrency;
import com.apollocurrency.aplwallet.apl.core.http.post.TrimDerivedTables;
import com.apollocurrency.aplwallet.apl.core.http.post.UploadTaggedData;

public enum APIEnum {
    //To preserve compatibility, please add new APIs to the end of the enum.
    //When an API is deleted, set its name to empty string and handler to null.
    APPROVE_TRANSACTION("approveTransaction", new ApproveTransaction()),
    BROADCAST_TRANSACTION("broadcastTransaction", new BroadcastTransaction()),
    CALCULATE_FULL_HASH("calculateFullHash", new CalculateFullHash()),
    CANCEL_ASK_ORDER("cancelAskOrder", new CancelAskOrder()),
    CANCEL_BID_ORDER("cancelBidOrder", new CancelBidOrder()),
    CAST_VOTE("castVote", new CastVote()),
    CREATE_POLL("createPoll", new CreatePoll()),
    CURRENCY_BUY("currencyBuy", new CurrencyBuy()),
    CURRENCY_SELL("currencySell", new CurrencySell()),
    CURRENCY_RESERVE_INCREASE("currencyReserveIncrease", new CurrencyReserveIncrease()),
    CURRENCY_RESERVE_CLAIM("currencyReserveClaim", new CurrencyReserveClaim()),
    CURRENCY_MINT("currencyMint", new CurrencyMint()),
    DECRYPT_FROM("decryptFrom", new DecryptFrom()),
    DELETE_ASSET_SHARES("deleteAssetShares", new DeleteAssetShares()),
    DGS_LISTING("dgsListing", new DGSListing()),
    DGS_DELISTING("dgsDelisting", new DGSDelisting()),
    DGS_DELIVERY("dgsDelivery", new DGSDelivery()),
    DGS_FEEDBACK("dgsFeedback", new DGSFeedback()),
    DGS_PRICE_CHANGE("dgsPriceChange", new DGSPriceChange()),
    DGS_PURCHASE("dgsPurchase", new DGSPurchase()),
    DGS_QUANTITY_CHANGE("dgsQuantityChange", new DGSQuantityChange()),
    DGS_REFUND("dgsRefund", new DGSRefund()),
    DECODE_HALLMARK("decodeHallmark", new DecodeHallmark()),
    DECODE_TOKEN("decodeToken", new DecodeToken()),
    DECODE_FILE_TOKEN("decodeFileToken", new DecodeFileToken()),
    DECODE_Q_R_CODE("decodeQRCode", new DecodeQRCode()),
    ENCODE_Q_R_CODE("encodeQRCode", new  EncodeQRCode()),
    ENCRYPT_TO("encryptTo", new EncryptTo()),
    EVENT_REGISTER("eventRegister", new EventRegister()),
    EVENT_WAIT("eventWait", new EventWait()),
    GENERATE_TOKEN("generateToken", new GenerateToken()),
    GENERATE_FILE_TOKEN("generateFileToken", new GenerateFileToken()),
    GET_ACCOUNT("getAccount", new GetAccount()),
    GET_ACCOUNT_BLOCK_COUNT("getAccountBlockCount", new GetAccountBlockCount()),
    GET_ACCOUNT_BLOCK_IDS("getAccountBlockIds", new GetAccountBlockIds()),
    GET_ACCOUNT_BLOCKS("getAccountBlocks", new GetAccountBlocks()),
    GET_ACCOUNT_ID("getAccountId", new GetAccountId()),
    GET_ACCOUNT_LEDGER("getAccountLedger", new GetAccountLedger()),
    GET_ACCOUNT_LEDGER_ENTRY("getAccountLedgerEntry", new GetAccountLedgerEntry()),
    GET_VOTER_PHASED_TRANSACTIONS("getVoterPhasedTransactions", new GetVoterPhasedTransactions()),
    GET_LINKED_PHASED_TRANSACTIONS("getLinkedPhasedTransactions", new GetLinkedPhasedTransactions()),
    GET_POLLS("getPolls", new GetPolls()),
    GET_ACCOUNT_PHASED_TRANSACTIONS("getAccountPhasedTransactions", new GetAccountPhasedTransactions()),
    GET_ACCOUNT_PHASED_TRANSACTION_COUNT("getAccountPhasedTransactionCount", new GetAccountPhasedTransactionCount()),
    GET_ACCOUNT_PUBLIC_KEY("getAccountPublicKey", new GetAccountPublicKey()),
    GET_ACCOUNT_LESSORS("getAccountLessors", new GetAccountLessors()),
    GET_ACCOUNT_ASSETS("getAccountAssets", new GetAccountAssets()),
    GET_ACCOUNT_CURRENCIES("getAccountCurrencies", new GetAccountCurrencies()),
    GET_ACCOUNT_CURRENCY_COUNT("getAccountCurrencyCount", new GetAccountCurrencyCount()),
    GET_ACCOUNT_ASSET_COUNT("getAccountAssetCount", new GetAccountAssetCount()),
    GET_ACCOUNT_PROPERTIES("getAccountProperties", new GetAccountProperties()),
    SELL_ALIAS("sellAlias", new SellAlias()),
    BUY_ALIAS("buyAlias", new BuyAlias()),
    GET_ALIAS("getAlias", new GetAlias()),
    GET_ALIAS_COUNT("getAliasCount", new GetAliasCount()),
    GET_ALIASES("getAliases", new GetAliases()),
    GET_ALIASES_LIKE("getAliasesLike", new GetAliasesLike()),
    GET_ALL_ASSETS("getAllAssets", new GetAllAssets()),
    GET_ALL_CURRENCIES("getAllCurrencies", new GetAllCurrencies()),
    GET_ASSET("getAsset", new GetAsset()),
    GET_ASSETS("getAssets", new GetAssets()),
    GET_ASSET_IDS("getAssetIds", new GetAssetIds()),
    GET_ASSETS_BY_ISSUER("getAssetsByIssuer", new GetAssetsByIssuer()),
    GET_ASSET_ACCOUNTS("getAssetAccounts", new GetAssetAccounts()),
    GET_ASSET_ACCOUNT_COUNT("getAssetAccountCount", new GetAssetAccountCount()),
    GET_ASSET_PHASED_TRANSACTIONS("getAssetPhasedTransactions", new GetAssetPhasedTransactions()),
    GET_BALANCE("getBalance", new GetBalance()),
    GET_BLOCK("getBlock", new GetBlock()),
    GET_BLOCK_ID("getBlockId", new GetBlockId()),
    GET_BLOCKS("getBlocks", new GetBlocks()),
    GET_BLOCKCHAIN_STATUS("getBlockchainStatus", new GetBlockchainStatus()),
    GET_BLOCKCHAIN_TRANSACTIONS("getBlockchainTransactions", new GetBlockchainTransactions()),
    GET_REFERENCING_TRANSACTIONS("getReferencingTransactions", new GetReferencingTransactions()),
    GET_CONSTANTS("getConstants", new GetConstants()),
    GET_CURRENCY("getCurrency", new GetCurrency()),
    GET_CURRENCIES("getCurrencies", new GetCurrencies()),
    GET_CURRENCY_FOUNDERS("getCurrencyFounders", new GetCurrencyFounders()),
    GET_CURRENCY_IDS("getCurrencyIds", new GetCurrencyIds()),
    GET_CURRENCIES_BY_ISSUER("getCurrenciesByIssuer", new GetCurrenciesByIssuer()),
    GET_CURRENCY_ACCOUNTS("getCurrencyAccounts", new GetCurrencyAccounts()),
    GET_CURRENCY_ACCOUNT_COUNT("getCurrencyAccountCount", new GetCurrencyAccountCount()),
    GET_CURRENCY_PHASED_TRANSACTIONS("getCurrencyPhasedTransactions", new GetCurrencyPhasedTransactions()),
    GET_DGS_GOODS("getDGSGoods", new GetDGSGoods()),
    GET_DGS_GOODS_COUNT("getDGSGoodsCount", new GetDGSGoodsCount()),
    GET_DGS_GOOD("getDGSGood", new GetDGSGood()),
    GET_DGS_GOODS_PURCHASES("getDGSGoodsPurchases", new GetDGSGoodsPurchases()),
    GET_DGS_GOODS_PURCHASE_COUNT("getDGSGoodsPurchaseCount", new GetDGSGoodsPurchaseCount()),
    GET_DGS_PURCHASES("getDGSPurchases", new GetDGSPurchases()),
    GET_DGS_PURCHASE("getDGSPurchase", new GetDGSPurchase()),
    GET_DGS_PURCHASE_COUNT("getDGSPurchaseCount", new GetDGSPurchaseCount()),
    GET_DGS_PENDING_PURCHASES("getDGSPendingPurchases", new GetDGSPendingPurchases()),
    GET_DGS_EXPIRED_PURCHASES("getDGSExpiredPurchases", new GetDGSExpiredPurchases()),
    GET_DGS_TAGS("getDGSTags", new GetDGSTags()),
    GET_DGS_TAG_COUNT("getDGSTagCount", new GetDGSTagCount()),
    GET_DGS_TAGS_LIKE("getDGSTagsLike", new GetDGSTagsLike()),
    GET_GUARANTEED_BALANCE("getGuaranteedBalance", new GetGuaranteedBalance()),
    GET_E_C_BLOCK("getECBlock", new GetECBlock()),
    GET_INBOUND_PEERS("getInboundPeers", new GetInboundPeers()),
    GET_PLUGINS("getPlugins", new GetPlugins()),
    GET_MY_INFO("getMyInfo", new GetMyInfo()),
    GET_PEER("getPeer", new GetPeer()),
    GET_PEERS("getPeers", new GetPeers()),
    GET_PHASING_POLL("getPhasingPoll", new GetPhasingPoll()),
    GET_PHASING_POLLS("getPhasingPolls", new GetPhasingPolls()),
    GET_PHASING_POLL_VOTES("getPhasingPollVotes", new GetPhasingPollVotes()),
    GET_PHASING_POLL_VOTE("getPhasingPollVote", new GetPhasingPollVote()),
    GET_POLL("getPoll", new GetPoll()),
    GET_POLL_RESULT("getPollResult", new GetPollResult()),
    GET_POLL_VOTES("getPollVotes", new GetPollVotes()),
    GET_POLL_VOTE("getPollVote", new GetPollVote()),
    GET_STATE("getState", new GetState()),
    GET_TIME("getTime", new GetTime()),
    GET_TRADES("getTrades", new GetTrades()),
    GET_LAST_TRADES("getLastTrades", new GetLastTrades()),
    GET_EXCHANGES("getExchanges", new GetExchanges()),
    GET_EXCHANGES_BY_EXCHANGE_REQUEST("getExchangesByExchangeRequest", new GetExchangesByExchangeRequest()),
    GET_EXCHANGES_BY_OFFER("getExchangesByOffer", new GetExchangesByOffer()),
    GET_LAST_EXCHANGES("getLastExchanges", new GetLastExchanges()),
    GET_ALL_TRADES("getAllTrades", new GetAllTrades()),
    GET_ALL_EXCHANGES("getAllExchanges", new GetAllExchanges()),
    GET_ASSET_TRANSFERS("getAssetTransfers", new GetAssetTransfers()),
    GET_ASSET_DELETES("getAssetDeletes", new GetAssetDeletes()),
    GET_EXPECTED_ASSET_TRANSFERS("getExpectedAssetTransfers", new GetExpectedAssetTransfers()),
    GET_EXPECTED_ASSET_DELETES("getExpectedAssetDeletes", new GetExpectedAssetDeletes()),
    GET_EL_GAMAL_PUBLIC_KEY("getElGamalPublicKey", new GetElGamalPublicKey()),
    GET_CURRENCY_TRANSFERS("getCurrencyTransfers", new GetCurrencyTransfers()),
    GET_EXPECTED_CURRENCY_TRANSFERS("getExpectedCurrencyTransfers", new GetExpectedCurrencyTransfers()),
    GET_TRANSACTION("getTransaction", new GetTransaction()),
    GET_TRANSACTION_BYTES("getTransactionBytes", new GetTransactionBytes()),
    GET_UNCONFIRMED_TRANSACTION_IDS("getUnconfirmedTransactionIds", new GetUnconfirmedTransactionIds()),
    GET_UNCONFIRMED_TRANSACTIONS("getUnconfirmedTransactions", new GetUnconfirmedTransactions()),
    GET_EXPECTED_TRANSACTIONS("getExpectedTransactions", new GetExpectedTransactions()),
    GET_ACCOUNT_CURRENT_ASK_ORDER_IDS("getAccountCurrentAskOrderIds", new GetAccountCurrentAskOrderIds()),
    GET_ACCOUNT_CURRENT_BID_ORDER_IDS("getAccountCurrentBidOrderIds", new GetAccountCurrentBidOrderIds()),
    GET_ACCOUNT_CURRENT_ASK_ORDERS("getAccountCurrentAskOrders", new GetAccountCurrentAskOrders()),
    GET_ACCOUNT_CURRENT_BID_ORDERS("getAccountCurrentBidOrders", new GetAccountCurrentBidOrders()),
    GET_ALL_OPEN_ASK_ORDERS("getAllOpenAskOrders", new GetAllOpenAskOrders()),
    GET_ALL_OPEN_BID_ORDERS("getAllOpenBidOrders", new GetAllOpenBidOrders()),
    GET_BUY_OFFERS("getBuyOffers", new GetBuyOffers()),
    GET_EXPECTED_BUY_OFFERS("getExpectedBuyOffers", new GetExpectedBuyOffers()),
    GET_SELL_OFFERS("getSellOffers", new GetSellOffers()),
    GET_EXPECTED_SELL_OFFERS("getExpectedSellOffers", new GetExpectedSellOffers()),
    GET_OFFER("getOffer", new GetOffer()),
    GET_AVAILABLE_TO_BUY("getAvailableToBuy", new GetAvailableToBuy()),
    GET_AVAILABLE_TO_SELL("getAvailableToSell", new GetAvailableToSell()),
    GET_ASK_ORDER("getAskOrder", new GetAskOrder()),
    GET_ASK_ORDER_IDS("getAskOrderIds", new GetAskOrderIds()),
    GET_ASK_ORDERS("getAskOrders", new GetAskOrders()),
    GET_BID_ORDER("getBidOrder", new GetBidOrder()),
    GET_BID_ORDER_IDS("getBidOrderIds", new GetBidOrderIds()),
    GET_BID_ORDERS("getBidOrders", new GetBidOrders()),
    GET_EXPECTED_ASK_ORDERS("getExpectedAskOrders", new GetExpectedAskOrders()),
    GET_EXPECTED_BID_ORDERS("getExpectedBidOrders", new GetExpectedBidOrders()),
    GET_EXPECTED_ORDER_CANCELLATIONS("getExpectedOrderCancellations", new GetExpectedOrderCancellations()),
    GET_ORDER_TRADES("getOrderTrades", new GetOrderTrades()),
    GET_ACCOUNT_EXCHANGE_REQUESTS("getAccountExchangeRequests", new GetAccountExchangeRequests()),
    GET_EXPECTED_EXCHANGE_REQUESTS("getExpectedExchangeRequests", new GetExpectedExchangeRequests()),
    GET_MINTING_TARGET("getMintingTarget", new GetMintingTarget()),
    GET_ALL_SHUFFLINGS("getAllShufflings", new GetAllShufflings()),
    GET_ACCOUNT_SHUFFLINGS("getAccountShufflings", new GetAccountShufflings()),
    GET_ASSIGNED_SHUFFLINGS("getAssignedShufflings", new GetAssignedShufflings()),
    GET_HOLDING_SHUFFLINGS("getHoldingShufflings", new GetHoldingShufflings()),
    GET_SHUFFLING("getShuffling", new GetShuffling()),
    GET_SHUFFLING_PARTICIPANTS("getShufflingParticipants", new GetShufflingParticipants()),
    GET_PRUNABLE_MESSAGE("getPrunableMessage", new GetPrunableMessage()),
    GET_PRUNABLE_MESSAGES("getPrunableMessages", new GetPrunableMessages()),
    GET_ALL_PRUNABLE_MESSAGES("getAllPrunableMessages", new GetAllPrunableMessages()),
    VERIFY_PRUNABLE_MESSAGE("verifyPrunableMessage", new VerifyPrunableMessage()),
    ISSUE_ASSET("issueAsset", new IssueAsset()),
    ISSUE_CURRENCY("issueCurrency", new IssueCurrency()),
    LEASE_BALANCE("leaseBalance", new LeaseBalance()),
    LONG_CONVERT("longConvert", new LongConvert()),
    HEX_CONVERT("hexConvert", new HexConvert()),
    MARK_HOST("markHost", new MarkHost()),
    PARSE_TRANSACTION("parseTransaction", new ParseTransaction()),
    PLACE_ASK_ORDER("placeAskOrder", new PlaceAskOrder()),
    PLACE_BID_ORDER("placeBidOrder", new PlaceBidOrder()),
    PUBLISH_EXCHANGE_OFFER("publishExchangeOffer", new PublishExchangeOffer()),
    RS_CONVERT("rsConvert", new RSConvert()),
    READ_MESSAGE("readMessage", new ReadMessage()),
    SEND_MESSAGE("sendMessage", new SendMessage()),
    SEND_MONEY("sendMoney", new SendMoney()),
    SET_ACCOUNT_INFO("setAccountInfo", new SetAccountInfo()),
    SET_ACCOUNT_PROPERTY("setAccountProperty", new SetAccountProperty()),
    DELETE_ACCOUNT_PROPERTY("deleteAccountProperty", new DeleteAccountProperty()),
    SET_ALIAS("setAlias", new SetAlias()),
    SHUFFLING_CREATE("shufflingCreate", new ShufflingCreate()),
    SHUFFLING_REGISTER("shufflingRegister", new ShufflingRegister()),
    SHUFFLING_PROCESS("shufflingProcess", new ShufflingProcess()),
    SHUFFLING_VERIFY("shufflingVerify", new ShufflingVerify()),
    SHUFFLING_CANCEL("shufflingCancel", new ShufflingCancel()),
    START_SHUFFLER("startShuffler", new StartShuffler()),
    STOP_SHUFFLER("stopShuffler", new StopShuffler()),
    GET_SHUFFLERS("getShufflers", new GetShufflers()),
    DELETE_ALIAS("deleteAlias", new DeleteAlias()),
    SIGN_TRANSACTION("signTransaction", new SignTransaction()),
    START_FORGING("startForging", new StartForging()),
    STOP_FORGING("stopForging", new StopForging()),
    GET_FORGING("getForging", new GetForging()),
    TRANSFER_ASSET("transferAsset", new TransferAsset()),
    TRANSFER_CURRENCY("transferCurrency", new TransferCurrency()),
    CAN_DELETE_CURRENCY("canDeleteCurrency", new CanDeleteCurrency()),
    DELETE_CURRENCY("deleteCurrency", new DeleteCurrency()),
    DIVIDEND_PAYMENT("dividendPayment", new DividendPayment()),
    SEARCH_DGS_GOODS("searchDGSGoods", new SearchDGSGoods()),
    SEARCH_ASSETS("searchAssets", new SearchAssets()),
    SEARCH_CURRENCIES("searchCurrencies", new SearchCurrencies()),
    SEARCH_POLLS("searchPolls", new SearchPolls()),
    SEARCH_ACCOUNTS("searchAccounts", new SearchAccounts()),
    SEARCH_TAGGED_DATA("searchTaggedData", new SearchTaggedData()),
    UPLOAD_TAGGED_DATA("uploadTaggedData", new UploadTaggedData()),
    EXTEND_TAGGED_DATA("extendTaggedData", new ExtendTaggedData()),
    GET_ACCOUNT_TAGGED_DATA("getAccountTaggedData", new GetAccountTaggedData()),
    GET_ALL_TAGGED_DATA("getAllTaggedData", new GetAllTaggedData()),
    GET_CHANNEL_TAGGED_DATA("getChannelTaggedData", new GetChannelTaggedData()),
    GET_TAGGED_DATA("getTaggedData", new GetTaggedData()),
    DOWNLOAD_TAGGED_DATA("downloadTaggedData", new DownloadTaggedData()),
    GET_DATA_TAGS("getDataTags", new GetDataTags()),
    GET_DATA_TAG_COUNT("getDataTagCount", new GetDataTagCount()),
    GET_DATA_TAGS_LIKE("getDataTagsLike", new GetDataTagsLike()),
    VERIFY_TAGGED_DATA("verifyTaggedData", new VerifyTaggedData()),
    GET_TAGGED_DATA_EXTEND_TRANSACTIONS("getTaggedDataExtendTransactions", new GetTaggedDataExtendTransactions()),
    CLEAR_UNCONFIRMED_TRANSACTIONS("clearUnconfirmedTransactions", new ClearUnconfirmedTransactions()),
    REQUEUE_UNCONFIRMED_TRANSACTIONS("requeueUnconfirmedTransactions", new RequeueUnconfirmedTransactions()),
    REBROADCAST_UNCONFIRMED_TRANSACTIONS("rebroadcastUnconfirmedTransactions", new RebroadcastUnconfirmedTransactions()),
    GET_ALL_WAITING_TRANSACTIONS("getAllWaitingTransactions", new GetAllWaitingTransactions()),
    GET_ALL_BROADCASTED_TRANSACTIONS("getAllBroadcastedTransactions", new GetAllBroadcastedTransactions()),
    FULL_RESET("fullReset", new FullReset()),
    POP_OFF("popOff", new PopOff()),
    SCAN("scan", new Scan()),
    LUCENE_REINDEX("luceneReindex", new LuceneReindex()),
    ADD_PEER("addPeer", new AddPeer()),
    BLACKLIST_PEER("blacklistPeer", new BlacklistPeer()),
    DUMP_PEERS("dumpPeers", new DumpPeers()),
    GET_LOG("getLog", new GetLog()),
    GET_STACK_TRACES("getStackTraces", new GetStackTraces()),
    RETRIEVE_PRUNED_DATA("retrievePrunedData", new RetrievePrunedData()),
    RETRIEVE_PRUNED_TRANSACTION("retrievePrunedTransaction", new RetrievePrunedTransaction()),
    SET_LOGGING("setLogging", new SetLogging()),
    SHUTDOWN("shutdown", new Shutdown()),
    TRIM_DERIVED_TABLES("trimDerivedTables", new TrimDerivedTables()),
    HASH("hash", new Hash()),
    FULL_HASH_TO_ID("fullHashToId", new FullHashToId()),
    SET_PHASING_ONLY_CONTROL("setPhasingOnlyControl", new SetPhasingOnlyControl()),
    GET_PHASING_ONLY_CONTROL("getPhasingOnlyControl", new GetPhasingOnlyControl()),
    GET_ALL_PHASING_ONLY_CONTROLS("getAllPhasingOnlyControls", new GetAllPhasingOnlyControls()),
    DETECT_MIME_TYPE("detectMimeType", new DetectMimeType()),
    START_FUNDING_MONITOR("startFundingMonitor", new StartFundingMonitor()),
    STOP_FUNDING_MONITOR("stopFundingMonitor", new StopFundingMonitor()),
    GET_FUNDING_MONITOR("getFundingMonitor", new GetFundingMonitor()),
    DOWNLOAD_PRUNABLE_MESSAGE("downloadPrunableMessage", new DownloadPrunableMessage()),
    GET_SHARED_KEY("getSharedKey", new GetSharedKey()),
    SET_API_PROXY_PEER("setAPIProxyPeer", new SetAPIProxyPeer()),
    SEND_TRANSACTION("sendTransaction", new SendTransaction()),
    GET_ASSET_DIVIDENDS("getAssetDividends", new GetAssetDividends()),
    BLACKLIST_API_PROXY_PEER("blacklistAPIProxyPeer", new BlacklistAPIProxyPeer()),
    GET_NEXT_BLOCK_GENERATORS("getNextBlockGenerators", new GetNextBlockGeneratorsTemp()),
    GET_SCHEDULED_TRANSACTIONS("getScheduledTransactions", new GetScheduledTransactions()),
    SCHEDULE_CURRENCY_BUY("scheduleCurrencyBuy", new ScheduleCurrencyBuy()),
    DELETE_SCHEDULED_TRANSACTION("deleteScheduledTransaction", new DeleteScheduledTransaction()),
    SEND_MONEY_PRIVATE("sendMoneyPrivate", new SendMoneyPrivate()),
    GET_PRIVATE_BLOCKCHAIN_TRANSACTIONS("getPrivateBlockchainTransactions", new GetPrivateBlockchainTransactions()),
    GET_PRIVATE_TRANSACTION("getPrivateTransaction", new GetPrivateTransaction()),
    GET_PRIVATE_ACCOUNT_LEDGER("getPrivateAccountLedger", new GetPrivateAccountLedger()),
    GET_PRIVATE_UNCONFIRMED_TRANSACTIONS("getPrivateUnconfirmedTransactions", new GetPrivateUnconfirmedTransactions()),
    GET_PRIVATE_ACCOUNT_LEDGER_ENTRY("getPrivateAccountLedgerEntry", new GetPrivateAccountLedgerEntry()),
    SEND_UPDATE_TRANSACTION("sendUpdateTransaction", new SendUpdateTransaction()),
    GET_UPDATE_STATUS("getUpdateStatus", new GetUpdateStatus()),
    START_MINOR_UPDATE("startAvailableUpdate", new StartAvailableUpdate()),
    GET_ALL_TRANSACTIONS("getAllTransactions", new GetAllTransactions()),
    GET_VOTED_ACCOUNT_POLLS("getVotedAccountPolls", new GetVotedAccountPolls()),
    GET_CHATS("getChats", new GetChats()),
    GET_CHAT_HISTORY("getChatHistory", new GetChatHistory()),
    GET_TOTAL_SUPPLY("getTotalSupply", new GetTotalSupply()),
    GET_ACCOUNTS("getAccounts", new GetAccounts()),
    GENERATE_ACCOUNT("generateAccount", new GenerateAccount()),
    EXPORT_KEY("exportKey", new ExportKey()),
    IMPORT_KEY("importKey", new ImportKey()),
    ENABLE_2FA("enable2FA", new Enable2FA()),
    DISABLE_2FA("disable2FA", new Disable2FA()),
    CONFIRM_2FA("confirm2FA", new Confirm2FA()),
    GET_GENESIS_BALANCES("getGenesisBalances", new GetGenesisBalances()),
    DELETE_KEY("deleteKey", new DeleteKey()),
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

            final AbstractAPIRequestHandler handler = api.getHandler();
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
    private final AbstractAPIRequestHandler handler;

    APIEnum(String name, AbstractAPIRequestHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public AbstractAPIRequestHandler getHandler() {
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
