
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Info about blockchain parameters and parameters changes
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockchainInfoDTO {
    //TODO
    public String genesis_pub_key;
    public List<BlockchainInfoRecDTO> bil = new ArrayList<>();
    public Boolean correctInvalidFees;
    public Long ledgerTrimKeep;
    public Long maxAPIRecords;
    public BlockchainState blockchainState;
    public Boolean includeExpiredPrunable;
    public Long maxRollback;
    public Long maxBlockPayloadLength;
    public String coinSymbol;
    public Boolean isScanning;
    public Boolean isDownloading;
    public Boolean adaptiveForging;
    public String cumulativeDifficulty;
    public String chainName;
    public Boolean apiProxy;
    public Long currentMinRollbackHeight;
    public Long numberOfBlocks;
    public String accountPrefix;
    public Boolean isLightClient;
    public List<PeerServices> services;
    public Long requestProcessingTime;
    public String version;
    public String consensus;
    public Long adaptiveBlockTime;
    public String lastBlock;
    public String application;
    public String chainId;
    public Long lastBlockchainFeederHeight;
    public Long maxPrunableLifetime;
    public Long blockTime;
    public Long time;
    public String initialBaseTarget;
    public String chainDescription;
    public String projectName;
    public String lastBlockchainFeeder;

}
