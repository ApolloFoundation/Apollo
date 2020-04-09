/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import java.util.Objects;

public class GeneratorInfo {
    private Long effectiveBalanceAPL;
    private Long deadline;
    private Long hitTime;
    private BasicAccount account;

    public GeneratorInfo() {
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
        if (!(o instanceof GeneratorInfo)) return false;
        GeneratorInfo generator = (GeneratorInfo) o;
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

    public void setAccount(long account) {
        this.account = new BasicAccount(account);
    }

    public Long getHitTime() {
        return hitTime;
    }

    public void setHitTime(Long hitTime) {
        this.hitTime = hitTime;
    }
}
