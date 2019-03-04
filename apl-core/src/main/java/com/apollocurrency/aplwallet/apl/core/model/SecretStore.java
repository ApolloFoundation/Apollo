package com.apollocurrency.aplwallet.apl.core.model;

import java.util.ArrayList;
import java.util.List;

public class SecretStore {

    private List<AplWalletKey> aplKeys = new ArrayList<>();
    private List<EthWalletKey> ethKeys = new ArrayList<>();

    public List<AplWalletKey> getAplKeys() {
        return aplKeys;
    }

    public void addAplKeys(AplWalletKey aplKey) {
        this.aplKeys.add(aplKey);
    }

    public List<EthWalletKey> getEthKeys() {
        return ethKeys;
    }

    public void addEthKeys(EthWalletKey ethKey) {
        this.ethKeys.add(ethKey);
    }
}
