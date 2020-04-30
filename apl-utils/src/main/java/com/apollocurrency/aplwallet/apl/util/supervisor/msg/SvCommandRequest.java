package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import java.util.HashMap;

/**
 * This class represents command request to supervisor
 * @author alukin@gmail.com
 */
// @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class SvCommandRequest extends SvBusRequest{
    public String cmd;
    public HashMap<String,String> params=new HashMap<>();
}
