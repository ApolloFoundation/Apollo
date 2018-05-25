package test;

import java.util.List;

public class Block {
    private String previousBlockHash;
    private Long payloadLength;
    private Long totalAmountNQT;
    private String generationSignature;
    private String generator;
    private String generatorPublicKey;
    private Long baseTarget;
    private String payloadHash;
    private String generatorRS;
    private Long numberOfTransactions;
    private String blockSignature;
    private List<Transaction> transactions;
    private Long version;
    private Long totalFeeNQT;
    private String previousBlock;
    private String cumulativeDifficulty;
    private String block; //block id
    private Long height;
    private Long timestamp; //time in seconds since genesis block

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

    public Long getTotalAmountNQT() {
        return totalAmountNQT;
    }

    public void setTotalAmountNQT(Long totalAmountNQT) {
        this.totalAmountNQT = totalAmountNQT;
    }

    public String getGenerationSignature() {
        return generationSignature;
    }

    public void setGenerationSignature(String generationSignature) {
        this.generationSignature = generationSignature;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
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

    public String getGeneratorRS() {
        return generatorRS;
    }

    public void setGeneratorRS(String generatorRS) {
        this.generatorRS = generatorRS;
    }

    public Long getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
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

    public Long getTotalFeeNQT() {
        return totalFeeNQT;
    }


    public void setTotalFeeNQT(Long totalFeeNQT) {
        this.totalFeeNQT = totalFeeNQT;
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

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
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
            ", totalAmountNQT=" + totalAmountNQT +
            ", generatorPublicKey='" + generatorPublicKey + '\'' +
            ", blockSignature='" + blockSignature + '\'' +
            ", numberOfTransactions=" + numberOfTransactions +
            ", transactions=" + transactions +
            ", totalFeeNQT=" + totalFeeNQT +
            '}';
    }
}
