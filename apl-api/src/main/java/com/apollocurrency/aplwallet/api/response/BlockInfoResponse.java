
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response with one found block
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Blocks info representation")
//@ApiModel
public class BlockInfoResponse extends ResponseBase {
    //@ApiModelProperty(value = "Hash value of previous block in HEX representation", allowEmptyValue = true)
    public String previousBlockHash;
    //@ApiModelProperty("Payload length)")
    public Long payloadLength;
    public String totalFeeATM;
    //@ApiModelProperty(value = "Hash value of generator in HEX representation")
    public String generationSignature;
    //@ApiModelProperty(value = "Generator id value")
    public String generator;
    //@ApiModelProperty(value = "Generator Public Key value in HEX representation")
    public String generatorPublicKey;
    public String baseTarget;
    public String payloadHash;
    //@ApiModelProperty(value = "Generator value in Reed-Solomon representation")
    public String generatorRS;
    //@ApiModelProperty(value = "Number of transactions inside block")
    public Long numberOfTransactions;
    public String blockSignature;
    public Long version;
    public String totalFeeNQT;
    public String previousBlock;
    public String cumulativeDifficulty;
    //@ApiModelProperty(value = "Unique block id value")
    public String block;
    //@ApiModelProperty("Block length)")
    public Long height;
    //@ApiModelProperty(value = "Block's timestamp")
    public Long timestamp;
    public String nextBlock;
    public long totalAmountATM;
    //@ApiModelProperty(value = "Transaction List", allowEmptyValue = true)
    public List<Object> transactions;
    //@ApiModelProperty(value = "Executed Phased Transaction List", allowEmptyValue = true)
    public List<Object> executedPhasedTransactions;
    public long timeout;

    public BlockInfoResponse() {
    }

    @Override
    public String toString() {
        return "BlockInfoResponse{" +
                "previousBlockHash='" + previousBlockHash + '\'' +
                ", payloadLength=" + payloadLength +
                ", generator='" + generator + '\'' +
                ", version=" + version +
                ", blockId='" + block + '\'' +
                ", height=" + height +
                ", timestamp=" + timestamp +
                ", transactions=[" + (transactions != null ? transactions.size() : 0) +
                "]}";
    }
}
