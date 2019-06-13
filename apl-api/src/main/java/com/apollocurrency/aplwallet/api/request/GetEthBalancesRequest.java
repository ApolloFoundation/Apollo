package com.apollocurrency.aplwallet.api.request;

import java.util.List;

/**
 * Addresses for getting balance.
 */
public class GetEthBalancesRequest {

    public List<String> ethAddresses;

    public GetEthBalancesRequest(List<String> ethAddresses) {
        this.ethAddresses = ethAddresses;
    }
}
