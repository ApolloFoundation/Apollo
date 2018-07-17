package dto;

import java.util.Objects;

public class ForgingDetails {
    Long deadline;
    Long hitTime;
    Long remaining;
    Boolean foundAndStopped;
    String account;
    String accountRS;

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
                Objects.equals(account, that.account) &&
                Objects.equals(accountRS, that.accountRS);
    }

    @Override
    public String toString() {
        return "ForgingDetails{" +
                "deadline=" + deadline +
                ", hitTime=" + hitTime +
                ", remaining=" + remaining +
                ", foundAndStopped=" + foundAndStopped +
                ", account=" + account +
                ", accountRS='" + accountRS + '\'' +
                '}';
    }

    @Override
    public int hashCode() {

        return Objects.hash(deadline, hitTime, remaining, foundAndStopped, account, accountRS);
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public ForgingDetails(Long deadline, Long hitTime, Long remaining, Boolean foundAndStopped, String account, String accountRS) {
        this.deadline = deadline;
        this.hitTime = hitTime;
        this.remaining = remaining;
        this.foundAndStopped = foundAndStopped;
        this.account = account;
        this.accountRS = accountRS;
    }
}