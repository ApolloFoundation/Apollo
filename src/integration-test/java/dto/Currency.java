package dto;


import java.util.Objects;

public class Currency {
    private long accountId;
    private String name;
    private String code;
    private String description;
    private int type;
    private long maxSupply;
    private long reserveSupply;
    private int creationHeight;
    private int issuanceHeight;
    private long minReservePerUnitATM;
    private int minDifficulty;
    private int maxDifficulty;
    private byte ruleset;
    private byte algorithm;
    private byte decimals;
    private long initialSupply;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return accountId == currency.accountId &&
                type == currency.type &&
                maxSupply == currency.maxSupply &&
                reserveSupply == currency.reserveSupply &&
                creationHeight == currency.creationHeight &&
                issuanceHeight == currency.issuanceHeight &&
                minReservePerUnitATM == currency.minReservePerUnitATM &&
                minDifficulty == currency.minDifficulty &&
                maxDifficulty == currency.maxDifficulty &&
                ruleset == currency.ruleset &&
                algorithm == currency.algorithm &&
                decimals == currency.decimals &&
                initialSupply == currency.initialSupply &&
                Objects.equals(name, currency.name) &&
                Objects.equals(code, currency.code) &&
                Objects.equals(description, currency.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, name, code, description, type, maxSupply, reserveSupply, creationHeight, issuanceHeight, minReservePerUnitATM, minDifficulty, maxDifficulty, ruleset, algorithm, decimals, initialSupply);
    }

    public Currency() {
        super();
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getMaxSupply() {
        return maxSupply;
    }

    public void setMaxSupply(long maxSupply) {
        this.maxSupply = maxSupply;
    }

    public long getReserveSupply() {
        return reserveSupply;
    }

    public void setReserveSupply(long reserveSupply) {
        this.reserveSupply = reserveSupply;
    }

    public int getCreationHeight() {
        return creationHeight;
    }

    public void setCreationHeight(int creationHeight) {
        this.creationHeight = creationHeight;
    }

    public int getIssuanceHeight() {
        return issuanceHeight;
    }

    public void setIssuanceHeight(int issuanceHeight) {
        this.issuanceHeight = issuanceHeight;
    }

    public long getMinReservePerUnitATM() {
        return minReservePerUnitATM;
    }

    public void setMinReservePerUnitATM(long minReservePerUnitATM) {
        this.minReservePerUnitATM = minReservePerUnitATM;
    }

    public int getMinDifficulty() {
        return minDifficulty;
    }

    public void setMinDifficulty(int minDifficulty) {
        this.minDifficulty = minDifficulty;
    }

    public int getMaxDifficulty() {
        return maxDifficulty;
    }

    public void setMaxDifficulty(int maxDifficulty) {
        this.maxDifficulty = maxDifficulty;
    }

    public byte getRuleset() {
        return ruleset;
    }

    public void setRuleset(byte ruleset) {
        this.ruleset = ruleset;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(byte algorithm) {
        this.algorithm = algorithm;
    }

    public byte getDecimals() {
        return decimals;
    }

    public void setDecimals(byte decimals) {
        this.decimals = decimals;
    }

    public long getInitialSupply() {
        return initialSupply;
    }

    public void setInitialSupply(long initialSupply) {
        this.initialSupply = initialSupply;
    }
}
