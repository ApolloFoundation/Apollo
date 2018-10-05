/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;

import java.util.List;
import java.util.Objects;

public class Block {
    private String previousBlockHash;
    private Long payloadLength;
    private Long totalAmountATM;
    private String generationSignature;
    private BasicAccount generator;
    private String generatorPublicKey;
    private Long baseTarget;
    private String payloadHash;
    private Long numberOfTransactions;
    private String blockSignature;
    private List<JSONTransaction> transactions;
    private Long version;
    private Long totalFeeATM;
    private String previousBlock;
    private String cumulativeDifficulty;
    private String block; //block id
    private int height;
    private Long timestamp; //time in seconds since genesis block

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block block1 = (Block) o;
        return Objects.equals(previousBlockHash, block1.previousBlockHash) &&
                Objects.equals(payloadLength, block1.payloadLength) &&
                Objects.equals(totalAmountATM, block1.totalAmountATM) &&
                Objects.equals(generationSignature, block1.generationSignature) &&
                Objects.equals(generator, block1.generator) &&
                Objects.equals(generatorPublicKey, block1.generatorPublicKey) &&
                Objects.equals(baseTarget, block1.baseTarget) &&
                Objects.equals(payloadHash, block1.payloadHash) &&
                Objects.equals(numberOfTransactions, block1.numberOfTransactions) &&
                Objects.equals(blockSignature, block1.blockSignature) &&
                Objects.equals(transactions, block1.transactions) &&
                Objects.equals(version, block1.version) &&
                Objects.equals(totalFeeATM, block1.totalFeeATM) &&
                Objects.equals(previousBlock, block1.previousBlock) &&
                Objects.equals(cumulativeDifficulty, block1.cumulativeDifficulty) &&
                Objects.equals(block, block1.block) &&
                Objects.equals(height, block1.height) &&
                Objects.equals(timestamp, block1.timestamp);
    }

    @Override
    public int hashCode() {

        return Objects.hash(previousBlockHash, payloadLength, totalAmountATM, generationSignature, generator, generatorPublicKey, baseTarget,
                payloadHash, numberOfTransactions, blockSignature, transactions, version, totalFeeATM, previousBlock, cumulativeDifficulty, block, height, timestamp);
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public void setPreviousBlockHash(String previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    public Long getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(Long payloadLength) {
        this.payloadLength = payloadLength;
    }

    public Long getTotalAmountATM() {
        return totalAmountATM;
    }

    public void setTotalAmountATM(Long totalAmountATM) {
        this.totalAmountATM = totalAmountATM;
    }

    public String getGenerationSignature() {
        return generationSignature;
    }

    public void setGenerationSignature(String generationSignature) {
        this.generationSignature = generationSignature;
    }

    public BasicAccount getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = new BasicAccount(generator);
    }

    public String getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    public void setGeneratorPublicKey(String generatorPublicKey) {
        this.generatorPublicKey = generatorPublicKey;
    }

    public Long getBaseTarget() {
        return baseTarget;
    }

    public void setBaseTarget(Long baseTarget) {
        this.baseTarget = baseTarget;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public Long getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public List<JSONTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<JSONTransaction> transactions) {
        this.transactions = transactions;
    }

    public void setNumberOfTransactions(Long numberOfTransactions) {
        this.numberOfTransactions = numberOfTransactions;
    }

    public String getBlockSignature() {
        return blockSignature;
    }

    public void setBlockSignature(String blockSignature) {
        this.blockSignature = blockSignature;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getTotalFeeATM() {
        return totalFeeATM;
    }


    public void setTotalFeeATM(Long totalFeeATM) {
        this.totalFeeATM = totalFeeATM;
    }

    public String getPreviousBlock() {
        return previousBlock;
    }

    public void setPreviousBlock(String previousBlock) {
        this.previousBlock = previousBlock;
    }

    public String getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    public void setCumulativeDifficulty(String cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Block{" +
                "height=" + height +
                ", previousBlockHash='" + previousBlockHash + '\'' +
                ", totalAmountATM=" + totalAmountATM +
                ", generatorPublicKey='" + generatorPublicKey + '\'' +
                ", blockSignature='" + blockSignature + '\'' +
                ", numberOfTransactions=" + numberOfTransactions +
                ", transactions=" + transactions +
                ", totalFeeATM=" + totalFeeATM +
                '}';
    }
}
