/*
 * Copyright © 2018 Apollo Foundation
 */
package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;

import java.util.Objects;

public class ForgingDetails {
    private Long deadline;
    private Long hitTime;
    private Long remaining;
    private Boolean foundAndStopped;
    private BasicAccount account;
    public ForgingDetails() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForgingDetails)) return false;
        ForgingDetails that = (ForgingDetails) o;
        return Objects.equals(deadline, that.deadline) &&
                Objects.equals(hitTime, that.hitTime) &&
                Objects.equals(remaining, that.remaining) &&
                Objects.equals(foundAndStopped, that.foundAndStopped) &&
                Objects.equals(account, that.account);
    }

    @Override
    public String toString() {
        return "ForgingDetails{" +
                "deadline=" + deadline +
                ", hitTime=" + hitTime +
                ", remaining=" + remaining +
                ", foundAndStopped=" + foundAndStopped +
                ", account=" + account +
                '}';
    }

    @Override
    public int hashCode() {

        return Objects.hash(deadline, hitTime, remaining, foundAndStopped, account);
    }

    public Long getDeadline() {

        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public Long getHitTime() {
        return hitTime;
    }

    public void setHitTime(Long hitTime) {
        this.hitTime = hitTime;
    }

    public Long getRemaining() {
        return remaining;
    }

    public void setRemaining(Long remaining) {
        this.remaining = remaining;
    }

    public Boolean getFoundAndStopped() {
        return foundAndStopped;
    }

    public void setFoundAndStopped(Boolean foundAndStopped) {
        this.foundAndStopped = foundAndStopped;
    }

    public BasicAccount getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = new BasicAccount(account);
    }

    public ForgingDetails(Long deadline, Long hitTime, Long remaining, Boolean foundAndStopped, String account) {
        this.deadline = deadline;
        this.hitTime = hitTime;
        this.remaining = remaining;
        this.foundAndStopped = foundAndStopped;
        this.account = new BasicAccount(account);
    }
}