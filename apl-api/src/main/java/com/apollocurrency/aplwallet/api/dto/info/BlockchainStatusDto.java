/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.info;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class BlockchainStatusDto extends BaseDTO {
    public String application;
    public String version;
    public int time;

    public String lastBlock = "-1";
    public String cumulativeDifficulty = "-1";
    public int numberOfBlocks = -1;
    public long shardInitialBlock = -1L;
    public long lastShardHeight = -1L;

    public String lastBlockchainFeeder;
    public int lastBlockchainFeederHeight;
    public boolean isScanning;
    public boolean isDownloading;
    public int maxRollback;
    public int currentMinRollbackHeight;
    public int maxPrunableLifetime;
    public boolean includeExpiredPrunable;
    public boolean correctInvalidFees;
    public int ledgerTrimKeep;
    public UUID chainId;
    public String chainName;
    public String chainDescription;
    public int blockTime;
    public boolean adaptiveForging;
    public int adaptiveBlockTime;
    public String consensus;
    public int maxBlockPayloadLength;
    public String initialBaseTarget;
    public String coinSymbol;
    public String accountPrefix;
    public String projectName;
    public List<String> services = new ArrayList<>();

    public boolean apiProxy;
    public String apiProxyPeer;
    public boolean isLightClient;
    public int maxAPIRecords;
    public String blockchainState;

    public BlockchainStatusDto(String application, String version, int time, String blockchainState) {
        this.application = application;
        this.version = version;
        this.time = time;
        this.blockchainState = blockchainState;
    }

    public BlockchainStatusDto(BlockchainStatusDto statusDto) {
        this.application = statusDto.application;
        this.version = statusDto.version;
        this.time = statusDto.time;
        this.lastBlock = statusDto.lastBlock;
        this.cumulativeDifficulty = statusDto.cumulativeDifficulty;
        this.numberOfBlocks = statusDto.numberOfBlocks;
        this.shardInitialBlock = statusDto.shardInitialBlock;
        this.lastShardHeight = statusDto.lastShardHeight;
        this.lastBlockchainFeeder = statusDto.lastBlockchainFeeder;
        this.lastBlockchainFeederHeight = statusDto.lastBlockchainFeederHeight;
        this.isScanning = statusDto.isScanning;
        this.isDownloading = statusDto.isDownloading;
        this.maxRollback = statusDto.maxRollback;
        this.currentMinRollbackHeight = statusDto.currentMinRollbackHeight;
        this.maxPrunableLifetime = statusDto.maxPrunableLifetime;
        this.includeExpiredPrunable = statusDto.includeExpiredPrunable;
        this.correctInvalidFees = statusDto.correctInvalidFees;
        this.ledgerTrimKeep = statusDto.ledgerTrimKeep;
        this.chainId = statusDto.chainId;
        this.chainName = statusDto.chainName;
        this.chainDescription = statusDto.chainDescription;
        this.blockTime = statusDto.blockTime;
        this.adaptiveForging = statusDto.adaptiveForging;
        this.adaptiveBlockTime = statusDto.adaptiveBlockTime;
        this.consensus = statusDto.consensus;
        this.maxBlockPayloadLength = statusDto.maxBlockPayloadLength;
        this.initialBaseTarget = statusDto.initialBaseTarget;
        this.coinSymbol = statusDto.coinSymbol;
        this.accountPrefix = statusDto.accountPrefix;
        this.projectName = statusDto.projectName;
        this.services = new ArrayList<>(statusDto.services);
        this.apiProxy = statusDto.apiProxy;
        this.apiProxyPeer = statusDto.apiProxyPeer;
        this.isLightClient = statusDto.isLightClient;
        this.maxAPIRecords = statusDto.maxAPIRecords;
        this.blockchainState = statusDto.blockchainState;
    }

}
