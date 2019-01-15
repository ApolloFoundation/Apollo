
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Info about blockchain parameters and parameters changes
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockchainInfoDTO {
    //TODO
    public String genesis_pub_key;
    public List<BlockchainInfoRecDTO> bil = new ArrayList<>();
}
