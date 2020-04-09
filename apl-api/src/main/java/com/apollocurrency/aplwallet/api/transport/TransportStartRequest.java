/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.transport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Serhiy Lymar
 */
public class TransportStartRequest {
    @JsonProperty("type")
    public String type;
    @JsonProperty("serversjson")
    public String serversjson;
    @JsonProperty("uniqueenable")
    public boolean uniqueenable;
    @JsonProperty("shuffle")
    public boolean shuffle;
    @JsonProperty("uniqueport")
    public int uniqueport;
    @JsonProperty("logpath")
    public String logpath;
    @JsonProperty("id")
    public int id;
}
