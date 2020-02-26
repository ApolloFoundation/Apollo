/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class BlockchainStateDto extends BlockchainStatusDto {
    public int numberOfTransactions;
    public int numberOfAccounts;
    public int numberOfAssets;
    public int numberOfOrders;
    public int numberOfAskOrders;
    public int numberOfBidOrders;
    public int numberOfTrades;
    public int numberOfTransfers;
    public int numberOfCurrencies;
    public int numberOfOffers;
    public int numberOfExchangeRequests;
    public int numberOfExchanges;
    public int numberOfCurrencyTransfers;
    public int numberOfAliases;
    public int numberOfGoods;
    public int numberOfPurchases;
    public int numberOfTags;
    public int numberOfPolls;
    public int numberOfVotes;
    public int numberOfPrunableMessages;
    public int numberOfTaggedData;
    public int numberOfDataTags;
    public int numberOfAccountLeases;
    public int numberOfActiveAccountLeases;
    public int numberOfShufflings;
    public int numberOfActiveShufflings;
    public int numberOfPhasingOnlyAccounts;

    public int numberOfPeers;
    public int numberOfActivePeers;
    public int numberOfUnlockedAccounts;
    public int availableProcessors;
    public long maxMemory;
    public long totalMemory;
    public long freeMemory;
    public int peerPort;
    public boolean isOffline;
    public boolean needsAdminPassword;
    public String customLoginWarning;
    public String upnpExternalAddress;

    public BlockchainStateDto(BlockchainStatusDto stateDto) {
        super(stateDto);
    }
}
