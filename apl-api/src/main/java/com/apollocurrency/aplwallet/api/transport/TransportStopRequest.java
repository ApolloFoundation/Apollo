/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.transport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Serhiy Lymar
 */
public class TransportStopRequest {
    @JsonProperty("type")
    public String type;
    @JsonProperty("id")
    public int id;
}
