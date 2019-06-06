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
public class TransportGenericPacket {
    @JsonProperty("type")
    public String type;   
}
