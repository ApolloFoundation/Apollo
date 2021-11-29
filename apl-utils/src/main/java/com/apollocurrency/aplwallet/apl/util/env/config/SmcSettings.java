/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author andrew.zinchenko@gmail.com
 */
@EqualsAndHashCode
public class SmcSettings {
    @Getter
    private final long smcMasterAccountId;

    @JsonCreator
    public SmcSettings(@JsonProperty(value = "smcMasterAccountId", required = true) Long masterAccountId) {
        this.smcMasterAccountId = masterAccountId;
        if (masterAccountId == 0) {
            System.out.println("Smart-contract transactions acceptance will be disabled for this instance of settings, cause masterAccountId=0.");
        }
    }

    public SmcSettings() {
        this(0L);
    }

    public SmcSettings copy() {
        return new SmcSettings(smcMasterAccountId);
    }

    @Override
    public String toString() {
        return "SmcSettings{" +
            "smcMasterAccountId=" +
            "long: " + smcMasterAccountId +
            ", ulong: " + Long.toUnsignedString(smcMasterAccountId) +
            ", hex: " + Convert.toHexString(smcMasterAccountId) +
            '}';
    }
}
