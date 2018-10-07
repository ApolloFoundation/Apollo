/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.AccountLedger;
import com.apollocurrency.aplwallet.apl.BasicAccount;

import java.util.Objects;

/**
 * "ledgerId":"176","isTransactionEvent":true,"balance":"10000000000000","holdingType":"UNCONFIRMED_APL_BALANCE","change":"10000000000000","block":"2383565938233337990","eventType":"ORDINARY_PAYMENT","event":"4125735318134947539","account":"1855669503552333464","height":10422,"timestamp":9486050}
 */
public class LedgerEntry {
    private Long ledgerId;
    private boolean isTransactionEvent;
    private Long balance;
    private AccountLedger.LedgerHolding holdingType;
    private Long change;
    private String block;
    private AccountLedger.LedgerEvent eventType;
    private String event;
    private BasicAccount account;
    private Long height;
    private Long timestamp;
    private JSONTransaction transaction;

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public boolean isTransactionEvent() {
        return isTransactionEvent;
    }

    public void setTransactionEvent(boolean transactionEvent) {
        isTransactionEvent = transactionEvent;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public AccountLedger.LedgerHolding getHoldingType() {
        return holdingType;
    }

    public void setHoldingType(AccountLedger.LedgerHolding holdingType) {
        this.holdingType = holdingType;
    }

    public Long getChange() {
        return change;
    }

    public void setChange(Long change) {
        this.change = change;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public AccountLedger.LedgerEvent getEventType() {
        return eventType;
    }

    public void setEventType(AccountLedger.LedgerEvent eventType) {
        this.eventType = eventType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public BasicAccount getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = new BasicAccount(account);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerEntry)) return false;
        LedgerEntry that = (LedgerEntry) o;
        return isTransactionEvent() == that.isTransactionEvent() &&
                Objects.equals(getLedgerId(), that.getLedgerId()) &&
                Objects.equals(getBalance(), that.getBalance()) &&
                getHoldingType() == that.getHoldingType() &&
                Objects.equals(getChange(), that.getChange()) &&
                Objects.equals(getBlock(), that.getBlock()) &&
                getEventType() == that.getEventType() &&
                Objects.equals(getEvent(), that.getEvent()) &&
                Objects.equals(getAccount(), that.getAccount()) &&
                Objects.equals(getHeight(), that.getHeight()) &&
                Objects.equals(getTimestamp(), that.getTimestamp()) &&
                Objects.equals(getTransaction(), that.getTransaction());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getLedgerId(), isTransactionEvent(), getBalance(), getHoldingType(), getChange(), getBlock(), getEventType(), getEvent(), getAccount(), getHeight(), getTimestamp(), getTransaction());
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public JSONTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(JSONTransaction transaction) {
        this.transaction = transaction;
    }

    public LedgerEntry() {
    }

    @Override
    public String toString() {
        return "LedgerEntry{" +
                "balance=" + balance +
                ", holdingType=" + holdingType +
                ", change=" + change +
                ", eventType=" + eventType +
                ", transaction=" + transaction +
                '}';
    }

    public boolean isPrivate() {
        return AccountLedger.LedgerEvent.PRIVATE_PAYMENT == eventType;
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public boolean isNull() {
        return ledgerId == null && account == null && block == null && timestamp == null;
    }
}
