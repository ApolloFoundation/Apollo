package com.apollocurrency.aplwallet.api.request;

public class AddPeerRequestDTO {
    public String peer; // peer address

    @Override
    public String toString() {
        return "AddPeerRequestDTO{" +
                "peer='" + peer + '\'' +
                '}';
    }
}
