/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockDTO extends BaseDTO {
    private String block; //block id
    private Integer height;
    private String generator;
    private String generatorRS;
    private String generatorPublicKey;
    private Integer timestamp; //time in seconds since genesis block
    private Integer timeout;
    private Long numberOfTransactions;
    private String totalFeeATM;
    private Integer payloadLength;
    private Integer version;
    private String baseTarget;
    private String cumulativeDifficulty;
    private String previousBlock;
    private String nextBlock;
    private String payloadHash;
    private String generationSignature;
    private String previousBlockHash;
    private String blockSignature;
    private String totalAmountATM;
    private Integer numberOfFailedTxs;
    private List<TxErrorHashDTO> txErrorHashes = new ArrayList<>();

    private List<TransactionDTO> transactions;
    private List executedPhasedTransactions;

}
