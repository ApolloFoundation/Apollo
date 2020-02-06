package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockchainInfoDTO extends BaseDTO {
    private String genesis_pub_key;
    //private List<BlockchainInfoRecDTO> bil = new ArrayList<>();
    private Boolean correctInvalidFees;
    private Long ledgerTrimKeep;
    private Long maxAPIRecords;
    private BlockchainState blockchainState;
    private Boolean includeExpiredPrunable;
    private Long maxRollback;
    private Long maxBlockPayloadLength;
    private String coinSymbol;
    private Boolean isScanning;
    private Boolean isDownloading;
    private Boolean adaptiveForging;
    private String cumulativeDifficulty;
    private String chainName;
    private Boolean apiProxy;
    private Long currentMinRollbackHeight;
    private Long numberOfBlocks;
    private String accountPrefix;
    private Boolean isLightClient;
    //private List<PeerServices> services;
    private Long requestProcessingTime;
    private String version;
    private String consensus;
    private Long adaptiveBlockTime;
    private String lastBlock;
    private String application;
    private String chainId;
    private Long lastBlockchainFeederHeight;
    private Long maxPrunableLifetime;
    private Long blockTime;
    private Long time;
    private String initialBaseTarget;
    private String chainDescription;
    private String projectName;
    private String lastBlockchainFeeder;
}
