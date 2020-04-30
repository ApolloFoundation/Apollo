/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author al
 */
public class SvCommandResponse extends SvBusResponse{
    public List<String> out = new ArrayList<>();
    public List<String> err = new ArrayList<>();
}
