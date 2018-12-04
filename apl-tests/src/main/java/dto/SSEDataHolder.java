package dto;


import java.util.List;

public class SSEDataHolder{
    private List<JSONTransaction> transactions;
    private int aliasCount;
    private List<DGSPurchase> purchases;
    private int purchaseCount;
    private int messageCount;
    private int currencyCount;
    private List<Asset> assets;
    private Account account;
    private List<Currency> currencies;
    private Block block;

    public List<DGSPurchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<DGSPurchase> purchases) {
        this.purchases = purchases;
    }

    public List<Currency> getCurrencies() {
        return currencies;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public void setCurrencies(List<Currency> currencies) {
        this.currencies = currencies;
    }

    public SSEDataHolder() {
        super();
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public List<JSONTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<JSONTransaction> transactions) {
        this.transactions = transactions;
    }

    public int getAliasCount() {
        return aliasCount;
    }

    public void setAliasCount(int aliasCount) {
        this.aliasCount = aliasCount;
    }


    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(int purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getCurrencyCount() {
        return currencyCount;
    }

    public void setCurrencyCount(int currencyCount) {
        this.currencyCount = currencyCount;
    }

}