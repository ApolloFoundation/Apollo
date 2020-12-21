/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Setter
@ToString(callSuper = true)
public class SmcContract  extends VersionedDerivedEntity {
    private String address; //contract address/id
    private String data;
    private String contractName;
    private String languageName;

    private BigInteger fuelValue;//initial fuel value

    private BigInteger fuelPrice;

    private String transactionId;

    public SmcContract(Long dbId, Integer height,
                       String address, String data, String contractName, String languageName,
                       BigInteger fuelValue, BigInteger fuelPrice, String transactionId) {
        super(dbId, height);
        this.address = address;
        this.data = data;
        this.contractName = contractName;
        this.languageName = languageName;
        this.fuelValue = fuelValue;
        this.fuelPrice = fuelPrice;
        this.transactionId = transactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContract that = (SmcContract) o;
        return Objects.equals(address, that.address)
            && Objects.equals(data, that.data)
            && Objects.equals(contractName, that.contractName)
            && Objects.equals(languageName, that.languageName)
            && Objects.equals(fuelValue, that.fuelValue)
            && Objects.equals(fuelPrice, that.fuelPrice)
            && Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, data, contractName, languageName, fuelValue, fuelPrice, transactionId);
    }
}
