package dto;

import java.util.List;
import java.util.Objects;

public class NextGenerators {
    private Long activeCount;
    private String lastBlock;
    private List<Generator> generators;
    private Long height;
    private Long timestamp;

    public NextGenerators() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NextGenerators)) return false;
        NextGenerators that = (NextGenerators) o;
        return Objects.equals(activeCount, that.activeCount) &&
                Objects.equals(lastBlock, that.lastBlock) &&
                Objects.equals(generators, that.generators) &&
                Objects.equals(height, that.height) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {

        return Objects.hash(activeCount, lastBlock, generators, height, timestamp);
    }

    @Override
    public String toString() {
        return "NextGenerators{" +
                "activeCount=" + activeCount +
                ", lastBlock='" + lastBlock + '\'' +
                ", generators=" + generators +
                ", height=" + height +
                ", timestamp=" + timestamp +
                '}';
    }

    public Long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Long activeCount) {
        this.activeCount = activeCount;
    }

    public String getLastBlock() {
        return lastBlock;
    }

    public void setLastBlock(String lastBlock) {
        this.lastBlock = lastBlock;
    }

    public List<Generator> getGenerators() {
        return generators;
    }

    public void setGenerators(List<Generator> generators) {
        this.generators = generators;
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

    public NextGenerators(Long activeCount, String lastBlock, List<Generator> generators, Long height, Long timestamp) {

        this.activeCount = activeCount;
        this.lastBlock = lastBlock;
        this.generators = generators;
        this.height = height;
        this.timestamp = timestamp;
    }
}
