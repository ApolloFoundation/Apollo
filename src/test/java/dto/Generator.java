/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import java.util.Objects;

public class Generator {
    Long effectiveBalanceAPL;
    String accountRS;
    Long deadline;
    String account;
    Long hitTime;

    public Generator(Long effectiveBalanceAPL, String accountRS, Long deadline, String account, Long hitTime) {
        this.effectiveBalanceAPL = effectiveBalanceAPL;
        this.accountRS = accountRS;
        this.deadline = deadline;
        this.account = account;
        this.hitTime = hitTime;
    }

    public Generator() {
    }

    public Long getEffectiveBalanceAPL() {
        return effectiveBalanceAPL;
    }

    public void setEffectiveBalanceAPL(Long effectiveBalanceAPL) {
        this.effectiveBalanceAPL = effectiveBalanceAPL;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    @Override
    public String toString() {
        return "Generator{" +
                "effectiveBalanceAPL=" + effectiveBalanceAPL +
                ", accountRS='" + accountRS + '\'' +
                ", deadline=" + deadline +
                ", account='" + account + '\'' +
                ", hitTime=" + hitTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Generator)) return false;
        Generator generator = (Generator) o;
        return Objects.equals(effectiveBalanceAPL, generator.effectiveBalanceAPL) &&
                Objects.equals(accountRS, generator.accountRS) &&
                Objects.equals(deadline, generator.deadline) &&
                Objects.equals(account, generator.account) &&
                Objects.equals(hitTime, generator.hitTime);
    }

    @Override
    public int hashCode() {

        return Objects.hash(effectiveBalanceAPL, accountRS, deadline, account, hitTime);
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Long getHitTime() {
        return hitTime;
    }

    public void setHitTime(Long hitTime) {
        this.hitTime = hitTime;
    }
}
