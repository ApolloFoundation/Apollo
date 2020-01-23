package com.apollocurrency.aplwallet.apl.eth.web3j;

import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Represent ordinary StaticGasProvider with implemented equals/hashcode for
 * unit testing only
 */
public class ComparableStaticGasProvider extends StaticGasProvider {
    public ComparableStaticGasProvider(BigInteger gasPrice, BigInteger gasLimit) {
        super(gasPrice, gasLimit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaticGasProvider)) return false;
        StaticGasProvider gasProvider = (StaticGasProvider) o;
        return Objects.equals(getGasLimit(), gasProvider.getGasLimit()) &&
                Objects.equals(getGasPrice(), gasProvider.getGasPrice());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGasLimit(), getGasPrice());
    }

    @Override
    public String toString() {
        return "GasPrice:" + getGasPrice() + ",GasLimit:" + getGasLimit();
    }
}
