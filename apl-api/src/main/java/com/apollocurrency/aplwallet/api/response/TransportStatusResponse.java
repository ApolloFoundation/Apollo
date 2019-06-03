/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.api.response;


import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)


@Getter @Setter
public class TransportStatusResponse extends ResponseBase {
    
    private Boolean done;
    
}