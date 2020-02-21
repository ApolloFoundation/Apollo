/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.info;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
}
