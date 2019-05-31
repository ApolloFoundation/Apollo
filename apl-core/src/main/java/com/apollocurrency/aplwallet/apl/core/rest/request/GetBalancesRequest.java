package com.apollocurrency.aplwallet.apl.core.rest.request;

import java.util.List;

/**
 * Addresses for getting balance.
 */
public class GetBalancesRequest {

    public List<String> ethAddresses;

    public GetBalancesRequest(List<String> ethAddresses) {
        this.ethAddresses = ethAddresses;
    }
}
