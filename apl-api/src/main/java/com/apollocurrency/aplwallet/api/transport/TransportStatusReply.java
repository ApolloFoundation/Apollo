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
public class TransportStatusReply {
    @JsonProperty("type")
    public String type;
    @JsonProperty("status")
    public String status;    
    @JsonProperty("remoteip")
    public String remoteip;
    @JsonProperty("remoteport")
    public int remoteport;
    @JsonProperty("tunaddr")
    public String tunaddr;    
    @JsonProperty("tunnetmask")
    public String tunnetmask;        
    @JsonProperty("id")
    public int id;    
}
