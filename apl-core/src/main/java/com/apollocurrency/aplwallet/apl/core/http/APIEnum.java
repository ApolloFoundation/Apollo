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
    GET_EL_GAMAL_PUBLIC_KEY("getElGamalPublicKey", GetElGamalPublicKey.getInstance()),
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
    START_MINOR_UPDATE("startAvailableUpdate", StartAvailableUpdate.getInstance()),
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
