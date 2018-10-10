/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;

import java.util.Objects;

public class Generator {
    private Long effectiveBalanceAPL;
    private Long deadline;
    private Long hitTime;
    private BasicAccount account;

    public Generator(Long effectiveBalanceAPL, Long deadline, String account, Long hitTime) {
        this.effectiveBalanceAPL = effectiveBalanceAPL;
        this.deadline = deadline;
        this.account = new BasicAccount(account);
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

    @Override
    public String toString() {
        return "Generator{" +
                "effectiveBalanceAPL=" + effectiveBalanceAPL +
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
                Objects.equals(deadline, generator.deadline) &&
                Objects.equals(account, generator.account) &&
                Objects.equals(hitTime, generator.hitTime);
    }

    @Override
    public int hashCode() {

        return Objects.hash(effectiveBalanceAPL, deadline, account, hitTime);
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public BasicAccount getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = new BasicAccount(account);
    }

    public Long getHitTime() {
        return hitTime;
    }

    public void setHitTime(Long hitTime) {
        this.hitTime = hitTime;
    }
}
