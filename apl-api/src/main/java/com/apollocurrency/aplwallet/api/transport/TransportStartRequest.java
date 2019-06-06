/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.api.transport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author nemez
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
