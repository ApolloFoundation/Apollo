
/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * This class represents "envelope" message format for all
 * messages going trough messaging systems of Apollo Supervisor
 * Null fields ignored on serializing.
 * @author alukinb@gmail.conm
 */
@NoArgsConstructor
@AllArgsConstructor
public class SvChannelMessage {
    public SvChannelHeader header = new SvChannelHeader();    
    public Object body;
}

